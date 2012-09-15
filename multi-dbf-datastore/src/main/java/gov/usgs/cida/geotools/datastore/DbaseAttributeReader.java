package gov.usgs.cida.geotools.datastore;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.data.AttributeReader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class DbaseAttributeReader implements AttributeReader {
    
    private final SimpleFeatureType featureType;
    private final FieldIndexedDbaseFileReader dbaseReader;
    private final int attributeCount;
    
    private int[] dBaseFieldIndices;
    private FieldIndexedDbaseFileReader.Row dbaseRow;

    DbaseAttributeReader(FieldIndexedDbaseFileReader dbaseReader, SimpleFeatureType featureType) throws IOException {
        this.featureType = featureType;
        this.dbaseReader = dbaseReader;
        this.attributeCount = featureType.getAttributeCount();
        
        dBaseFieldIndices = new int[attributeCount];
        for (int attributeIndex = 0; attributeIndex < attributeCount; ++attributeIndex) {
            Object o = featureType.getType(attributeIndex).getUserData().get(DbaseShapefileDataStore.KEY_FIELD_INDEX);
            dBaseFieldIndices[attributeIndex] = o instanceof Integer ?
                    ((Integer)o).intValue() :
                    -1;
        }
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
        dbaseReader.close();
    }

    @Override
    public boolean hasNext() throws IOException {
        return dbaseReader.hasNext();
    }

    @Override
    public void next() throws IOException, IllegalArgumentException, NoSuchElementException {
        dbaseRow = dbaseReader.readRow();
    }

    @Override
    public Object read(int index) throws IOException, ArrayIndexOutOfBoundsException {
        if (index < attributeCount) {
            return dbaseRow.read(dBaseFieldIndices[index]);
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }
}
