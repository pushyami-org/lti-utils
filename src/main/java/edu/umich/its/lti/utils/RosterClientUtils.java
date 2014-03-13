package edu.umich.its.lti.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.utils.OauthCredentials;
/**
 *
 * @author ranaseef, dlhaines
 *
 */

public class RosterClientUtils {
	private static Log M_log = LogFactory.getLog(RosterClientUtils.class);
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
			result = extractPersonContactEmailPrimaryFromRosterFull(httpEntity);
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
			return null;
		}

		List<String> result = new ArrayList<String>();
		Document doc = documentInfo(httpEntity);
		NodeList nodes = doc
				.getElementsByTagName("person_contact_email_primary");
		for (int nodeIdx = 0; nodeIdx < nodes.getLength(); nodeIdx++) {
			Node node = nodes.item(nodeIdx);
			result.add(node.getTextContent());
		}
		return result;
	}	
	
	/*
	 * Parse the xml obtained from the roster service to get a list of all the item in the <member>.
	 */
	protected static HashMap<String, HashMap<String, String>> extractPersonContactEmailPrimaryFromRosterFull(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {
		if (httpEntity == null ) {
			return null;
		}
		HashMap<String, HashMap<String, String>> newRosterBig = new HashMap<String, HashMap<String, String>>();
		Document doc = documentInfo(httpEntity);
		String[] rosterDetailInfo= {"person_contact_email_primary","role","lis_result_sourcedid","person_name_family",
				"person_name_full","person_name_given","person_sourcedid","user_id"};
		NodeList memberList = doc.getElementsByTagName("member");
		for(int i=0;i<memberList.getLength();i++) {
			HashMap<String, String> nestedMap = new HashMap<String, String>();
			Node nNode = memberList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				 for (String tagName : rosterDetailInfo) {
					 nestedMap.put(tagName, eElement.getElementsByTagName(tagName).item(0).getTextContent());
				}
				 			}
			newRosterBig.put(nestedMap.get("person_contact_email_primary"), nestedMap);
		}
		return newRosterBig;
	}


	private static Document documentInfo(HttpEntity httpEntity)
			throws ParserConfigurationException, SAXException, IOException {
			// See: http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
		DocumentBuilderFactory dbFactory =
				DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
	    //String xml = EntityUtils.toString(httpEntity);
       // System.out.println("See the XML: ----"+ xml);
		Document doc = docBuilder.parse(httpEntity.getContent());
		//optional, but recommended
		//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		return doc;
	}
	
	

	/* 
	 * Make call to lti roster service url and capture the response.
	 */

	protected static HttpEntity getRosterHttpEntity(
			TcSessionData tcSessionData, String sourceUrl)
					throws UnsupportedEncodingException, IOException,
					ClientProtocolException {
		HttpPost httpPost = new HttpPost(sourceUrl);

		// TODO: can this be generalized to not explicitly depend on the roster parameters? 
		Map<String, String> ltiParams =
				getLtiRosterParameters(tcSessionData, sourceUrl);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
			addParameter(nvps, parameter.getKey(), parameter.getValue());
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		// Wrapping the client to trust ANY certificate - Dangerous!
		HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());
		HttpResponse httpResponse = client.execute(httpPost);
		HttpEntity httpEntity = httpResponse.getEntity();
		return httpEntity;
	}



	// Static private methods ---------------------------------------

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
	 * site, so that roster may be retrieved.
	 * @param sourceUrl Client server's URL for requesting rosters.
	 * @return
	 */
	static private Map<String, String> getLtiRosterParameters(
			TcSessionData tcSessionData,
			String sourceUrl)
			{
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
