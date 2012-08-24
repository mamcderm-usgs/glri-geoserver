/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.usgs.cida.geotools.datastore;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;

/**
 *
 * @author tkunicki
 */
public class DSGUtilTest {

    public DSGUtilTest() {
    }

    @Test
    public void testExtractStationTimeSeriesFeatureCollection() throws IOException {
        FeatureDataset fd = null;
        try {
            fd = openFeatureDataSet();
            assertNotNull(fd);
            StationTimeSeriesFeatureCollection stsfc = NetCDFUtil.extractStationTimeSeriesFeatureCollection(fd);
            assertNotNull(stsfc);
            assertTrue(true);
        } finally {
            if (fd != null) {
                fd.close();
            }
        }
    }

    @Test
    public void testGetObservationTimeVariable() throws IOException {
        FeatureDataset fd = null;
        try {
            fd = openFeatureDataSet();
            VariableSimpleIF variable = NetCDFUtil.getObservationTimeVariable(fd);
            assertNotNull(variable);
            assertEquals("time", variable.getShortName());
            assertEquals("record.time", variable.getFullName());
        } finally {
            if (fd != null) {
                fd.close();
            }
        }
    }
    
    @Test
    public void testGetStationIdVariable() throws IOException {
        FeatureDataset fd = null;
        try {
            fd = openFeatureDataSet();
            VariableSimpleIF variable = NetCDFUtil.getStationIdVariable(fd);
            assertNotNull(variable);
            assertEquals("station_id", variable.getShortName());
        } finally {
            if (fd != null) {
                fd.close();
            }
        }
    }
    
    private FeatureDataset openFeatureDataSet() throws IOException {
        return openFeatureDataset("target/test-classes/pcm_b1_tmax-days_above_threshold,100.0,dsg.nc");
    }
    
    private FeatureDataset openFeatureDataset(String path) throws IOException {
        return FeatureDatasetFactoryManager.open(
                    FeatureType.ANY,
                    path,
                    null,
                    new Formatter(System.err));
    }
}
