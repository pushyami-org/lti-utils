package edu.umich.its.lti.utils;

// Data class to hold credentials for oauth messages.
// Stores in properties structure with keys of <consumerKey>.<type> -> value
// e.g. lmsng.school.edu.secret=ImColderTHANuARE
//      lmsmg.school.edu.url=https://localesthostess:8080/socoolithurts
// The key and secret are required for this to be useful.  The url property is optional.

// The factory and the getOauthCredentials will always return non-null.
// Requests for properties that aren't set will return a null.

import java.util.Properties;
import edu.umich.its.lti.utils.OauthCredentials;

// These imports are here to make logging easy to add when there is something
// worth logging and it is safe to log it.

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class OauthCredentialsFactory {
//	private static Log M_log = LogFactory.getLog(OauthCredentialsFactory.class);

	private Properties props = null;

	// Create a properties object for reference.  This is a copy of the properties
	// object passed in so that it is isolated from any outside changes.

	public OauthCredentialsFactory(Properties props) {
		this.props = new Properties();
		if (props != null) {
			this.props.putAll(props);
		}
	}

	// return a single object encapsulating the security information tied to
	// oauth verification.
	public OauthCredentials getOauthCredentials(String consumerKey){
		OauthCredentials oc = new OauthCredentials(consumerKey,
				(String)this.props.get(consumerKey+".secret"),
				(String)this.props.get(consumerKey+".url"));
		return oc;
	}
}