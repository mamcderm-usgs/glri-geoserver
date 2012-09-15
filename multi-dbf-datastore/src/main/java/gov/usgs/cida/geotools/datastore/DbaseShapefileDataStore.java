package gov.usgs.cida.geotools.datastore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.*;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.FieldIndexedDbaseFileReader;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class DbaseShapefileDataStore extends ShapefileDataStore {
    
    public final static String KEY_FIELD_INDEX = "dbfFieldIndex";
    
    private final URL dBaseURL;
    private final String shapefileJoinAttributeName;

    public DbaseShapefileDataStore(URI namespaceURI, URL dBaseURL, URL shapefileURL, String shapefileStationAttributeName) throws MalformedURLException, IOException {
        super(shapefileURL, namespaceURI, true, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
        
        this.dBaseURL = dBaseURL;
        
        this.shapefileJoinAttributeName = shapefileStationAttributeName;
        
        // NOTE: if this method is removed from constructor it should be synchronized...
        createDbaseReader();
    }

    private List<AttributeDescriptor> shapefileAttributeDescriptors;
    private List<AttributeDescriptor> dBaseFileAttributeDescriptors;
    private List<AttributeDescriptor> attributeDescriptors;
    private ArrayList<String> shapefileAttributeNames;
    private ArrayList<String> dbaseAttributeNames;
    private int shapefileJoinAttributeIndex;
    
    @Override
    protected List<AttributeDescriptor> readAttributes() throws IOException {
        shapefileAttributeDescriptors = super.readAttributes();
        
        FieldIndexedDbaseFileReader dbaseReader = createDbaseReader();
        DbaseFileHeader dbaseFileHeader = dbaseReader.getHeader();
        int dbaseFieldCount = dbaseFileHeader.getNumFields();
        dBaseFileAttributeDescriptors = new ArrayList<AttributeDescriptor>(dbaseFieldCount - 1);
        
        AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();

        for (int dbaseFieldIndex = 0; dbaseFieldIndex < dbaseFieldCount; ++dbaseFieldIndex) {
            String dbaseFieldName = dbaseFileHeader.getFieldName(dbaseFieldIndex);
            if (!shapefileJoinAttributeName.equalsIgnoreCase(dbaseFieldName)) {
                dBaseFileAttributeDescriptors.add(atBuilder.
                    userData(KEY_FIELD_INDEX, dbaseFieldIndex).
                    binding(dbaseFileHeader.getFieldClass(dbaseFieldIndex)).
                    buildDescriptor(dbaseFileHeader.getFieldName(dbaseFieldIndex)));
            }
        }

        shapefileAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
            shapefileAttributeNames.add(attributeDescriptor.getLocalName());
        }
        dbaseAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : dBaseFileAttributeDescriptors) {
            dbaseAttributeNames.add(attributeDescriptor.getLocalName());
        }

        shapefileJoinAttributeIndex = shapefileAttributeNames.indexOf(shapefileJoinAttributeName);

        attributeDescriptors = new ArrayList<AttributeDescriptor>(
                shapefileAttributeDescriptors.size() +
                dBaseFileAttributeDescriptors.size());
        attributeDescriptors.addAll(shapefileAttributeDescriptors);
        attributeDescriptors.addAll(dBaseFileAttributeDescriptors);

        return attributeDescriptors;
    }

    private Map<Object, Integer> fieldIndexMap;
    // NOTE:  not synchronized because this is called in contructor
    private FieldIndexedDbaseFileReader createDbaseReader() throws IOException {
        File dBaseFile = new File(dBaseURL.getFile());
        FileChannel dBaseFileChannel = (new FileInputStream(dBaseFile)).getChannel();
        FieldIndexedDbaseFileReader dbaseReader = new FieldIndexedDbaseFileReader(dBaseFileChannel);
        if (fieldIndexMap == null) {
            dbaseReader.buildFieldIndex(shapefileJoinAttributeName);
            fieldIndexMap = Collections.unmodifiableMap(dbaseReader.getFieldIndex());
        } else {
            dbaseReader.setFieldIndex(fieldIndexMap);
        }
        return dbaseReader;
    }
    
    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        if (requiresShapefileAttributes(query)) {
            return super.getFeatureReader(typeName, query);
        } else {
            try {
                List<String> propertyNames = Arrays.asList(query.getPropertyNames());
                SimpleFeatureType subTypeSchema = DataUtilities.createSubType(getSchema(), propertyNames.toArray(new String[0]));
                return new DefaultFeatureReader(new DbaseAttributeReader(createDbaseReader(), subTypeSchema), subTypeSchema);
            } catch (SchemaException ex) {
                // hack
                throw new IOException(ex);
            }
        }
    }

    @Override
    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query) throws IOException {
        if (requiresDbaseAttributes(query)) {
            return new DbaseShapefileAttributeJoiningReader(super.getAttributesReader(true, query), createDbaseReader(), shapefileJoinAttributeIndex);
        } else {
            return super.getAttributesReader(readDBF, query);
        }
    }
    
    private boolean requiresShapefileAttributes(Query query) {
        if (query == null) {
            return true;
        }
        if (query == Query.ALL) {
            return true;
        }
        List<String> propertyNames = Arrays.asList(query.getPropertyNames());
        List<String> attributeNames = Arrays.asList(DataUtilities.attributeNames(query.getFilter()));
        return  !Collections.disjoint(propertyNames, shapefileAttributeNames) ||
                !Collections.disjoint(attributeNames, shapefileAttributeNames);
    }
    
    private boolean requiresDbaseAttributes(Query query) {
        if (query == null) {
            return true;
        }
        if (query == Query.ALL) {
            return true;
        }
        List<String> propertyNames = Arrays.asList(query.getPropertyNames());
        List<String> attributeNames = Arrays.asList(DataUtilities.attributeNames(query.getFilter()));
        return !Collections.disjoint(propertyNames, dbaseAttributeNames) ||
               !Collections.disjoint(attributeNames, dbaseAttributeNames);
    }
    
    @Override
    protected String createFeatureTypeName() {
        String path = dBaseURL.getPath();
        File file = new File(path);
        String name = file.getName();
        int suffixIndex = name.lastIndexOf("dbf");
        if (suffixIndex > -1) {
            name = name.substring(0, suffixIndex -1);
        }
        name = name.replace(',', '_'); // Are there other characters?
        return name;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return super.getBounds(query);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

}

