package edu.umich.its.lti.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;
import net.oauth.signature.OAuthSignatureMethod;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.http.HttpRequest;

/**
 * This handles security for requests, signing outgoing request, and ensuring
 * that incoming requests are proper.
 *
 * @author ranaseef
 *
 */
public class RequestSignatureUtils {
	// Constants ----------------------------------------------------

	private static final Log M_log =
			LogFactory.getLog(RequestSignatureUtils.class);


	// Static public methods ----------------------------------------

	/**
	 * This signs the parameters, and returns the updated map to the caller.
	 * This is a copy of code
	 * org.imsglobal.basiclti.BasicLTIUtil.signParameters().
	 *
	 * Note, the given map is not updated; the caller needs to use the returned
	 * map.
	 *
	 * @param parameters Map<String, String> of the parameter keys & values
	 * @param url        URL of the request to be made
	 * @param method     Request method (POST, GET)
	 * @param oauth_consumer_key Consumer's key
	 * @param oauth_consumer_secret Consumer's secret
	 * @return Map with oauth_signature to validate this request
	 */
	public static Map<String, String> signParameters(
			Map<String, String> parameters,
			String url,
			String method,
			OauthCredentials oc)
	{
		Map<String, String> result = null;
		OAuthMessage oam = new OAuthMessage(method, url, parameters.entrySet());
		OAuthConsumer cons = new OAuthConsumer(
				"about:blank",
				oc.getKey(),
				oc.getSecret(),
				null);
		OAuthAccessor acc = new OAuthAccessor(cons);
		try {
			oam.addRequiredParameters(acc);
			List<Map.Entry<String, String>> params = oam.getParameters();
			result = new HashMap<String, String>();
			// Convert to Map<String, String>
			for (final Map.Entry<String, String> entry : params) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		} catch (Exception err) {
			M_log.error(
					"BasicLTIUtil.signProperties OAuth Exception ",
					err);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Verifies the incoming request is valid request from client with the given
	 * key, and the request contains the matching signature.  This also ensures
	 * the nonce has not been used in another request.
	 *
	 * This is copy of code org.sakaiproject.blti.ServiceServlet.doPostForm()
	 *
	 * @param request HttpServletRequest made by the client
	 * @param oauth_consumer_key The client's key, included in the request
	 * @param oauth_consumer_secret THe client's secret, known locally and not
	 * @param URL the LTI launch url
	 * included in the request.
	 * @return true if the request is valid
	 */
	public static boolean verifySignature(
			HttpServletRequest request,
			String oauth_consumer_key,
			String oauth_consumer_secret,
			String URL)
	{
		if (URL == null||URL.isEmpty()) {
			URL=request.getRequestURL().toString();

		}

		boolean result = false;
		OAuthMessage oam = OAuthServlet.getMessage(request, URL);
		OAuthValidator oav = new SimpleOAuthValidator();
		OAuthConsumer cons = new OAuthConsumer(
				"about:blank",
				oauth_consumer_key,
				oauth_consumer_secret,
				null);
		OAuthAccessor acc = new OAuthAccessor(cons);
		try {
			oav.validateMessage(oam, acc);
			// If no error thrown, the message is valid
			result = true;
		} catch (Exception err) {
			String errMsg = "Failed to validate message";
			M_log.error(errMsg, err);
			try {
				String base_string = OAuthSignatureMethod.getBaseString(oam);
				if (base_string != null) {
					M_log.warn(base_string);
				}
			} catch (Exception err2) {
				M_log.error(
						"Failed get get BaseString; this is for debugging - "
						+ "look at prior error \""
						+ errMsg
						+ "\"",
						err);
			}
		}
		return result;
	}

	/**
	 * returns true if message is NOT valid.
	 * @param request
	 * @param oauth_consumer_key
	 * @return
	 */
	static public Boolean validateMessage(HttpServletRequest request,
			OauthCredentials oc) {

		OAuthMessage oam = OAuthServlet.getMessage(request, null);
		OAuthValidator oav = new SimpleOAuthValidator();
		OAuthConsumer cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed",
				oc.getKey(),oc.getSecret(),null);

		OAuthAccessor acc = new OAuthAccessor(cons);

		String base_string = null;

		Boolean errorReturn = false;

		try {
			base_string = OAuthSignatureMethod.getBaseString(oam);
		} catch (Exception e) {
			base_string = null;
		}

		try {
			oav.validateMessage(oam,acc);
		} catch(OAuthProblemException oape){
			M_log.error("OAuthProblemException during validation: ",oape);
			if (M_log.isDebugEnabled()) {
				Map<String, Object> parameters = oape.getParameters();
				Set<String> keys = parameters.keySet();
				for(String k: keys) {
							M_log.debug("key: ["+k+"] value: ["+parameters.get(k)+"]");
				}
			}
		} catch(Exception e) {
		    M_log.error("LTI validation failed: ",e);
		    if ( base_string != null ) {
			//			M_log.debug("base string: ",base_string);
		    }
			errorReturn = true;
		}
		return errorReturn;
	}

}
