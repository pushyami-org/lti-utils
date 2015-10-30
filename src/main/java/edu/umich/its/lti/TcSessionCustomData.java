package edu.umich.its.lti;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import edu.umich.its.lti.utils.OauthCredentials;

public class TcSessionCustomData extends TcSessionData{
	
	private HashMap<String, Object> customValuesMap;	
	
	public TcSessionCustomData(HttpServletRequest request, OauthCredentials oac, HashMap<String, Object> customValuesMap) {
		super(request, oac);
	}
	
	public HashMap<String, Object> getCustomValuesMap() {
		return customValuesMap;
	}

	public void setCustomValuesMap(HashMap<String, Object> customValuesMap) {
		this.customValuesMap = customValuesMap;
	}

}