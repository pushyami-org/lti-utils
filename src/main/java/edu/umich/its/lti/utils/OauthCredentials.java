package edu.umich.its.lti.utils;

/* Data object to hold the oauth credentials for LTI providers.
 * See OauthCredentialsFactory for more information.
*/

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OauthCredentials {
	private String key;
	private String secret;
	private String url;
}
