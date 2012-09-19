package gov.usgs.cida.geotools.datastore;

import gov.usgs.cida.prms.PRMSAnimationFileMetaData;
import gov.usgs.cida.prms.RecordEntryDescriptor;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileAttributeReader;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.joda.time.DateTime;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationShapefileDataStore extends ShapefileDataStore {

    private final static String ATTRIBUTE_TIMESTAMP = "timestamp";
    private final static String ATTRIBUTE_NHRU = "nhru";
     
    private final URL animationURL;
    private final String shapefileNHRUAttributeName;
    
    private final PRMSAnimationFileMetaData animationFileMetaData;

    public PRMSAnimationShapefileDataStore(URI namespaceURI, URL prmsAnimationURL, URL shapefileURL, String shapefileNHRUAttributeName) throws MalformedURLException, IOException {
        super(shapefileURL, namespaceURI, true, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
        
        this.animationURL = prmsAnimationURL;
        
        this.shapefileNHRUAttributeName = shapefileNHRUAttributeName;
   
        animationFileMetaData = PRMSAnimationFileMetaData.getMetaData(prmsAnimationURL);
    }

    private List<AttributeDescriptor> shapefileAttributeDescriptors;
    private List<AttributeDescriptor> animationAttributeDescriptors;
    private List<AttributeDescriptor> attributeDescriptors;
    private ArrayList<String> shapefileAttributeNames;
    private ArrayList<String> animationAttributeNames;
    private int shapefileJoinAttributeIndex;
    private int animationJoinValueOffset;
    @Override
    protected List<AttributeDescriptor> readAttributes() throws IOException {
        shapefileAttributeDescriptors = super.readAttributes();
        
        int recordEntryCount = animationFileMetaData.getRecordEntryCount();
        
        animationAttributeDescriptors = new ArrayList<AttributeDescriptor>(recordEntryCount);
        
        AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();
        
//        int maxOccurs = animationFileMetaData.getTimeStepCount();
        
        animationAttributeDescriptors.add(atBuilder.
//                minOccurs(1).
//                maxOccurs(maxOccurs).
//                nillable(false).
                binding(Date.class).
                buildDescriptor(ATTRIBUTE_TIMESTAMP));
        animationAttributeDescriptors.add(atBuilder.
//                minOccurs(1).
//                maxOccurs(maxOccurs).
//                nillable(false).
                binding(Integer.class).
                buildDescriptor(ATTRIBUTE_NHRU));          

        
        List<RecordEntryDescriptor> recordEntryDescriptors = animationFileMetaData.getRecordEntryDescriptors();
        for (int recordEntryIndex = 2; recordEntryIndex < recordEntryCount; ++recordEntryIndex) {
            RecordEntryDescriptor recordEntryDescriptor = recordEntryDescriptors.get(recordEntryIndex);
            animationAttributeDescriptors.add(atBuilder.
//                minOccurs(1).
//                maxOccurs(maxOccurs).
//                nillable(false).
                binding(Float.class).
                buildDescriptor(recordEntryDescriptor.getName()));
        }
        
                
        shapefileAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
            shapefileAttributeNames.add(attributeDescriptor.getLocalName());
        }
        animationAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : animationAttributeDescriptors) {
            animationAttributeNames.add(attributeDescriptor.getLocalName());
        }
        
        shapefileJoinAttributeIndex = shapefileAttributeNames.indexOf(shapefileNHRUAttributeName);
        
        animationJoinValueOffset = ((Number)animationFileMetaData.getRecordEntryRanges().get(animationFileMetaData.getRecordEntryIndex("nhru")).getMinimum()).intValue();
        
        attributeDescriptors = new ArrayList<AttributeDescriptor>(
                shapefileAttributeDescriptors.size() +
                animationAttributeDescriptors.size());
        attributeDescriptors.addAll(shapefileAttributeDescriptors);
        attributeDescriptors.addAll(animationAttributeDescriptors);
        return attributeDescriptors;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        if (requiresShapefileAttributes(query)) {
            return super.getFeatureReader(typeName, query);
        } else {
            try {
                List<String> propertyNames = Arrays.asList(query.getPropertyNames());
                SimpleFeatureType subTypeSchema = DataUtilities.createSubType(getSchema(), propertyNames.toArray(new String[0]));
                boolean timeStampOnly = propertyNames.size() == 1 && ATTRIBUTE_TIMESTAMP.equals(propertyNames.get(0));
                if (timeStampOnly) {
                    return new DefaultFeatureReader(new PRMSAnimationTimeStampAttributeReader(animationFileMetaData, subTypeSchema), subTypeSchema);
                } else {
                    return new DefaultFeatureReader(new PRMSAnimationAttributeReader(animationFileMetaData, subTypeSchema), subTypeSchema);
                }
            } catch (SchemaException ex) {
                // hack
                throw new IOException(ex);
            }
        }
    }

    @Override
    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query) throws IOException {
        if (requiresAnimationAttributes(query)) {
            DateTime timeStamp = extractTimeStampFromQuery(query);
            int timeStepIndex = timeStamp != null ? animationFileMetaData.getTimeStepIndex(timeStamp) : 0;
            return new PRMSAnimationShapefileAttributeJoiningReader(super.getAttributesReader(true, query), animationFileMetaData, shapefileJoinAttributeIndex, animationJoinValueOffset, timeStepIndex);
        } else {
            return super.getAttributesReader(readDBF, query);
        }
    }
    
    private boolean requiresShapefileAttributes(Query query) {
        return QueryUtil.requiresAttributes(query, shapefileAttributeNames);
    }
    
    private boolean requiresAnimationAttributes(Query query) {
        return QueryUtil.requiresAttributes(query, animationAttributeNames);
    }
    
    private DateTime extractTimeStampFromQuery(Query query) {
        Date timestamp = QueryUtil.extractValueFromQueryFilter(query, ATTRIBUTE_TIMESTAMP, Date.class);
        return timestamp == null ? null : new DateTime(timestamp);
    }
    
    @Override
    protected String createFeatureTypeName() {
        String path = animationURL.getPath();
        File file = new File(path);
        String name = file.getName();
        int suffixIndex = name.lastIndexOf("animation.nhru");
        if (suffixIndex > -1) {
            return name.substring(0, suffixIndex -1);
        }
        return path;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return super.getBounds(query);
    }
}
