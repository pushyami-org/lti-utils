package edu.umich.its.lti.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.StatusLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import edu.umich.its.lti.TcSessionData;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umich.its.lti.TcSessionData;
//import edu.umich.ctools.qualtricslti.Setting;

/*
 * Static methods to allow setting / getting the Settings string from the lti setting service for
 * a particular tool installation.
 */

public class SettingsClientUtils {

	private static Log M_log = LogFactory.getLog(SettingsClientUtils.class);


	/*
	 * Get the settings string from the setting service for this tools instance.  Will default to
	 * returning string representing empty javascript object.  TODO: The default string should be passed in.
	 */

	// get settings string as a single string, not an array.
	static public String getSettingString(TcSessionData tcSessionData)
			throws ServletException, IOException
			{
		List<String> resultStringList = null;
		String resultString = null;
		String sourceUrl = tcSessionData.getSettingUrl();


		// get from setting service if it exists.
		if (sourceUrl != null) {
			try {
				// Make post to get resource

			    M_log.debug("get setting from: ["+sourceUrl+"]");

				HttpPost httpPost = new HttpPost(sourceUrl);
				Map<String, String> ltiParams =
						loadSettingFillParametersAndSignRequest(tcSessionData);
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
					addParameter(nvps, parameter.getKey(), parameter.getValue());
				}
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				// TODO: unacceptable to trust all certificates
				// Wrapping the client to trust ANY certificate - Dangerous!
				M_log.error("current trusts ANY certificate");
				HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());
				HttpResponse httpResponse = client.execute(httpPost);

				StatusLine status = httpResponse.getStatusLine();
				HttpEntity httpEntity = httpResponse.getEntity();

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
		}


		return resultString;
	}


	/*
	 * Setting string is returned with an xml wapper.  Pull it out.
	 */
	private static List<String> parseSettingXml(InputStream stream)
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
	static private void printNode(Node rootNode, String spacer) {
	    M_log.debug(spacer + rootNode.getNodeName() + " -> " + rootNode.getNodeValue());
		NodeList nl = rootNode.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++)
			printNode(nl.item(i), spacer + "   ");
	}


	/*
	 * Send this string as the settinng value for this tool instance.
	 */

	static public Boolean setSetting(TcSessionData tcSessionData,String setting)
			throws ServletException, IOException
			{
		Boolean success = true;

		M_log.debug("setSetting string: ["+setting+"]");
		try {
			// Make post to get resource
			String sourceUrl = tcSessionData.getSettingUrl();
			M_log.debug("save setting to ["+sourceUrl+"]");
			HttpPost httpPost = new HttpPost(sourceUrl);
			Map<String, String> ltiParams =
					saveSettingFillParametersAndSignRequest(tcSessionData,setting);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
				addParameter(nvps, parameter.getKey(), parameter.getValue());
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			// Wrapping the client to trust ANY certificate - Dangerous!
			// TODO: fix certificate usage
			HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());
			HttpResponse httpResponse = client.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();

			StatusLine status = httpResponse.getStatusLine();
			M_log.debug("setSetting: httpResponse.getStatusLine(): "+ status);
			M_log.debug("setSetting: statusCode: "+ status.getStatusCode());

		} catch (Exception err) {
			success=false;
			err.printStackTrace();
		}
		return success;
	}


	static private void addParameter(
			List<NameValuePair> nvps,
			String name,
			String value)
	{
		nvps.add(new BasicNameValuePair(name, value));
	}

	/**
	 * Creates map of the request's parameters, including a signature the client
	 * server will verify matches with the request.
	 *
	 * @param request Incoming request containing some of the ID of the client's
	 * site, so that the settings may be retrieved.
	 * @param sourceUrl Client server's URL for requesting settings.
	 * @return
	 */


	// specialize request for saving the setting value
	static private Map<String, String> saveSettingFillParametersAndSignRequest(
			TcSessionData tcSessionData,String setting)
			{
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
	static private Map<String, String> loadSettingFillParametersAndSignRequest(
			TcSessionData tcSessionData)
			{
		Map<String, String> result = new HashMap<String, String>();
		result.put("lti_message_type", "basic-lti-loadsetting");

		return createSignedResult(tcSessionData, result);
			}

	// Common code for building setting request.
	static private Map<String, String> createSignedResult(
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
