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
public class PRMSAnimationDirectoryShapefileDataStore extends AbstractDataStore {

    private TreeMap<String, PRMSAnimationShapefileDataStore> prmsDataStoreMap;
    private ReferencedEnvelope bounds;

    public PRMSAnimationDirectoryShapefileDataStore(URI namespaceURI, URL prmsAnimationDirectoryURL, URL shapefileURL, String shapefileNHRUAttributeName) throws MalformedURLException, IOException {
        prmsDataStoreMap = new TreeMap<String, PRMSAnimationShapefileDataStore>();
        for (File file : getAnimationFiles(prmsAnimationDirectoryURL)) {
            PRMSAnimationShapefileDataStore dataStore = new PRMSAnimationShapefileDataStore(namespaceURI, file.toURI().toURL(), shapefileURL, shapefileNHRUAttributeName);
            dataStore.getSchema(); // prime schemas
            prmsDataStoreMap.put(dataStore.getTypeNames()[0], dataStore);
        }
        
        PRMSAnimationShapefileDataStore prmsDataStore = prmsDataStoreMap.firstEntry().getValue();
        bounds = prmsDataStore.getBounds(Query.ALL);
    }
    
    private List<File> getAnimationFiles(URL url) {
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
            if (child.getName().endsWith("animation.nhru")) {
                animationFiles.add(child);
            }
        }
        return animationFiles;
    }    

    @Override
    public String[] getTypeNames() throws IOException {
        return prmsDataStoreMap.keySet().toArray(new String[0]);
    }

    @Override
    public SimpleFeatureType getSchema(String typeName) throws IOException {
        return prmsDataStoreMap.get(typeName).getSchema();
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName) throws IOException {
        return prmsDataStoreMap.get(typeName).getFeatureReader();
    }

    @Override
    public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction) throws IOException {
        return prmsDataStoreMap.get(query.getTypeName()).getFeatureReader(query, transaction);
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(String typeName, Query query) throws IOException {
        return prmsDataStoreMap.get(typeName).getFeatureReader(typeName, query);
    }

    @Override
    public void dispose() {
        for (PRMSAnimationShapefileDataStore dataStore : prmsDataStoreMap.values()) {
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
