package gov.usgs.cida.prms;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationFileUtility {

	public static final String CHARSET = "US-ASCII";

	public static final char COMMENT_DELIMITER = '#';

	public static final String DATE_FORMAT = "yyyy-MM-dd:HH:mm:ss"; //"yyyy-MM-dd:HH:mm:ss";

    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DATE_FORMAT).withZoneUTC();

	public static List<RecordEntryDescriptor> generateRecordEntryDescriptors(String record, String[] entryNames) {
		int entryCount = entryNames.length;

		List<RecordEntryDescriptor> list = new ArrayList<RecordEntryDescriptor>(entryCount);

		int lastOffset = 0;
		int entryCountMinusOne = entryCount - 1;
		for(int entryIndex = 0; entryIndex < entryCountMinusOne; ++entryIndex) {
			int currentOffset = record.indexOf('\t', lastOffset);
			int length = currentOffset - lastOffset;
			RecordEntryDescriptor.Type type = determineRecordType(entryIndex, record.substring(lastOffset, currentOffset));
			list.add(new RecordEntryDescriptor(
					entryNames[entryIndex],
					entryIndex,
					type,
					lastOffset,
					length,
					type == RecordEntryDescriptor.Type.INT));
			lastOffset = currentOffset + 1;
		}

		int length = record.length() - lastOffset;
		RecordEntryDescriptor.Type type = determineRecordType(entryCountMinusOne, record.substring(lastOffset));
		list.add(new RecordEntryDescriptor(
					entryNames[entryCountMinusOne],
					entryCountMinusOne,
					type,
					lastOffset,
					length,
					type == RecordEntryDescriptor.Type.INT));

		return list;
	}

	private static RecordEntryDescriptor.Type determineRecordType(int index, String value) {
		switch (index) {
			case 0:
				return RecordEntryDescriptor.Type.STRING;
			case 1:
				return RecordEntryDescriptor.Type.INT;
			default:
				return RecordEntryDescriptor.Type.FLOAT;
		}
	}
    
	public static String quickExtractRecordAsString(char recordBuffer[], RecordEntryDescriptor descriptor) {
		if(descriptor.isTrimRequired()) {
			int offsetTrimmed = descriptor.getOffset();
			int lengthTrimmed = descriptor.getLength();
			while(recordBuffer[offsetTrimmed] == ' ') {
				++offsetTrimmed;
				--lengthTrimmed;
			}
			return new String(recordBuffer, offsetTrimmed, lengthTrimmed);
		} else {
			return new String(recordBuffer, descriptor.getOffset(), descriptor.getLength());
		}
	}

    public static DateTime quickExtractRecordAsDateTime(RecordEntryDescriptor descriptor, char recordBuffer[]) {
        String recordAsString = quickExtractRecordAsString(recordBuffer, descriptor);
        return dateTimeFormatter.parseDateTime(recordAsString);
    }
    
	public static int quickExtractRecordAsInt(RecordEntryDescriptor descriptor, char[] recordBuffer) {
		return Integer.parseInt(quickExtractRecordAsString(recordBuffer, descriptor));
	}

	public static float quickExtractRecordAsFloat(RecordEntryDescriptor descriptor, char[] recordBuffer) {
		return Float.parseFloat(quickExtractRecordAsString(recordBuffer, descriptor));
	}

}
