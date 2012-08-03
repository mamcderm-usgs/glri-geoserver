package gov.usgs.cida.prms;

import com.thoughtworks.xstream.XStream;

/**
 *
 * @author tkunicki
 */
public class XStreamUtility {

	public static void simpleAlias(XStream xstream, Class clazz) {
        String simpleName = clazz.getSimpleName();
        String simpleNameCamelCase = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
		xstream.alias(simpleNameCamelCase, clazz);
	}
}
