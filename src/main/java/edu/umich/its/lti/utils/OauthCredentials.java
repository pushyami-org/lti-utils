package edu.umich.its.lti.utils;

// data object hold the oauth credentials for a specific key.

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OauthCredentials {
	private String key;
	private String secret;
	private String url;
}
