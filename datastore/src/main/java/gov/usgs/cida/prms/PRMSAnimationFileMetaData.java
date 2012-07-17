package gov.usgs.cida.prms;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import static gov.usgs.cida.prms.PRMSAnimationFileUtility.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationFileMetaData {


	private final static XStream XSTREAM;

	static {
		XSTREAM = new XStream(new DomDriver());

		// cleans up tags and let me refactor (packages only) without breaking persistence format...
		XStreamUtility.simpleAlias(XSTREAM, PRMSAnimationFileMetaData.class);
		XStreamUtility.simpleAlias(XSTREAM, RecordEntryRange.class);
		XStreamUtility.simpleAlias(XSTREAM, RecordEntryDescriptor.class);
        XStreamUtility.simpleAlias(XSTREAM, DateTime.class);

		// don't want relative references to SequenceType (human readability)
		XSTREAM.addImmutableType(RecordEntryDescriptor.Type.class);
        
        XSTREAM.registerConverter(new DateTimeConverter());
	}

	public static PRMSAnimationFileMetaData getMetaData(String filePath) throws IOException {
		return getMetaData(new File(filePath));
	}

    public static PRMSAnimationFileMetaData getMetaData(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            return getMetaData(url.getPath());
        } else {
            throw new IOException("Only URL with \"file\" protocol are supported");
        }
    }
    
	public static PRMSAnimationFileMetaData getMetaData(File file) throws IOException {

		PRMSAnimationFileMetaData metaData = null;

		String metaDataPath = file.getCanonicalPath() + ".xml";
		File metaDataFile = new File(metaDataPath);

		if(metaDataFile.exists() && metaDataFile.lastModified() > file.lastModified()) {
			InputStream inputStream = null;
			try {
				inputStream = new BufferedInputStream(
						new FileInputStream(metaDataFile),
						1 << 20);
				Object o = XSTREAM.fromXML(inputStream);
				if(o != null && o instanceof PRMSAnimationFileMetaData) {
					metaData = (PRMSAnimationFileMetaData)o;
				}
			} catch (Exception e) {
				System.out.println("Error reading " + metaDataPath + ", will attempt to regenerate...");
			} finally {
				if(inputStream != null) {
					inputStream.close();
				}
			}
		}

		if(metaData == null) {
			metaData = new PRMSAnimationFileMetaData(file);

			OutputStream outputStream = null;
			try {
				outputStream = new BufferedOutputStream(
						new FileOutputStream(metaDataFile),
						1 << 20);
				XSTREAM.toXML(metaData, outputStream);
			} catch (Exception e) {
				System.out.println("Error writing " + metaDataPath);
			} finally {
				if (outputStream != null) {
					outputStream.close();
				}
			}
		}

		return metaData;
	}

	private String animationFilePath;
	private int recordEntryCount; // number of entry per record
	private List<RecordEntryDescriptor> recordEntryDescriptors;
    private List<RecordEntryRange> recordEntryRanges;
	private int endOfLineSizeBytes;
	private int headerSizeBytes;
	private long dataSizeBytes;
	private int recordCount; // number of records per file
	private int recordSizeBytes;
    private int timeStepCount; // number of timesteps per file
	private int timeStepRecordCount; // number of records per timestep
	private int timeStepSizeBytes;
    private ArrayList<DateTime> timeStepList;
    
    private Map<String, Integer> recordEntryNameToIndex = new LinkedHashMap<String, Integer>();

	private PRMSAnimationFileMetaData() {

	}

	private PRMSAnimationFileMetaData(File file) throws IOException {
		Reader r = new Reader(file);
		r.parse();
	}

	public String getAnimationFilePath() {
		return animationFilePath;
	}

	public int getRecordEntryCount() {
		return recordEntryCount;
	}

	public List<RecordEntryDescriptor> getRecordEntryDescriptors() {
		return recordEntryDescriptors;
	}
    
    public List<RecordEntryRange> getRecordEntryRanges() {
        return recordEntryRanges;
    }

	public long getDataSizeBytes() {
		return dataSizeBytes;
	}

	public int getEndOfLineSizeBytes() {
		return endOfLineSizeBytes;
	}

	public int getHeaderSizeBytes() {
		return headerSizeBytes;
	}

	public int getRecordCount() {
		return recordCount;
	}

	public int getRecordSizeBytes() {
		return recordSizeBytes;
	}
    
    public int getTimeStepCount() {
        return timeStepCount;
    }

	public int getTimeStepRecordCount() {
		return timeStepRecordCount;
	}

	public int getTimeStepSizeBytes() {
		return timeStepSizeBytes;
	}
    
    public List<DateTime> getTimeStepList() {
        return Collections.unmodifiableList(timeStepList);
    }
    
    public int getTimeStepIndex(DateTime dateTime) {
        return timeStepList.indexOf(dateTime);
    }
    
    public int getRecordEntryIndex(String name) {
        Integer i = recordEntryNameToIndex.get(name);
        if (i != null) {
            return i.intValue();
        } else {
            throw new IllegalArgumentException(name + " not found");
        }
    }

	private class Reader {

		private Reader(File file) throws IOException {
			animationFilePath = file.getPath();
		}

		private void parse() throws IOException {
			long start = System.currentTimeMillis();
			parseHeader();
			parseData();
			long delta = System.currentTimeMillis() - start;
			float deltaSeconds = (float)delta / (float)1000;
			float sizeMB = (float)(headerSizeBytes + dataSizeBytes) / (float)(1 << 20);
			float rate = sizeMB / deltaSeconds;
			System.out.println("parsed " + animationFilePath + " in " + deltaSeconds + "s (" + rate + " MiB/s)");
		}

		private void parseHeader() throws IOException {

			RandomAccessFile randomAccessFile = new RandomAccessFile(animationFilePath, "r");

			String line = randomAccessFile.readLine();

			endOfLineSizeBytes = (int) randomAccessFile.getFilePointer() - line.length();

			while (line.charAt(0) == COMMENT_DELIMITER) {
				line = randomAccessFile.readLine();
			}

			String[] recordEntryNames = line.split("\\s+");
			recordEntryCount = recordEntryNames.length;

			// column type line, nothing in here we can use... (in fact it's incorrect)
			randomAccessFile.readLine();

			headerSizeBytes = (int) randomAccessFile.getFilePointer();

			// first data line
			line = randomAccessFile.readLine();

			recordEntryDescriptors = generateRecordEntryDescriptors(line, recordEntryNames);
            recordEntryRanges = new ArrayList<RecordEntryRange>(recordEntryNames.length);

			recordSizeBytes = (int) (randomAccessFile.getFilePointer() - headerSizeBytes);
			dataSizeBytes = randomAccessFile.length() - headerSizeBytes;
			recordCount = (int) (dataSizeBytes / (long) recordSizeBytes);

			randomAccessFile.close();
			randomAccessFile = null;

            for (int recordEntryIndex = 0; recordEntryIndex < recordEntryCount; ++recordEntryIndex) {
                recordEntryNameToIndex.put(recordEntryDescriptors.get(recordEntryIndex).getName(), recordEntryIndex);
            }

		}

		private void parseData() throws IOException {

			PRMSAnimationRecordBuffer recordBuffer = null;
			try {
				recordBuffer = new PRMSAnimationRecordBuffer(PRMSAnimationFileMetaData.this);
				
				MetaDataRecordHandler mdrh = new MetaDataRecordHandler();

				mdrh.initialize(recordBuffer.getRecord(0));
				for (int recordIndex = 1; recordIndex < recordCount; ++recordIndex) {
					mdrh.process(recordBuffer.getRecord(recordIndex));
				}
				mdrh.finish(recordCount);
			} finally {
				if (recordBuffer != null) {
					recordBuffer.close();
					recordBuffer = null;
				}
			}
		}

		private class MetaDataRecordHandler {

			// TimeStep tracking
			DateTime currentTimeStep;
            int currentTimeStepRecordCount;

			public MetaDataRecordHandler() {
                timeStepCount = 1;
				timeStepRecordCount = -1;
                timeStepList = new ArrayList<DateTime>();
			}

			public void initialize(PRMSAnimationRecord record) {

				currentTimeStep = record.getTimeStamp();
                currentTimeStepRecordCount = 0;
                timeStepList.add(currentTimeStep);

				initializeRanges(record);

			}

			public void process(PRMSAnimationRecord record) {

				DateTime recordTimeStep = record.getTimeStamp();

                updateRangesFromRecord(record);
                
				if (!recordTimeStep.equals(currentTimeStep)) {
                    timeStepList.add(recordTimeStep);
                    if (currentTimeStep == null) {
                        if (currentTimeStepRecordCount != timeStepRecordCount) {
                            throw new IllegalStateException(
                                    "timestep record mismatch, expected " + timeStepRecordCount +
                                    " but found " + currentTimeStepRecordCount +
                                    " for " + currentTimeStep.toString());
                        }
                    } else {
                        timeStepRecordCount = currentTimeStepRecordCount;
                        timeStepSizeBytes = currentTimeStepRecordCount * recordSizeBytes;
                    }
                    ++timeStepCount;
                    currentTimeStepRecordCount = 0;
				}
                ++currentTimeStepRecordCount;
                currentTimeStep = recordTimeStep;
			}

			public void finish(int recordIndex) {

			}

			private void initializeRanges(PRMSAnimationRecord record) {
                recordEntryRanges.add(new RecordEntryRange(record.getTimeStamp()));
                recordEntryRanges.add(new RecordEntryRange(record.getNHRU()));
                for (int recordEntryIndex = 2; recordEntryIndex < recordEntryCount; ++recordEntryIndex) {
                    recordEntryRanges.add(new RecordEntryRange(record.getValue(recordEntryIndex)));
                }
			}

			private void updateRangesFromRecord(PRMSAnimationRecord record) {
                recordEntryRanges.get(0).update(record.getTimeStamp());
                recordEntryRanges.get(1).update(record.getNHRU());
                for (int recordEntryIndex = 2; recordEntryIndex < recordEntryCount; ++recordEntryIndex) {
                    recordEntryRanges.get(recordEntryIndex).update(record.getValue(recordEntryIndex));
                }
			}
		}
	}

    private final static DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
    
    public static class DateTimeConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return clazz.equals(DateTime.class);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            writer.setValue(formatter.print((DateTime)value));
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            return formatter.parseDateTime(reader.getValue());
        }
    }

}
