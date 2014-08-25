package edu.umich.its.lti.utils;

	/*
	 * Handle communication with the LTI tool client for retrieving and storing the setting string value.
	 * All methods are static.
	 */

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.http.StatusLine;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import edu.umich.its.lti.TcSessionData;

import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SettingsClientUtils {

	private static Log M_log = LogFactory.getLog(SettingsClientUtils.class);

	/*
	 * Get the settings string from the setting service for this tools instance.  Will default to
	 * returning string representing empty javascript object.  TODO: The default string should be passed in.
	 */

	// get settings string as a single string, not an array as the setting service natively returns.
	static public String getSettingString(TcSessionData tcSessionData)
			throws ServletException, IOException
			{
		List<String> resultStringList = null;
		String resultString = null;
		String sourceUrl = tcSessionData.getSettingUrl();
	    M_log.debug("get setting from: ["+sourceUrl+"]");

		// get from setting service if it exists.
		if (sourceUrl != null) {
			try {
				// Make post to get resource

				Map<String, String> ltiParams =
						loadSettingFillParametersAndSignRequest(tcSessionData);

				HttpResponse httpResponse = postSettingRequestAndGetHttpResponse(
						sourceUrl, ltiParams);

				StatusLine status = httpResponse.getStatusLine();
				HttpEntity httpEntity = httpResponse.getEntity();

				if (M_log.isDebugEnabled()) {
					String entityAsString = httpEntity.getContent().toString();
					M_log.debug("setting service entity as string: "+entityAsString);
				}

				if (httpEntity != null) {
					resultStringList = parseSettingXml(httpEntity.getContent());
				}

			} catch (Exception err) {
				M_log.error("error getting or parsing setting value",err);
			}
		}
		else {
			// TODO: pass in the default value or at least allow passing in the default value.
			resultString = "{}";
			if (tcSessionData != null && tcSessionData.getSetting() != null) {
				resultString = tcSessionData.getSetting().toString();
			}
		}

		if (resultStringList != null && resultStringList.size() > 0) {

			resultString = resultStringList.get(0);
			resultString = decodeSettingString(resultString);
		}

		M_log.debug("read setting string: "+resultString);
		return resultString;
	}


	/*
	 * Setting string is returned with an xml wapper.  Pull out the string.
	 */
	protected static List<String> parseSettingXml(InputStream stream)
			throws ParserConfigurationException,
			SAXException, IOException {

		List<String> result = new ArrayList<String>();
		// See: http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
		DocumentBuilderFactory dbFactory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(stream);

		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		NodeList nodes = doc.getElementsByTagName("setting");
		for (int nodeIdx = 0; nodeIdx < nodes.getLength(); nodeIdx++) {
			Node node = nodes.item(nodeIdx);
			result.add(node.getTextContent().trim());
		}
		return result;
	}

	// Useful for debugging
	// Invoke like:	printNode(doc,"..");
	@SuppressWarnings("unused")
	static protected void printNode(Node rootNode, String spacer) {
	    M_log.debug(spacer + rootNode.getNodeName() + " -> " + rootNode.getNodeValue());
		NodeList nl = rootNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			printNode(nl.item(i), spacer + "   ");
	}

	/*
	 * Send this string as the setting value for this tool instance.
	 */

	static public Boolean setSetting(TcSessionData tcSessionData,String setting)
			throws ServletException, IOException
			{
		Boolean success = true;

		M_log.debug("write setting string: "+setting);
		String encodedSetting = encodeSettingString(setting);
		M_log.debug("setSetting encoded string: ["+encodedSetting+"]");

		try {
			// Make post to get resource

			String sourceUrl = tcSessionData.getSettingUrl();
			M_log.debug("save setting to ["+sourceUrl+"]");

			Map<String, String> ltiParams =
					saveSettingFillParametersAndSignRequest(tcSessionData,encodedSetting);

			HttpResponse httpResponse = postSettingRequestAndGetHttpResponse(
					sourceUrl, ltiParams);
			StatusLine status = httpResponse.getStatusLine();

			M_log.debug("setSetting: httpResponse.getStatusLine(): "+ status);
			M_log.debug("setSetting: statusCode: "+ status.getStatusCode());

		} catch (Exception err) {
			success=false;
			M_log.error("Exception writing settings string",err);
		}
		return success;
	}


	/*
	 * Post request to setting service and get response.  Use for both
	 * getSettings and setSettings requests.
	 */

	public static HttpResponse postSettingRequestAndGetHttpResponse(
			String sourceUrl, Map<String, String> ltiParams)
			throws UnsupportedEncodingException, IOException,
			ClientProtocolException {

		HttpPost httpPost = new HttpPost(sourceUrl);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
			addParameter(nvps, parameter.getKey(), parameter.getValue());
		}

		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());

		HttpResponse httpResponse = client.execute(httpPost);
		return httpResponse;
	}


	// Do any pre/post processing for setting string when sending / getting from setting service.
	// The encoding result should be the exact string that will be sent.

	/**
	 * Take a regular setting string and encode into Base64.
	 */
	public static String encodeSettingString(String setting) {
		// Base64 encode the setting string to avoid some encoding issues when
		// extracting the string from the XML.
		// Base64 works in bytes so will need to translate between bytes and strings.
		String encodedSetting = Base64.encodeBase64String(setting.getBytes());
		M_log.debug("base64 setting string: "+encodedSetting);

		return encodedSetting;
	}

	/**
	 * Take the string from the setting service and undo the encoding.
	 */
	public static String decodeSettingString(String resultString) {
		// The setting string will be stored in base64 from now on.  This check will allow for
		// reading pre-existing stored settings that weren't stored in base64.
		if (Base64.isBase64(resultString)) {
			byte[] decoded = Base64.decodeBase64(resultString);
			resultString = new String(decoded);
		}
		return resultString;
	}


	static protected void addParameter(
			List<NameValuePair> nvps,
			String name,
			String value)
	{
		nvps.add(new BasicNameValuePair(name, value));
	}

	/**
	 * Creates map of the request's parameters, including a signature the client
	 * server will verify matches with the request.
	 */

	// specialize request for saving the setting value
	static protected Map<String, String> saveSettingFillParametersAndSignRequest(
			TcSessionData tcSessionData,String setting) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("lti_message_type", "basic-lti-savesetting");
		// no string specified means that should save empty string.
		if (setting == null) {
			setting = "";
		}
		result.put("setting",setting);
		return createSignedResult(tcSessionData, result);
	}

	// specialize request for loading (getting/reading) the setting value
	static protected Map<String, String> loadSettingFillParametersAndSignRequest(TcSessionData tcSessionData) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("lti_message_type", "basic-lti-loadsetting");

		return createSignedResult(tcSessionData, result);
	}

	// Common code for building the setting request.
	static protected Map<String, String> createSignedResult(
			TcSessionData tcSessionData, Map<String, String> result) {
		result.put("id", tcSessionData.getSettingId());
		result.put("lti_version", "LTI-1p0");
		result.put("oauth_callback", "about:blank");
		result = RequestSignatureUtils.signParameters(
				result,
				tcSessionData.getSettingUrl(),
				"POST",
				tcSessionData.getOauthCredentials());
		return result;
	}

}
