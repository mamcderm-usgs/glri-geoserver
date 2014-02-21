package gov.usgs.cida.prms;

import java.io.File;
import java.net.URL;
import org.junit.*;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationFileMetaDataTest {
    
    @Test
    public void testOldAnimationFile() throws Exception {
        URL url = getClass().getClassLoader().getResource("pcm.a1fi.1980-2099.annual.animation.nhru");
        File oldMetaData = new File(url.getPath() + ".xml");
        if (oldMetaData.exists()) {
            oldMetaData.delete();
        }
       PRMSAnimationFileMetaData result = PRMSAnimationFileMetaData.getMetaData(url);
       System.out.println(result);
    }
    
    @Test
    public void testNewAnimationFile() throws Exception {
        URL url = getClass().getClassLoader().getResource("cccma_post-processed.nhru");
        File oldMetaData = new File(url.getPath() + ".xml");
        if (oldMetaData.exists()) {
            oldMetaData.delete();
        }
       PRMSAnimationFileMetaData result = PRMSAnimationFileMetaData.getMetaData(url);
       System.out.println(result);
    }

}