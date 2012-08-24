package gov.usgs.cida.geotools.datastore;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.geotools.data.*;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

/**
 *
 * @author tkunicki
 */
public class NetCDFDirectoryShapefileDataStore extends AbstractDataStore {

    private TreeMap<String, NetCDFShapefileDataStore> netCDFDataStoreMap;
    private ReferencedEnvelope bounds;

    public NetCDFDirectoryShapefileDataStore(URI namespaceURI, URL netCDFDirectoryURL, URL shapefileURL, String shapefileNHRUAttributeName) throws MalformedURLException, IOException {
        netCDFDataStoreMap = new TreeMap<String, NetCDFShapefileDataStore>();
        for (File file : getNetCDFFiles(netCDFDirectoryURL)) {
            NetCDFShapefileDataStore dataStore = new NetCDFShapefileDataStore(namespaceURI, file.toURI().toURL(), shapefileURL, shapefileNHRUAttributeName);
            dataStore.getSchema(); // prime schemas
            netCDFDataStoreMap.put(dataStore.getTypeNames()[0], dataStore);
        }
        
        NetCDFShapefileDataStore netCDFDataStore = netCDFDataStoreMap.firstEntry().getValue();
        bounds = netCDFDataStore.getBounds(Query.ALL);
    }
    
    private List<File> getNetCDFFiles(URL url) {
        if(!"file".equals(url.getProtocol())) {
            throw new IllegalArgumentException("only url \"file\" protocols accepted");
        }
        File urlAsFile = new File(url.getPath());
        if (!urlAsFile.canRead()) {
            throw new IllegalArgumentException("can't read " + url);
        }
        if (!urlAsFile.isDirectory()) {
            throw new IllegalArgumentException(url + " must be a directory");
        }
        List<File> animationFiles = new ArrayList<File>();
        for (File child : urlAsFile.listFiles()) {
            if (child.getName().endsWith("nc")) {
                animationFiles.add(child);
            }
        }
        return animationFiles;
    }    

    @Override
    public String[] getTypeNames() throws IOException {
        return netCDFDataStoreMap.keySet().toArray(new String[0]);
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return netCDFDataStoreMap.get(typeName).getSchema();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
        return netCDFDataStoreMap.get(typeName).getFeatureReader();
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction) throws IOException {
        return netCDFDataStoreMap.get(query.getTypeName()).getFeatureReader(query, transaction);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        return netCDFDataStoreMap.get(typeName).getFeatureReader(typeName, query);
    }

    @Override
    public void dispose() {
        for (NetCDFShapefileDataStore dataStore : netCDFDataStoreMap.values()) {
            dataStore.dispose();
        }
    }

    @Override
    protected ReferencedEnvelope getBounds(Query query) throws IOException {
        if (query.getFilter().equals(Filter.INCLUDE)) {
            return bounds;
        }
        return null; // too expensive
    }
    
}
