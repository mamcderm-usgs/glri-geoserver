package gov.usgs.cida.prms;

import java.io.File;
import java.net.URL;
import org.junit.Test;

/**
 *
 * @author tkunicki
 */
public class Utilities {

    public final static String DEFALT_ANIMATION_RESOURCE = "pcm.a1fi.1980-2099.annual.animation.nhru";
    public final static String DEFALT_SHAPEFILE_RESOURCE = "pcm.a1fi.1980-2099.annual.animation.nhru";
    public final static String DEFALT_SHAPEFILE_NHRU_ATTRIBUTE = "HRU_ID";

    public static URL findURLForResource(String resource) {
        return Utilities.class.getClassLoader().getResource(resource);
    }
    
    public static URL getDefaultAnimationResource() {
        return findURLForResource(DEFALT_ANIMATION_RESOURCE);
    }
    
    public static URL getDefaultShapefileResource() {
        return findURLForResource(DEFALT_SHAPEFILE_RESOURCE);
    }
    
    public static String getDefaultShapefileNHRUAttribute() {
        return DEFALT_SHAPEFILE_NHRU_ATTRIBUTE;
    }
}
