package gov.usgs.cida.prms;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PRMSAnimationRecordBuffer implements Iterator<PRMSAnimationRecord>, Iterable<PRMSAnimationRecord> {

	// This class was originally designed for used with MappedByteBuffer for
	// speed.  At this time we should avoid this since:
	//  1) We can run out of virtual address space on 32 bit platforms.  This is
	//     exaserbated by the lack of unmap() type functionality and we have to
	//     rely on JVM GC to release system resources (virutal address space).
	//     This is not an issue with 64 bit JVM
	//  2) We still need to implement a sliding buffer strategy (not enough time)
	//     as mapping a large enough file w/o  enough physical RAM can cause
	//     irrecoverable thrashing (32 *and* 64 bit platforms)
	//  These issues are avoidable but we don't have time to implement *and*
	//  test
	private final static boolean MAPPED_IO = true;

	public static final int INVALID_RECORD_INDEX = Integer.MIN_VALUE;

	final private RecordEntryDescriptor[] recordEntryDescriptors;

	final private int columnCount;

	final private int recordIndexStart;
	final private int recordIndexEnd;
	private int recordIndex;

	private BufferedInputStream inputStream;

	private RandomAccessFile randomAccessFile;
	private MappedByteBuffer mappedBuffer;

	private long bufferOffsetBytes;
	private long bufferLengthBytes;

	final private int recordSizeBytes;
    final private int endOfLineSizeBytes;
	
	public PRMSAnimationRecordBuffer(PRMSAnimationFileMetaData metaData) throws IOException {
		this(metaData, 0, metaData.getRecordCount());
	}

	public PRMSAnimationRecordBuffer(PRMSAnimationFileMetaData metaData, int recordIndexStart, int recordIndexEnd) throws IOException {

		this.columnCount = metaData.getRecordEntryCount();
		this.recordEntryDescriptors = metaData.getRecordEntryDescriptors().toArray(new RecordEntryDescriptor[0]);

		this.recordIndexStart = recordIndexStart;
		this.recordIndexEnd = recordIndexEnd;

		int headerSizeBytes = metaData.getHeaderSizeBytes();
        
		recordSizeBytes = metaData.getRecordSizeBytes();
		endOfLineSizeBytes = metaData.getEndOfLineSizeBytes();

		bufferOffsetBytes = headerSizeBytes + (recordIndexStart * recordSizeBytes);
		bufferLengthBytes = (recordIndexEnd - recordIndexStart) * recordSizeBytes;

		// prime value for validity test in getRecordFromInputStream(...) and
		// iterator();
		recordIndex = recordIndexStart - 1;
		if (MAPPED_IO) {

			randomAccessFile = new RandomAccessFile(metaData.getAnimationFilePath(), "r");
			mappedBuffer = randomAccessFile.getChannel().map(MapMode.READ_ONLY, bufferOffsetBytes, bufferLengthBytes);
		} else {

			inputStream = new BufferedInputStream(
					new FileInputStream(metaData.getAnimationFilePath()),
					1 << 20);
			
			inputStream.skip(bufferOffsetBytes);
			
		}
	}

	// TODO: document (only valid until get record called again!!!)
	public PRMSAnimationRecord getRecord(int recordIndex) throws IOException {
		if (MAPPED_IO) {
			return getRecordFromMappedByteBuffer(recordIndex);
		} else {
			return getRecordFromInputStream(recordIndex);
		}
	}

	private PRMSAnimationRecord getRecordFromMappedByteBuffer(int recordIndex) throws IOException {
		if (recordIndex < recordIndexStart) {
			throw new NoSuchElementException();
		}
		if (recordIndex < recordIndexEnd) {
			try {
                byte[] recordBytes = new byte[recordSizeBytes];
                char[] recordChars = new char[recordSizeBytes - endOfLineSizeBytes];
                
				mappedBuffer.position((recordIndex - recordIndexStart) * recordSizeBytes);
				mappedBuffer.get(recordBytes);
                
				int recorndCharCount = recordChars.length;
				for (int recordCharIndex = 0; recordCharIndex < recorndCharCount; ++recordCharIndex) {
					recordChars[recordCharIndex] = (char) (recordBytes[recordCharIndex] & 255);
				}
                
				this.recordIndex = recordIndex;
                
                return new PRMSAnimationRecordImpl(
                        Arrays.asList(recordEntryDescriptors),
                        recordIndex,
                        recordChars);
                
			} catch (Exception e) {
				this.recordIndex = INVALID_RECORD_INDEX;
				throw new IOException("Incomplete read.");
			}
		} else {
			throw new NoSuchElementException();
		}
	}

	private PRMSAnimationRecord getRecordFromInputStream(int recordIndex) throws IOException {
		if (recordIndex == this.recordIndex + 1) {
			if (recordIndex < recordIndexStart) {
				throw new NoSuchElementException();
			}
			if (recordIndex < recordIndexEnd) {
                
                byte[] recordBytes = new byte[recordSizeBytes];
                char[] recordChars = new char[recordSizeBytes - endOfLineSizeBytes];
                
				int read = inputStream.read(recordBytes);
				if (read == recordBytes.length) {
					int charCount = recordChars.length;
					for (int charIndex = 0; charIndex < charCount; ++charIndex) {
						recordChars[charIndex] = (char) (recordBytes[charIndex] & 255);
					}
					this.recordIndex = recordIndex;
                    
                    return new PRMSAnimationRecordImpl(
                        Arrays.asList(recordEntryDescriptors),
                        recordIndex,
                        recordChars);
                    
				} else {
					this.recordIndex = INVALID_RECORD_INDEX;
					throw new IOException("Incomplete read.");
				}
			} else {
				throw new NoSuchElementException();
			}
		} else {
			// The api is confusing but this class was originally designed for
			// use with NIO's MappedByteBuffer which would have supported random access
			throw new RuntimeException("Seguential record access required with InputStrem");
		}
	}

	public void close() throws IOException {
		if (MAPPED_IO) {
			if (randomAccessFile != null) {
				randomAccessFile.close();
				randomAccessFile = null;
			}
			mappedBuffer = null;
		} else {
			inputStream.close();
			inputStream = null;
		}
	}

    @Override
	public boolean hasNext() {
		return recordIndex + 1 < recordIndexEnd && recordIndex != INVALID_RECORD_INDEX;
	}

    @Override
	public PRMSAnimationRecord next() {
		PRMSAnimationRecord nextRecord = null;
		try {
			nextRecord = getRecord(recordIndex + 1);
		} catch (NoSuchElementException e) {
			throw e;
		} catch (Exception e) {
			throw new NoSuchElementException("Error on record read.  Caught " + e);
		}
		return nextRecord;
	}

    @Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

    @Override
	public Iterator<PRMSAnimationRecord> iterator() {
		if(recordIndex != recordIndexStart - 1) {
			throw new UnsupportedOperationException();
		}
		return this;
	}
}
