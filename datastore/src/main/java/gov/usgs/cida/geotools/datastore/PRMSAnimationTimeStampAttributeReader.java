package gov.usgs.cida.geotools.datastore;

import gov.usgs.cida.prms.PRMSAnimationFileMetaData;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.data.AttributeReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationTimeStampAttributeReader implements AttributeReader {
    
    private final SimpleFeatureType featureType;
    private final PRMSAnimationFileMetaData animationFileMetaData;
    
    private int timeStampIndex; // before next() called
    
    PRMSAnimationTimeStampAttributeReader(PRMSAnimationFileMetaData animationFileMetaData, SimpleFeatureType featureType) throws IOException {
        this.animationFileMetaData = animationFileMetaData;
        this.featureType = featureType;
        this.timeStampIndex = 0;
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
        // nothing to do...
    }

    @Override
    public boolean hasNext() throws IOException {
        return (timeStampIndex + 1) < animationFileMetaData.getTimeStepCount();
    }

    @Override
    public void next() throws IOException, IllegalArgumentException, NoSuchElementException {
        if (hasNext()) {
            ++timeStampIndex;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public Object read(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (index < 1) {
            return animationFileMetaData.getTimeStepList().get(timeStampIndex).toDate();
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }
    
}
