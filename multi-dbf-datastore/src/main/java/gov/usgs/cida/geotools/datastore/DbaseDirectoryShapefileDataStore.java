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
public class DbaseDirectoryShapefileDataStore extends ShapefileDataStore {
    
    public final static String KEY_FIELD_INDEX = "dbaseFieldIndex";
    public final static String KEY_READER_INDEX = "dbaseReaderIndex";
    
    private final URL dBaseDirectoryURL;
    private final String shapefileJoinAttributeName;
    private final List<File> joinableDbaseFiles;

    private List<String> shapefileAttributeNames;
    private List<String> dbaseAttributeNames;
    private int shapefileJoinAttributeIndex;
    
    private Map<File, Map<Object, Integer>> fileFieldIndexMap = new HashMap<File, Map<Object, Integer>>();
    
    public DbaseDirectoryShapefileDataStore(URI namespaceURI, URL dbaseDirectoryURL, URL shapefileURL, String shapefileJoinAttributeName) throws MalformedURLException, IOException {
        super(shapefileURL, namespaceURI, true, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
        
        this.dBaseDirectoryURL = dbaseDirectoryURL;
        
        this.shapefileJoinAttributeName = shapefileJoinAttributeName;
        
        joinableDbaseFiles = createJoinableDbaseFileList();
        if (joinableDbaseFiles.isEmpty()) {
            throw new IllegalArgumentException("no joinable dbf files on field " + shapefileJoinAttributeName + " in " + dbaseDirectoryURL);
        }
        
        // NOTE: if this method is removed from constructor it should be synchronized...
        createDbaseReaderList();
    }
    
    @Override
    protected List<AttributeDescriptor> readAttributes() throws IOException {
        List<AttributeDescriptor> shapefileAttributeDescriptors = super.readAttributes();
        
        ArrayList<AttributeDescriptor> dbaseFileAttributeDescriptors = new ArrayList<AttributeDescriptor>();
        dbaseAttributeNames = new ArrayList<String>();
        
        List<FieldIndexedDbaseFileReader> dbaseReaderList = createDbaseReaderList();
        int dbaseReaderCount = dbaseReaderList.size();
        for (int dbaseReaderIndex = 0; dbaseReaderIndex < dbaseReaderCount; ++dbaseReaderIndex) {
            
            FieldIndexedDbaseFileReader dbaseReader = dbaseReaderList.get(dbaseReaderIndex);
            try {

                DbaseFileHeader dbaseFileHeader = dbaseReader.getHeader();
                int dbaseFieldCount = dbaseFileHeader.getNumFields();

                AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();

                for (int dbaseFieldIndex = 0; dbaseFieldIndex < dbaseFieldCount; ++dbaseFieldIndex) {
                    String dbaseFieldName = dbaseFileHeader.getFieldName(dbaseFieldIndex);
                    if (!shapefileJoinAttributeName.equalsIgnoreCase(dbaseFieldName)) {
                        dbaseFileAttributeDescriptors.add(atBuilder.
                            userData(KEY_READER_INDEX, dbaseReaderIndex).
                            userData(KEY_FIELD_INDEX, dbaseFieldIndex).
                            binding(dbaseFileHeader.getFieldClass(dbaseFieldIndex)).
                            buildDescriptor(dbaseFileHeader.getFieldName(dbaseFieldIndex)));
                    }
                }

                for (AttributeDescriptor attributeDescriptor : dbaseFileAttributeDescriptors) {
                    dbaseAttributeNames.add(attributeDescriptor.getLocalName());
                }

            } finally {
                if (dbaseReader != null) {
                    try { dbaseReader.close(); } catch (IOException ignore) {}
                }
            }
        }
        
        shapefileAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
            shapefileAttributeNames.add(attributeDescriptor.getLocalName());
        }
        shapefileJoinAttributeIndex = shapefileAttributeNames.indexOf(shapefileJoinAttributeName);
        
        List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>(
                shapefileAttributeDescriptors.size() +
                dbaseFileAttributeDescriptors.size());
        attributeDescriptors.addAll(shapefileAttributeDescriptors);
        attributeDescriptors.addAll(dbaseFileAttributeDescriptors);
        
        return attributeDescriptors;
    }
    
    // NOTE:  not synchronized because this is called in contructor,
    // synchronization of initialization of fileIndexMap is the concern...
    private List<FieldIndexedDbaseFileReader> createDbaseReaderList() throws IOException {
        List<FieldIndexedDbaseFileReader> dbaseReaderList = new ArrayList<FieldIndexedDbaseFileReader>(joinableDbaseFiles.size());
        for (File dbaseFile : joinableDbaseFiles) {
            FileChannel dbaseFileChannel = (new FileInputStream(dbaseFile)).getChannel();
            FieldIndexedDbaseFileReader dbaseReader = new FieldIndexedDbaseFileReader(dbaseFileChannel);
            Map<Object, Integer> fieldIndexMap = fileFieldIndexMap.get(dbaseFile);
            if (fieldIndexMap == null) {
                dbaseReader.buildFieldIndex(shapefileJoinAttributeName);
                fileFieldIndexMap.put(dbaseFile, Collections.unmodifiableMap(dbaseReader.getFieldIndex()));
            } else {
                dbaseReader.setFieldIndex(fieldIndexMap);
            }
            dbaseReaderList.add(dbaseReader);
        }
        return dbaseReaderList;
    }
    
    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        if (requiresShapefileAttributes(query)) {
            return super.getFeatureReader(typeName, query);
        } else {
            try {
                List<String> propertyNames = Arrays.asList(query.getPropertyNames());
                SimpleFeatureType subTypeSchema = DataUtilities.createSubType(getSchema(), propertyNames.toArray(new String[0]));
                return new DefaultFeatureReader(new DbaseListAttributeReader(createDbaseReaderList(), subTypeSchema), subTypeSchema);
            } catch (SchemaException ex) {
                // hack
                throw new IOException(ex);
            }
        }
    }

    @Override
    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query) throws IOException {
        if (requiresDbaseAttributes(query)) {
            return new DbaseListShapefileAttributeJoiningReader(super.getAttributesReader(true, query), createDbaseReaderList(), shapefileJoinAttributeIndex);
        } else {
            return super.getAttributesReader(readDBF, query);
        }
    }
    
    private boolean requiresShapefileAttributes(Query query) {
        return QueryUtil.requiresAttributes(query, shapefileAttributeNames);
    }
    
    private boolean requiresDbaseAttributes(Query query) {
        return QueryUtil.requiresAttributes(query, dbaseAttributeNames);
    }
    
    @Override
    protected String createFeatureTypeName() {
        String path = dBaseDirectoryURL.getPath();
        File file = new File(path);
        String name = file.getName();
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
    
    private List<File> createJoinableDbaseFileList() throws IOException {
        if(!"file".equals(dBaseDirectoryURL.getProtocol())) {
            throw new IllegalArgumentException("only url \"file\" protocols accepted");
        }
        File urlAsFile = new File(dBaseDirectoryURL.getPath());
        if (!urlAsFile.canRead()) {
            throw new IllegalArgumentException("can't read " + dBaseDirectoryURL);
        }
        if (!urlAsFile.isDirectory()) {
            throw new IllegalArgumentException(dBaseDirectoryURL + " must be a directory");
        }
        List<File> joinableDbaseFiles = new ArrayList<File>();
        for (File child : urlAsFile.listFiles()) {
            if (child.getName().toLowerCase().endsWith("dbf")) {
                FieldIndexedDbaseFileReader dbaseReader = null;
                try {
                    dbaseReader = new FieldIndexedDbaseFileReader((new FileInputStream(child)).getChannel());
                    DbaseFileHeader header = dbaseReader.getHeader();
                    if (header.getNumRecords() > 0) {
                        int fieldCount = header.getNumFields();
                        for (int fieldIndex = 0; fieldIndex < fieldCount && !joinableDbaseFiles.contains(child); ++fieldIndex) {
                            if (shapefileJoinAttributeName.equalsIgnoreCase(header.getFieldName(fieldIndex))) {
                                joinableDbaseFiles.add(child);
                            }
                        }
                    }
                } catch (IOException e) {
                    
                } finally {
                    if (dbaseReader != null) {
                        try { dbaseReader.close(); } catch (IOException ignore) {}
                    }
                }
            }
        }
        return Collections.unmodifiableList(joinableDbaseFiles);
    }

}

