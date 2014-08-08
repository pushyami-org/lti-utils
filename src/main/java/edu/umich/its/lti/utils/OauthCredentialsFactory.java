package edu.umich.its.lti.utils;

/*
 * Manage OauthCredentials.

 * LTI Providers need to be able to work with multiple different consumers with different security credentials.
 * This class is a OauthCredentials factory that returns an OauthCredentials object specific to a particular consumer.

 * The factory object is created with the properties file in the format described below.
 * A specific LTI request will get an OauthCreditals object specific to that tool consumer using the
 * getOauthCredentials(consumerKey) method from the factory object.

 * The properties file format is: <consumerKey>.<type>=<whatever>.
 * The current types recognized are secret and url.
 * The consumerKey is defined implicitly by being the prefix on the property name.
 * e.g. lmsng.school.edu.secret=ImColderTHANuARE
 *      lmsmg.school.edu.url=https://localesthostess:8080/socoolithurts
 * The key and secret are required for this to be useful.  The url property is optional. Other
 * optional properties can be added easily if needed.  E.g. if we need an alternative
 * settings service a consumer specific URL could be added here.

 * The factory and the getOauthCredentials will always return non-null.
 * Requests for properties that aren't set will return a null.
 * Properties that aren't requested are ignored.
 */

import java.util.Properties;
import edu.umich.its.lti.utils.OauthCredentials;

// These imports are here to make logging easy to add when there is something
// worth logging and it is safe to log it.

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class OauthCredentialsFactory {
    //	private static Log M_log = LogFactory.getLog(OauthCredentialsFactory.class);

	private Properties props = null;

	// Create a properties object to store information for multiple tool consumers.
	// We store a copy of the properties object passed in so that it is isolated from any outside changes.

	public OauthCredentialsFactory(Properties props) {
		this.props = new Properties();
		if (props != null) {
			this.props.putAll(props);
		}
	}

	/* When OauthCredentials are required call this method on the factory
	 * to get OauthCredentials that are specific to this consumerKey.
     */
	public OauthCredentials getOauthCredentials(String consumerKey){
		OauthCredentials oc = new OauthCredentials(consumerKey,
				(String)this.props.get(consumerKey+".secret"),
				(String)this.props.get(consumerKey+".url"));
		return oc;
	}
}
