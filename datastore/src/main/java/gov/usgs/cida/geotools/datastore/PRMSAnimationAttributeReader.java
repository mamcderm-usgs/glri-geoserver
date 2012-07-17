package gov.usgs.cida.geotools.datastore;

import gov.usgs.cida.prms.PRMSAnimationFileMetaData;
import gov.usgs.cida.prms.PRMSAnimationRecord;
import gov.usgs.cida.prms.PRMSAnimationRecordBuffer;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.data.AttributeReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationAttributeReader implements AttributeReader {
    
    private final SimpleFeatureType featureType;
    private final PRMSAnimationFileMetaData animationFileMetaData;
    private final int readerAttributeCount;
    
    private int readerRecordIndex; // before next() called
    
    private PRMSAnimationRecordBuffer readerRecordBuffer;
    private PRMSAnimationRecord readerRecord;
    
    private int[] readerAttributeToRecordEntryIndices;

    PRMSAnimationAttributeReader(PRMSAnimationFileMetaData animationFileMetaData, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.animationFileMetaData = animationFileMetaData;
        this.readerAttributeCount = featureType.getAttributeCount();
        this.readerRecordIndex = 0;
        readerAttributeToRecordEntryIndices = new int[readerAttributeCount];
        for (int readerAttributeIndex = 0; readerAttributeIndex < readerAttributeCount; ++readerAttributeIndex) {
            readerAttributeToRecordEntryIndices[readerAttributeIndex] = animationFileMetaData.getRecordEntryIndex(featureType.getDescriptor(readerAttributeIndex).getLocalName());
        }
        readerRecordBuffer = new PRMSAnimationRecordBuffer(animationFileMetaData);
    }

    @Override
    public int getAttributeCount() {
        return featureType.getAttributeCount();
    }

    @Override
    public AttributeDescriptor getAttributeType(int index) throws ArrayIndexOutOfBoundsException {
        return featureType.getDescriptor(index);
    }

    @Override
    public void close() throws IOException {
        try {
            readerRecordBuffer.close();
        } catch (IOException e) {
            /* don't care */
        }
    }

    @Override
    public boolean hasNext() throws IOException {
        return (readerRecordIndex + 1) < animationFileMetaData.getRecordCount();
    }

    @Override
    public void next() throws IOException, IllegalArgumentException, NoSuchElementException {
        if (hasNext()) {
            readerRecord = readerRecordBuffer.getRecord(readerRecordIndex);
            ++readerRecordIndex;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public Object read(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (index < readerAttributeCount) {
            int recordEntryIndex = readerAttributeToRecordEntryIndices[index];
            switch (recordEntryIndex) {
                case 0:
                    return readerRecord.getTimeStamp().toDate();
                case 1:
                    return readerRecord.getNHRU();
                default:
                    return readerRecord.getValue(recordEntryIndex);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }
    
}
