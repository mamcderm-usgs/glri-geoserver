/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geotools.data.shapefile.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Map;
import org.junit.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author eeverman
 */
public class FieldIndexedDbaseFileReaderTest {
	
	final double COMP_ERR = .000001D;
	
	FieldIndexedDbaseFileReader dbaseReader = null;	//initialized for each use
	
	public FieldIndexedDbaseFileReaderTest() {
	}
	
	/*
	The sample.dbf file looks like this (copied from a sparrow example) :
	IDENTIFIER	|	VALUE
	5660		|	67303.8268
	5661		|	11082.0660
	5659		|	24008.4943
	
	
	*/

	@Before
	public void doSetup() throws Exception {
		URL url = this.getClass().getClassLoader().getResource("sample.dbf");
		File dBaseFile = new File(url.getFile());
		FileChannel dBaseFileChannel = (new FileInputStream(dBaseFile)).getChannel();
		dbaseReader = new FieldIndexedDbaseFileReader(dBaseFileChannel);

		dbaseReader.buildFieldIndex("IDENTIFIER");
	}
	
	@After
	public void doTeardown() throws Exception {
		if (dbaseReader != null) {
			dbaseReader.close();
		}
		
		dbaseReader = null;
	}

	
	@Test
	public void testTheFieldIndexing() throws Exception {

		
		Map<Object, Integer> indexMap = dbaseReader.getFieldIndex();
		
		//Test the indexed ID column to point to the correct row
		assertEquals(1, indexMap.get(5660).intValue());
		assertEquals(2, indexMap.get(5661).intValue());
		assertEquals(3, indexMap.get(5659).intValue());
		
		//Try to access the first row
		dbaseReader.setCurrentRecordByValue(5660);
		dbaseReader.read(); // required when using readField
		assertEquals(67303.8268d, ((Number)dbaseReader.readField(1)).doubleValue(), COMP_ERR);
		
		//Try to access the second row
		dbaseReader.setCurrentRecordByValue(5661);
		dbaseReader.read();
		assertEquals(11082.0660d, ((Number)dbaseReader.readField(1)).doubleValue(), COMP_ERR);
		
		//Try to access the third / last row
		dbaseReader.setCurrentRecordByValue(5659);
		dbaseReader.read();
		assertEquals(24008.4943d, ((Number)dbaseReader.readField(1)).doubleValue(), COMP_ERR);
	}
	
	/**
	 * The Zero index is not allowed
	 * @throws Exception 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void setCurrentRecordByIndexBoundLow() throws Exception {
		dbaseReader.setCurrentRecordByNumber(0);
	}
	
	/**
	 * One beyond the top index value
	 * @throws Exception 
	 */
	@Test(expected=IllegalArgumentException.class)
	public void setCurrentRecordByIndexBoundHigh() throws Exception {
		dbaseReader.setCurrentRecordByNumber(4);
	}
	
	@Test
	public void setCurrentRecordByValueNotFound() throws Exception {
		assertEquals(false, dbaseReader.setCurrentRecordByValue("MadeUp"));
	}
	
}
