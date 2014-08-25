package edu.umich.its.lti.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umich.its.lti.TcSessionData;

/**
 *
 * Parse the xml obtained from the LTI roster service to get a hash (keyed by email) of user information hashs for each person
 * in the roster.
 * @author ranaseef, dlhaines, pushyami
 *
 * This will get the roster from the LTI tool consumer and can return it in various formats.
 * ANY additional work should start with a refactoring to remove multiple implementations
 * of information extraction.
 *
 * All methods are static.
 */

public class RosterClientUtils {

	// Constants for all of the tags found in the roster xml.

	public static final String USER_ID = "user_id";
	public static final String PERSON_SOURCEDID = "person_sourcedid";
	public static final String PERSON_NAME_GIVEN = "person_name_given";
	public static final String PERSON_NAME_FULL = "person_name_full";
	public static final String PERSON_NAME_FAMILY = "person_name_family";
	public static final String LIS_RESULT_SOURCEDID = "lis_result_sourcedid";
	public static final String ROLE = "role";
	public static final String PERSON_CONTACT_EMAIL_PRIMARY = "person_contact_email_primary";

	private static Log M_log = LogFactory.getLog(RosterClientUtils.class);


	// tag names for the xml data
	static final String[] rosterDetailInfo= {PERSON_CONTACT_EMAIL_PRIMARY,ROLE,LIS_RESULT_SOURCEDID,PERSON_NAME_FAMILY,
			PERSON_NAME_FULL,PERSON_NAME_GIVEN,PERSON_SOURCEDID,USER_ID};

	// Static public methods ----------------------------------------

	/**
	 * Makes direct server-to-server request to get the site's roster in xml format and
	 * returns a list of the users by email.
	 */
	static public List<String> getRoster(TcSessionData tcSessionData)
			throws ServletException, IOException
			{
		List<String> result = null;
		String sourceUrl = tcSessionData.getMembershipsUrl();

		// Make post to get resource
		HttpEntity httpEntity = getRosterHttpEntity(tcSessionData,sourceUrl);
		try {
			result = extractPersonContactEmailPrimaryFromRoster(httpEntity);
		} catch (ParserConfigurationException e) {
			M_log.error("Parser configuration error occurred when parsing the xml response which contains roster details",e);
		} catch (SAXException e) {
			M_log.error("SAXException error occurred when parsing the xml response which contains roster details",e);
		}

		return result;
		}


	/**
	 * Makes direct server-to-server request to get the site's roster in xml format and
	 * returns a list of the users index by by email.
	 */
	static public List<String> getRosterWithNames(TcSessionData tcSessionData)
			throws ServletException, IOException
			{
		List<String> result = null;
		String sourceUrl = tcSessionData.getMembershipsUrl();

		// Make post to get resource
		HttpEntity httpEntity = getRosterHttpEntity(tcSessionData,sourceUrl);
		try {
			result = extractPersonEmailAndNamesFromRoster(httpEntity);
		} catch (ParserConfigurationException e) {
			M_log.error("Error occurred when parsing the xml response which contains roster details",e);
		} catch (SAXException e) {
			M_log.error("Error occurred when parsing the xml response which contains roster details",e);
		}

		return result;
		}

	/**
	 * Makes direct server-to-server request to get the site's roster in xml format and
	 * returns a nested hash map that holds all the information in the of the <member> in the xml
	 */
	static public HashMap<String,HashMap<String, String>> getRosterFull(TcSessionData tcSessionData)
			throws ServletException, IOException
			{
		HashMap<String,HashMap<String, String>> result = null;
		String sourceUrl = tcSessionData.getMembershipsUrl();

		// Make post to get resource
		HttpEntity httpEntity = getRosterHttpEntity(tcSessionData,sourceUrl);
		try {
			result = extractRosterInformationFromXML(httpEntity);
		} catch (ParserConfigurationException e) {
			M_log.error("Error occurred when parsing the xml response which contains roster details",e);
		} catch (SAXException e) {
			M_log.error("Error occurred when parsing the xml response which contains roster details",e);
		}

		return result;
			}

	/*
	 * Parse the xml obtained from the roster service to get a list of emails.
	 */
	protected static List<String> extractPersonContactEmailPrimaryFromRoster(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {

		if (httpEntity == null ) {
			M_log.warn("extractPersonContactEmailPrimaryFromRoster got null httpEntity");
			return null;
		}

		List<String> result = new ArrayList<String>();
		Document doc = documentInfo(httpEntity);

		if (doc == null ) {
			M_log.warn("extractPersonContactEmailPrimaryFromRoster got null doc");
			return null;
		}

		NodeList nodes = doc
				.getElementsByTagName(PERSON_CONTACT_EMAIL_PRIMARY);
		for (int nodeIdx = 0; nodeIdx < nodes.getLength(); nodeIdx++) {
			Node node = nodes.item(nodeIdx);
			result.add(node.getTextContent());
		}
		return result;
	}

	/*
	 * Parse the xml obtained from the roster service to get a list of emails and name information.
	 */
	protected static List<String> extractPersonEmailAndNamesFromRoster(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {

		if (httpEntity == null ) {
			M_log.warn("extractPersonEmailAndNamesFromRoster got null httpEntity");
			return null;
		}

		HashMap<String, HashMap<String, String>> roster = extractRosterInformationFromXML(httpEntity);
		List<String> result = buildCSVEmailFirstnameLastnameFromRoster(roster);
		return result;
	}

	// Translate the xml into a hash of hashs.
	protected static HashMap<String, HashMap<String, String>> extractRosterInformationFromXML(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {
		if (httpEntity == null ) {
			return null;
		}
		HashMap<String, HashMap<String, String>> newRosterBig = new HashMap<String, HashMap<String, String>>();
		Document doc = documentInfo(httpEntity);

		NodeList memberList = doc.getElementsByTagName("member");
		for(int i=0;i<memberList.getLength();i++) {
			HashMap<String, String> nestedMap = new HashMap<String, String>();
			Node nNode = memberList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				 for (String tagName : rosterDetailInfo) {
					 // skip any element in list that doesn't have a corresponding entry in the xml.
					 String text = "";
					 if (eElement.getElementsByTagName(tagName).item(0) != null) {
						 text = eElement.getElementsByTagName(tagName).item(0) .getTextContent();
						 nestedMap.put(tagName, text);
					 }
				}
				 			}
			newRosterBig.put(nestedMap.get(PERSON_CONTACT_EMAIL_PRIMARY), nestedMap);
		}
		return newRosterBig;
	}

	// Build lines in csv format that contain the email, family and given names.
	protected static List<String> buildCSVEmailFirstnameLastnameFromRoster(
			HashMap<String, HashMap<String, String>> roster) {
		List<String> result = new ArrayList<String>();
		for(String email : roster.keySet()) {
			String familyName = roster.get(email).get(PERSON_NAME_FAMILY);
			String givenName = roster.get(email).get(PERSON_NAME_GIVEN);
			StringBuilder entry = new StringBuilder();
			entry.append(email).append(",")
			.append(givenName).append(",")
			.append(familyName);
			result.add(entry.toString());
		}
		return result;
	}

	// do setup for parsing xml data.
	protected static Document documentInfo(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {
			// See: http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
		DocumentBuilderFactory dbFactory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(httpEntity.getContent());
		doc.getDocumentElement().normalize();
		return doc;
	}


	/*
	 * Make actual call to lti roster service url and capture the response.
	 */

	protected static HttpEntity getRosterHttpEntity(
			TcSessionData tcSessionData, String sourceUrl)
					throws UnsupportedEncodingException, IOException,
					ClientProtocolException {
		HttpPost httpPost = new HttpPost(sourceUrl);

		Map<String, String> ltiParams =
				getLtiRosterParameters(tcSessionData, sourceUrl);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
			addParameter(nvps, parameter.getKey(), parameter.getValue());
		}

		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		// Wrapping the client to trust ANY certificate - Dangerous!
		HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());

		if (client == null) {
			M_log.warn("getRosterHttpEntity got null client");
			return null;
		}

		HttpResponse httpResponse = client.execute(httpPost);
		HttpEntity httpEntity = httpResponse.getEntity();

		return httpEntity;
	}


	// utility methods to build roster query.
	static protected void addParameter(
			List<NameValuePair> nvps,
			String name,
			String value)
	{
		nvps.add(new BasicNameValuePair(name, value));
	}

	/**
	 * Creates map of the request's parameters, including a signature that the client
	 * server will verify matches with the request.
	 *
	 * The tcSession data and sourceUrl are required to call back to the tool client.
	 */
	// TODO: can this be generalized to not explicitly depend on the roster parameters?
	static protected Map<String, String> getLtiRosterParameters(
			TcSessionData tcSessionData,
			String sourceUrl) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("id", tcSessionData.getMembershipsId());
		result.put("lti_message_type", "basic-lis-readmembershipsforcontext");
		result.put("lti_version", "LTI-1p0");
		result.put("oauth_callback", "about:blank");
		result = RequestSignatureUtils.signParameters(
				result,
				sourceUrl,
				"POST",
				tcSessionData.getOauthCredentials());
		return result;
			}
}
