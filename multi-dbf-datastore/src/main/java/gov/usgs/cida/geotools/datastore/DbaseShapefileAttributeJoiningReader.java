package gov.usgs.cida.geotools.datastore;

import com.vividsolutions.jts.geom.Envelope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.data.shapefile.dbf.IndexedDbaseFileReader;
import org.geotools.renderer.ScreenMap;
import org.opengis.feature.type.AttributeDescriptor;


/**
 *
 * @author tkunicki
 */
public class DbaseShapefileAttributeJoiningReader  extends ShapefileAttributeReader {
    
    private final ShapefileAttributeReader delegate;
    private final int shapefileJoinAttributeIndex;
    private final FieldIndexedDbaseFileReader dbaseReader;
    
    private FieldIndexedDbaseFileReader.Row dBaseRow;
    private int[] dBaseFieldIndices;
    
    public DbaseShapefileAttributeJoiningReader(ShapefileAttributeReader delegate, FieldIndexedDbaseFileReader dbaseReader, int shapefileJoinAttributeIndex) throws IOException {
        super(hack(delegate), null, null); // lame duck
        this.delegate = delegate;
        this.shapefileJoinAttributeIndex = shapefileJoinAttributeIndex;
        this.dbaseReader = dbaseReader;
        
        int attributeCount = getAttributeCount();
        dBaseFieldIndices = new int[attributeCount];
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            Object o = getAttributeType(attributeIndex).getUserData().get(DbaseShapefileDataStore.KEY_FIELD_INDEX);
            dBaseFieldIndices[attributeIndex] = o instanceof Integer ?
                    ((Integer)o).intValue() :
                    -1;
        }
    }

    @Override
    public void close() throws IOException {
        try { delegate.close(); } catch (IOException ignore) { }
        try { dbaseReader.close(); } catch (IOException ignore) { }
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
        if (dbaseReader.setCurrentRecordByValue(delegate.read(shapefileJoinAttributeIndex))) {
            dBaseRow = dbaseReader.readRow();
        } else {
            dBaseRow = null;
        }
    }

    @Override
    public Object read(int attributeIndex) throws IOException, ArrayIndexOutOfBoundsException {
        int dBaseFieldIndex = dBaseFieldIndices[attributeIndex];
        if (dBaseFieldIndex < 0) {
            return delegate.read(attributeIndex);
        } else {
            return dBaseRow == null ? null : dBaseRow.read(dBaseFieldIndex);
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
