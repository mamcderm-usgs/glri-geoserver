package gov.usgs.cida.prms;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tkunicki
 */
public class PRMSAnimationFileUtilityTest {

	public PRMSAnimationFileUtilityTest() {
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

//	@Test
//	public void testQuickSplit() {
//
//		String testLine = Utilities.TEST_LINE;
//
//		String[] fakeNames = testLine.split("\\s+");
//		int tokenCount = fakeNames.length;
//
//		char[] testLineAsCharArray = testLine.toCharArray();
//		List<RecordEntryDescriptor> list = MMSAnimationFileUtility.generateRecordEntryDescriptors(testLine, fakeNames);
//		RecordEntryDescriptor[] array = list.toArray(new RecordEntryDescriptor[0]);
//		System.out.print("|" + MMSAnimationFileUtility.quickExtractRecordAsString(testLineAsCharArray, array[0]));
//		System.out.print("|" + MMSAnimationFileUtility.quickExtractRecordAsInt(testLineAsCharArray, array[1]));
//		for(int i = 2; i < tokenCount; ++i) {
//			System.out.print("|" + MMSAnimationFileUtility.quickExtractRecordAsFloat(testLineAsCharArray, array[i]));
//		}
//		System.out.println();
//    }

//	@Test
//	public void testQuickSplitSpeed() {
//		int iterations = 1000000;
//
//		String testLine = Utilities.TEST_LINE;
//		String[] fakeNames = testLine.split("\\s+");
//		int tokenCount = fakeNames.length;
//
//		{
//			long start = System.currentTimeMillis();
//			for (int index = 0; index < iterations; ++index) {
//				String[] split = testLine.split("\\s+");
//				Integer.parseInt(split[1]);
//				for(int i = 2; i < tokenCount; ++i) {
//					Float.parseFloat(split[i]);
//				}
//			}
//			System.out.println("String.split() completed " + iterations + " in " + (System.currentTimeMillis() - start) + "ms.");
//		}
//		{
//			long start = System.currentTimeMillis();
//			Pattern splitPattern = Pattern.compile("\\s+");
//			for (int index = 0; index < iterations; ++index) {
//				String[] split = splitPattern.split(testLine);
//				Integer.parseInt(split[1]);
//				for(int i = 2; i < tokenCount; ++i) {
//					Float.parseFloat(split[i]);
//				}
//			}
//			System.out.println("Pattern.split() completed " + iterations + " in " + (System.currentTimeMillis() - start) + "ms.");
//		}
//		{
//			char[] testLineAsCharArray = testLine.toCharArray();
//			List<RecordEntryDescriptor> list = MMSAnimationFileUtility.generateRecordEntryDescriptors(testLine, fakeNames);
//			RecordEntryDescriptor[] array = list.toArray(new RecordEntryDescriptor[0]);
//			long start = System.currentTimeMillis();
//			for (int index = 0; index < iterations; ++index) {
//				MMSAnimationFileUtility.quickExtractRecordAsString(testLineAsCharArray, array[0]);
//				MMSAnimationFileUtility.quickExtractRecordAsInt(testLineAsCharArray, array[1]);
//				for(int i = 2; i < tokenCount; ++i) {
//					MMSAnimationFileUtility.quickExtractRecordAsFloat(testLineAsCharArray, array[i]);
//				}
//			}
//			System.out.println("quickSplit() w/o token count completed " + iterations + " in " + (System.currentTimeMillis() - start) + "ms.");
//		}
//	}

}
