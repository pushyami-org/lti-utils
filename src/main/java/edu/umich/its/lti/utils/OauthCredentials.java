package edu.umich.its.lti.utils;

// class to hold credentials for oauth messages
// dumb defaults for now.

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OauthCredentials {
	private static Log M_log = LogFactory.getLog(OauthCredentials.class);

	private String consumerKey;
	private String secret;

	private HashMap<String,String> secretMap = new HashMap<String,String>();

	// initialize by fixed array for the moment.
	{
		secretMap.put("lmsng.school.edu", "secret");
	}

	public String getConsumerKey() {
		return consumerKey;
	}

	public String getSecret() {
		return secret;
	}

	public OauthCredentials(String consumerKey, String secret) {
		this.consumerKey = consumerKey;
		if (this.consumerKey == null) {
			M_log.error("No consumerKey defined for OauthCredentials.");
	}
		this.secret = secret;
		if (this.secret == null) {
			M_log.error("No secret defined for key: "+consumerKey);
	}
	}

	public OauthCredentials(String consumerKey) {
		this.consumerKey = consumerKey;
		this.secret = secretMap.get(consumerKey);
		if (this.secret == null) {
				M_log.error("No secret defined for key: "+consumerKey);
		}
	}
}