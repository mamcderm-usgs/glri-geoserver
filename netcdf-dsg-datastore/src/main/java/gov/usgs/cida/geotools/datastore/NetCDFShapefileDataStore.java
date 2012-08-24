package gov.usgs.cida.geotools.datastore;

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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;

/**
 *
 * @author tkunicki
 */
public class NetCDFShapefileDataStore extends ShapefileDataStore {
    
    public final static String VARIABLE_KEY = "variable";
    public final static String EXTRACTOR_KEY = "extractor";
     
    private final URL netCDFURL;
    private final String shapefileStationAttributeName;
    
    private final FeatureDataset featureDataset;
    
    private VariableSimpleIF observationTimeVariable;

    public NetCDFShapefileDataStore(URI namespaceURI, URL netCDFURL, URL shapefileURL, String shapefileStationAttributeName) throws MalformedURLException, IOException {
        super(shapefileURL, namespaceURI, true, true, ShapefileDataStore.DEFAULT_STRING_CHARSET);
        
        this.netCDFURL = netCDFURL;
        
        this.shapefileStationAttributeName = shapefileStationAttributeName;
        
        featureDataset = FeatureDatasetFactoryManager.open(
                    FeatureType.STATION,
                    netCDFURL.toString(),
                    null,
                    new Formatter(System.err));
    }

    private List<AttributeDescriptor> shapefileAttributeDescriptors;
    private List<AttributeDescriptor> netCDFAttributeDescriptors;
    private List<AttributeDescriptor> attributeDescriptors;
    private ArrayList<String> shapefileAttributeNames;
    private ArrayList<String> netCDFAttributeNames;
    private int shapefileJoinAttributeIndex;
    
    @Override
    protected List<AttributeDescriptor> readAttributes() throws IOException {
        shapefileAttributeDescriptors = super.readAttributes();
        
        List<VariableSimpleIF> observationVariables = NetCDFUtil.getObservationVariables(featureDataset);
        observationTimeVariable = NetCDFUtil.getObservationTimeVariable(featureDataset);
        
//        observationVariables.remove(observationTimeVariable);
        VariableSimpleIF toRemove = null;
        for (VariableSimpleIF variable : observationVariables) {
            if (variable.getFullName().equals(observationTimeVariable.getFullName())) {
                toRemove = variable;
            }
        }
        if (toRemove != null) {
            observationVariables.remove(toRemove);
        }

        netCDFAttributeDescriptors = new ArrayList<AttributeDescriptor>(observationVariables.size());

        AttributeTypeBuilder atBuilder = new AttributeTypeBuilder();

//        int maxOccurs = // ...
        netCDFAttributeDescriptors.add(atBuilder.
//                minOccurs(1).
//                maxOccurs(maxOccurs).
//                nillable(false).
                userData(VARIABLE_KEY, observationTimeVariable).
                userData(EXTRACTOR_KEY, new NetCDFPointFeatureExtractor.TimeStamp()).
                binding(Date.class).
                buildDescriptor(observationTimeVariable.getShortName()));

        for (int observationVariableIndex = 0, observationVariableCount = observationVariables.size();
                observationVariableIndex < observationVariableCount; ++observationVariableIndex) {
            VariableSimpleIF observationVariable = observationVariables.get(observationVariableIndex);
            netCDFAttributeDescriptors.add(atBuilder.
//                minOccurs(1).
//                maxOccurs(maxOccurs).
//                nillable(false).
                userData(VARIABLE_KEY, observationVariable).
                userData(EXTRACTOR_KEY, NetCDFPointFeatureExtractor.generatePointFeatureExtractor(observationVariable)).
                binding(observationVariable.getDataType().getClassType()).
                buildDescriptor(observationVariable.getShortName()));
        }

        shapefileAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : shapefileAttributeDescriptors) {
            shapefileAttributeNames.add(attributeDescriptor.getLocalName());
        }
        netCDFAttributeNames = new ArrayList<String>();
        for (AttributeDescriptor attributeDescriptor : netCDFAttributeDescriptors) {
            netCDFAttributeNames.add(attributeDescriptor.getLocalName());
        }

        shapefileJoinAttributeIndex = shapefileAttributeNames.indexOf(shapefileStationAttributeName);

        attributeDescriptors = new ArrayList<AttributeDescriptor>(
                shapefileAttributeDescriptors.size() +
                netCDFAttributeDescriptors.size());
        attributeDescriptors.addAll(shapefileAttributeDescriptors);
        attributeDescriptors.addAll(netCDFAttributeDescriptors);

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
                boolean timeStampOnly = propertyNames.size() == 1 && observationTimeVariable.getShortName().equals(propertyNames.get(0));
                if (timeStampOnly) {
                    return new DefaultFeatureReader(new NetCDFTimeStampAttributeReader(featureDataset, subTypeSchema), subTypeSchema);
                } else {
                    return new DefaultFeatureReader(new NetCDFAttributeReader(featureDataset, subTypeSchema), subTypeSchema);
                }
            } catch (SchemaException ex) {
                // hack
                throw new IOException(ex);
            }
        }
    }

    @Override
    protected ShapefileAttributeReader getAttributesReader(boolean readDBF, Query query) throws IOException {
        if (requiresNetCDFAttributes(query)) {
            Date time = extractTimeStampFromQuery(query);
            return new NetCDFShapefileAttributeJoiningReader(super.getAttributesReader(true, query), featureDataset, shapefileJoinAttributeIndex, time);
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
    
    private boolean requiresNetCDFAttributes(Query query) {
        if (query == null) {
            return true;
        }
        if (query == Query.ALL) {
            return true;
        }
        List<String> propertyNames = Arrays.asList(query.getPropertyNames());
        List<String> attributeNames = Arrays.asList(DataUtilities.attributeNames(query.getFilter()));
        return !Collections.disjoint(propertyNames, netCDFAttributeNames) ||
               !Collections.disjoint(attributeNames, netCDFAttributeNames);
    }
    
    private Date extractTimeStampFromQuery(Query query) {
        if (query == null) {
            return null;
        }
        Filter filter = query.getFilter();
        if (filter == null) {
            return null;
        }
        NetCDFQueryTimeFilterExtractor extractor = new NetCDFQueryTimeFilterExtractor(observationTimeVariable.getShortName());
        filter.accept(extractor, null);
        return extractor.getTime();
    }
    
    @Override
    protected String createFeatureTypeName() {
        String path = netCDFURL.getPath();
        File file = new File(path);
        String name = file.getName();
        int suffixIndex = name.lastIndexOf("nc");
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
        if (featureDataset != null) {
            try { featureDataset.close(); } catch (IOException e) { }
        }
    }

}

