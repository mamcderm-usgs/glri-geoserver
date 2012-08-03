package gov.usgs.cida.prms;

import java.io.File;
import java.net.URL;
import org.junit.*;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationFileMetaDataTest {

    public PRMSAnimationFileMetaDataTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void findAnimationFile() throws Exception {
        URL url = getClass().getClassLoader().getResource("pcm.a1fi.1980-2099.annual.animation.nhru");
        File oldMetaData = new File(url.getPath() + ".xml");
        if (oldMetaData.exists()) {
            oldMetaData.delete();
        }
       PRMSAnimationFileMetaData result = PRMSAnimationFileMetaData.getMetaData(url);
       System.out.println(result);
    }

}