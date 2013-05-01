package gov.usgs.cida.geotools.datastore;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Web application lifecycle listener.
 *
 * @author tkunicki
 */
public class NetCDFShutdownServletListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
         NetcdfDataset.shutdown();
    }
}
