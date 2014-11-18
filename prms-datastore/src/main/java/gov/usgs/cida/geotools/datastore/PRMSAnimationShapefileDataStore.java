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
	
	/** Attrib descriptor key to indicate which column the column is in the nhru file.  Value should be an Integer */
	public final static String NHRU_FILE_ATTRIB_COLUMN = "NHRU_FILE_ATTRIB_COLUMN";
     
    private final URL animationURL;
    private String shapefileNHRUAttributeName;	//not final so that it can be updated to the correct case
    
    private final PRMSAnimationFileMetaData animationFileMetaData;
	
	/** Combined list of attrib descriptors which is also used as a lock for building it.  Only an unmodifiable version is returned. */
	private final List<AttributeDescriptor> attributeDescriptors = new ArrayList<AttributeDescriptor>();

    public PRMSAnimationShapefileDataStore(URI namespaceURI, URL prmsAnimationURL, URL shapefileURL, String shapefileNHRUAttributeName) throws MalformedURLException, IOException {
        super(shapefileURL, namespaceURI, true, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
        
        this.animationURL = prmsAnimationURL;
        
        this.shapefileNHRUAttributeName = shapefileNHRUAttributeName;
   
        animationFileMetaData = PRMSAnimationFileMetaData.getMetaData(prmsAnimationURL);
		
		
		//Force reading of the attributes, which has the side-effect of normalizing
		//the shapefileNHRUAttributeName to handle case issues.
		this.readAttributes();
    }

    private Set<String> shapefileAttributeNames;
    private Set<String> animationAttributeNames;
    
    private int animationJoinValueOffset;
	
    @Override
    protected final List<AttributeDescriptor> readAttributes() throws IOException {
		
		synchronized (attributeDescriptors) {
			
			if (attributeDescriptors.size() > 0) {
				return Collections.unmodifiableList(attributeDescriptors);
			} else {

				List<AttributeDescriptor> shapefileAttributeDescriptors = super.readAttributes();

				shapefileAttributeNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
					
					String name = attributeDescriptor.getLocalName();
					shapefileAttributeNames.add(name);
					
					//Update the shapefileNHRUAttributeName to be case correct
					if (name.equalsIgnoreCase(shapefileNHRUAttributeName)) {
						shapefileNHRUAttributeName = name;
					}
				}

				int recordEntryCount = animationFileMetaData.getRecordEntryCount();

				List<AttributeDescriptor> animationAttributeDescriptors = new ArrayList<AttributeDescriptor>(recordEntryCount);

				AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();


				atBuilder.addUserData(NHRU_FILE_ATTRIB_COLUMN, 0);
				animationAttributeDescriptors.add(atBuilder.
						binding(Date.class).
						buildDescriptor(ATTRIBUTE_TIMESTAMP));

				atBuilder.addUserData(NHRU_FILE_ATTRIB_COLUMN, 1);
				if (!shapefileAttributeNames.contains(ATTRIBUTE_NHRU)) {
					animationAttributeDescriptors.add(atBuilder.
							binding(Integer.class).
							buildDescriptor(ATTRIBUTE_NHRU));
				}

				List<RecordEntryDescriptor> recordEntryDescriptors = animationFileMetaData.getRecordEntryDescriptors();
				for (int recordEntryIndex = 2; recordEntryIndex < recordEntryCount; ++recordEntryIndex) {
					RecordEntryDescriptor recordEntryDescriptor = recordEntryDescriptors.get(recordEntryIndex);
					String recordEntryName = recordEntryDescriptor.getName();
					if (!shapefileAttributeNames.contains(recordEntryName)) {

						atBuilder.addUserData(NHRU_FILE_ATTRIB_COLUMN, recordEntryIndex);
						animationAttributeDescriptors.add(atBuilder.
							binding(Float.class).
							buildDescriptor(recordEntryName));
					}
				}

				animationAttributeNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
				for (AttributeDescriptor attributeDescriptor : animationAttributeDescriptors) {
					animationAttributeNames.add(attributeDescriptor.getLocalName());
				}

				animationJoinValueOffset = ((Number)animationFileMetaData.getRecordEntryRanges().get(animationFileMetaData.getRecordEntryIndex("nhru")).getMinimum()).intValue();


				attributeDescriptors.addAll(shapefileAttributeDescriptors);
				attributeDescriptors.addAll(animationAttributeDescriptors);

				return Collections.unmodifiableList(attributeDescriptors);
			}
		}
		
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        if (requiresShapefileAttributes(query)) {
            if (requiresAnimationAttributes(query)) {
                // make sure join attribute is in property list if we need to join!
                String[] properties = query.getPropertyNames();
                int joinIndex = Arrays.asList(properties).indexOf(shapefileNHRUAttributeName);
                if (joinIndex == -1) {
                    int tailIndex = properties.length;
                    properties = Arrays.copyOf(properties, tailIndex + 1);
                    properties[tailIndex] = shapefileNHRUAttributeName;
                    query.setPropertyNames(properties);
                }
            }
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
    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query, String[] properties) throws IOException {
        if (requiresAnimationAttributes(query)) {
            DateTime timeStamp = extractTimeStampFromQuery(query);
            int timeStepIndex = timeStamp != null ? animationFileMetaData.getTimeStepIndex(timeStamp) : 0;
            int joinIndex = Arrays.asList(properties).indexOf(shapefileNHRUAttributeName);
            return new PRMSAnimationShapefileAttributeJoiningReader(super.getAttributesReader(true, query, properties), animationFileMetaData, joinIndex, animationJoinValueOffset, timeStepIndex);
        } else {
            return super.getAttributesReader(readDBF, query, properties);
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
