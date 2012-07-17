package gov.usgs.cida.geotools.datastore;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.util.KVP;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationShapefileDataStoreFactory implements DataStoreFactorySpi {

    /**
     * Public "no arguments" constructor called by Factory Service Provider
     * (SPI) based on entry in
     * META-INF/services/org.geotools.data.DataStoreFactorySpi
     */
    public PRMSAnimationShapefileDataStoreFactory() { }
    
    /**
     * No implementation hints are provided at this time.
     */
    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }
    
    @Override
    public String getDisplayName() {
        return "PRMS Animation Shapefile Joining Data Store";
    }

    @Override
    public String getDescription() {
        return "Allows joining of PRMS animation time-series with a Shapefile";
    }
    
    /**
     * Test to see if this datastore is available, if it has all the appropriate
     * libraries to construct a datastore. This datastore just returns true for
     * now. This method is used for interactive applications, so as to not
     * advertise data store capabilities they don't actually have.
     * 
     * @return <tt>true</tt> if and only if this factory is available to create
     *         DataStores.
     * @task <code>true</code> property datastore is always available
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    public static final Param NAMESPACE = // Name must be preserved (?)
        new Param("namespace", URI.class,
            "namespace uri", false, null,
            new KVP(Param.LEVEL,"advanced"));
    public static final Param PRMS_ANIMATION =
            new Param("prms_animation_file", URL.class, "PRMS Animation File", true, null,
                new KVP(Param.EXT, "animation.nhru"));
    public static final Param SHAPEFILE =
            new Param("shapefile", URL.class, "Shapefile", true, null,
                new KVP(Param.EXT, "shp"));
    public static final Param SHAPEFILE_NHRU =
            new Param("shapefile_nhru", String.class, "Shapefile NHRU Identifying Attribute", true);

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {
            NAMESPACE,
            PRMS_ANIMATION,
            SHAPEFILE,
            SHAPEFILE_NHRU,
        };
    }
    
        @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            URL prmsAnimationAsURL = (URL)PRMS_ANIMATION.lookUp(params);
            if (prmsAnimationAsURL != null) {
                File prmsAnimationAsFile = null;
                try {
                    prmsAnimationAsFile = new File(prmsAnimationAsURL.toURI());
                } catch (Exception ex) {
                    return false;
                }
                if (!prmsAnimationAsFile.canRead()) {
                    return false;
                }
                // TODO: make sure we can open as PRMS Animation file
            } else {
                return false;
            }
            URL shapefileAsURL = (URL)SHAPEFILE.lookUp(params);
            if (shapefileAsURL != null) {
                File shapeFileAsFile = null;
                try {
                    shapeFileAsFile = new File(shapefileAsURL.toURI());
                } catch (Exception ex) {
                    return false;
                }
                if (!shapeFileAsFile.canRead()) {
                    return false;
                }
                // TODO:  Make sure we can open as shapefile
            } else {
                return false;
            }
            Object shapefileNHRUAsObject = (String) SHAPEFILE_NHRU.lookUp(params);
            if (shapefileNHRUAsObject instanceof String) {
                // TODO: check that
                // 1) attribute exists in shapefile
                // 2) is Integer or can exract all values as Integers (maybe stored as String?)
                // 3) matches values stored in PRMS Animation nhru colum
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    
    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return new PRMSAnimationShapefileDataStore(
                (URI) NAMESPACE.lookUp(params),
                (URL) PRMS_ANIMATION.lookUp(params),
                (URL) SHAPEFILE.lookUp(params),
                (String) SHAPEFILE_NHRU.lookUp(params));
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        throw new UnsupportedOperationException("DataStore is Read-Only");
    }
}
