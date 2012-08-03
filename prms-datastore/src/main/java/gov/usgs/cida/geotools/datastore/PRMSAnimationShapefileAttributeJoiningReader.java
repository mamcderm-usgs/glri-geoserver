package gov.usgs.cida.geotools.datastore;

import com.vividsolutions.jts.geom.Envelope;
import gov.usgs.cida.prms.PRMSAnimationFileMetaData;
import gov.usgs.cida.prms.PRMSAnimationRecord;
import gov.usgs.cida.prms.PRMSAnimationRecordBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.renderer.ScreenMap;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationShapefileAttributeJoiningReader extends ShapefileAttributeReader {
    
    private final ShapefileAttributeReader delegate;
    private final int animationDescriptorIndexOffset;
    private final int shapefileJoinAttributeIndex;
    private final int animationJoinValueOffset;
    private final int animationTimeStepRecordOffset;
    
    private PRMSAnimationRecordBuffer animationRecordBuffer;
    private PRMSAnimationRecord animationRecord;

    public PRMSAnimationShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, PRMSAnimationFileMetaData animationFileMetaData, int shapefileJoinAttributeIndex, int animationJoinValueOffset) throws IOException {
        this(delegate, animationFileMetaData, shapefileJoinAttributeIndex, animationJoinValueOffset, 0);
    }

    public PRMSAnimationShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, PRMSAnimationFileMetaData animationFileMetaData, int shapefileJoinAttributeIndex, int animationJoinValueOffset, int timeStepIndex) throws IOException {
        super(hack(delegate), null, null); // lame duck
        this.delegate = delegate;
        this.animationDescriptorIndexOffset = getAttributeCount() - animationFileMetaData.getRecordEntryCount();
        this.shapefileJoinAttributeIndex = shapefileJoinAttributeIndex;
        this.animationJoinValueOffset = animationJoinValueOffset;
        this.animationTimeStepRecordOffset = timeStepIndex * animationFileMetaData.getTimeStepRecordCount();
        animationRecordBuffer = new PRMSAnimationRecordBuffer(animationFileMetaData, animationTimeStepRecordOffset, animationTimeStepRecordOffset + animationFileMetaData.getTimeStepRecordCount());
    }

    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException e) {
            /* don't care */
        }
        try {
            animationRecordBuffer.close();
        } catch (IOException e) {
            /* don't care */
        }
    }

    @Override
    public int getRecordNumber() {
        return delegate.getRecordNumber();
    }

    @Override
    public boolean hasNext() throws IOException {
        return delegate.hasNext();
    }

    @Override
    public void next() throws IOException {
        delegate.next();
        animationRecord = animationRecordBuffer.getRecord(animationTimeStepRecordOffset + ((Number) delegate.read(shapefileJoinAttributeIndex)).intValue() - animationJoinValueOffset);
    }

    @Override
    public Object read(int attributeIndex) throws IOException, ArrayIndexOutOfBoundsException {
        if (attributeIndex < animationDescriptorIndexOffset) {
            return delegate.read(attributeIndex);
        } else {
            int animationRecordIndex = attributeIndex - animationDescriptorIndexOffset;
            switch (animationRecordIndex) {
                case 0:
                    return animationRecord.getTimeStamp().toDate();
                case 1:
                    return animationRecord.getNHRU();
                default:
                    return animationRecord.getValue(animationRecordIndex);
            }
        }
    }

    @Override
    public void setScreenMap(ScreenMap screenMap) {
        delegate.setScreenMap(screenMap);
    }

    @Override
    public void setSimplificationDistance(double distance) {
        delegate.setSimplificationDistance(distance);
    }

    @Override
    public void setTargetBBox(Envelope envelope) {
        delegate.setTargetBBox(envelope);
    }

    private static List<AttributeDescriptor> hack(ShapefileAttributeReader delegate) {
        int attributeCount = delegate.getAttributeCount();
        List<AttributeDescriptor> descriptors = new ArrayList<AttributeDescriptor>(delegate.getAttributeCount());
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            descriptors.add(delegate.getAttributeType(attributeIndex));
        }
        return descriptors;
    }
    
}
