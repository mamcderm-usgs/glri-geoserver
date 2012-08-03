package gov.usgs.cida.prms;

import java.util.List;
import org.joda.time.DateTime;

/**
 *
 * @author tkunicki
 */
class PRMSAnimationRecordImpl implements PRMSAnimationRecord<Float> {
    private final List<RecordEntryDescriptor> recordEntryDescriptors;
    private final int recordIndex;
    private final char[] recordBuffer;

    public PRMSAnimationRecordImpl(List<RecordEntryDescriptor> recordEntryDescriptors, int recordIndex, char[] recordBuffer) {
        this.recordEntryDescriptors = recordEntryDescriptors;
        this.recordIndex = recordIndex;
        this.recordBuffer = recordBuffer;
    }

    @Override
    public int getIndex() {
        return recordIndex;
    }

    @Override
    public DateTime getTimeStamp() {
        return PRMSAnimationFileUtility.quickExtractRecordAsDateTime(recordEntryDescriptors.get(0), recordBuffer);
    }

    @Override
    public Integer getNHRU() {
        return PRMSAnimationFileUtility.quickExtractRecordAsInt(recordEntryDescriptors.get(1), recordBuffer);
    }

    @Override
    public Float getValue(int columnIndex) {
        if (columnIndex > 1) {
            return PRMSAnimationFileUtility.quickExtractRecordAsFloat(recordEntryDescriptors.get(columnIndex), recordBuffer);
        } else {
            throw new IllegalArgumentException("Can't use this method for column indices < 2");
        }
    }

    @Override
    public int getColumnCount() {
        return recordEntryDescriptors.size();
    }
    
}
