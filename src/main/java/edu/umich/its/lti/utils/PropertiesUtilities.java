package edu.umich.its.lti.utils;

// Static utility methods to:
// - get a property value from a properties object while allowing property overrides
//   from system properties and allowing default values.
// - get a Properties object from an URL.  The URL is resolved using java.net URL object
//   which provides the following protocols out of the box: http, https, ftp, file, and jar.

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PropertiesUtilities {

	private static Log M_log = LogFactory.getLog(PropertiesUtilities.class);

	/* Get a property from the supplied properties object.  If the value is set in
	 * system properties that value will override the properties object value.  If
	 * neither is set the default value supplied will be used.
	 */
	public static String getStringProperty(String defaultString, String propertyName, Properties prop) {

		String setValue = null;
		M_log.debug("setprop: defaultString: ["+defaultString+"] propertyName: ["+propertyName+"] local properties: ["+prop+"]");

		/* Best not to print these out unless absolutely necessary
		M_log.debug("system properties: ["+System.getProperties()+"]");
		*/

		// check system property
		setValue = System.getProperty(propertyName);
		M_log.debug("property after system properties check: ["+setValue+"]");

		// if not set then check properties file
		if (setValue == null) {
			setValue = prop.getProperty(propertyName);
		}
		M_log.debug("property after properties file check: ["+setValue+"]");

		// if not set then use default value.
		if (setValue == null) {
			setValue = defaultString;
		}

		M_log.debug("final properties value: ["+setValue+"]");
		M_log.info("found property value: "+propertyName+": "+setValue);
		return setValue;
	}

	/*
	 Try to get properties object from a remote properties file.  If this can't be done
	 it will return null.  An empty properties object means that the files exists
	 and can be read but doesn't have anything specified within it.
	 Null is reasonable to return for a missing file since the properties might be gotten
	 from another source.
 	 */

	public static Properties getPropertiesObjectFromURL(String propertiesFileURL) {
		Properties props = new Properties();
		InputStream in = null;

		in = getInputStreamFromURL(propertiesFileURL);

		if (in == null) {
			return null;
		}

		//  Got a source of properties.  Try to read them.
		try {
			props.load(in);
		} catch (IOException e) {
			M_log.error("IO exception loading propertiesFileURL: "+propertiesFileURL,e);
			return null;
		}

		return props;
	}

	// just establish the connection to the URL file.
	static InputStream getInputStreamFromURL(String fileURL) {

		InputStream in = null;
		URL url = null;

		// check if some url string was supplied.
		if (fileURL == null || fileURL.length() == 0) {
			M_log.debug("null or zero length fileURL supplied");
			return null;
		}

		// open a connection to the file specified by that url.
		try {
			url = new URL(fileURL);
		} catch (MalformedURLException e) {
			M_log.error("file URL is malformed: "+fileURL,e);
			return null;
		}

		// try to get access to that file.
		try {
			in = url.openStream();
		} catch (IOException e) {
			M_log.error("IO exception opening fileURL: "+fileURL,e);
			return null;
		}

		return in;
	}


}
