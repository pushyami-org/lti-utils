This is README.txt for project lti-utils

Developed by Raymond Naseef (developer) under leadership of John
Johnston, Catherine Crouch, Chris Kretler, and the entire CTools
Development Team, for University of Michigan.  Code in
RequestSignatureUtils is based on code from Sakai Project "basiclti",
as each method's javadoc specifies.


[Purpose]
=========

This project provides utilities for LTI communication between TC (Tool
Consumer) and TP (Tool Provider), along with giving TP information it
can store for communicating with the browser.  The key to all of this
is class TcSessionData: one instance of this will hold consumer data,
and will provide a unique identification key to be used in
communication between TP and the browser.

For example, TC launches LTI by posting to TP with parameters,
including URL the TP can contact to request the TC site's roster.



[Security]
==========

- For TP to make sure operations are correct, it needs to validate
  requests from TC along with requests with the browser.

Verifying signatures and nonce from TC will make sure requests are
valid.  Method RequestSignatureUtils.verifySignature() checks the
signature and that nonce has not been used before.

- The TcSessionData key will ensure incoming requests from the browser
  are for a valid LTI session, and will also make sure the request is
  for the correct LTI.  The most likely case for "bad" requests would
  likely come from the user backing up their browser to a page that
  was interacting with a different LTI session.  Having TP verify the
  browser's request will keep requests from affecting the wrong LTI
  session.

- Client code for requesting roster is written to accept ANY server's
  certificate without verifying it.  This is dangerous, and is not
  recommended for communication across the Internet.  For using this
  to be considered safe, all networks the communication travels over
  need to be ones the owners of TP and TC both trust to block any
  connection by third parties.  The code accepting ANY certificate can
  be found at edu.umich.its.lti.utils.ClientSslWrapper#wrapClient().



[Example Code]
==============

This code shows operations being taken by the Java Servlet for TP,
including handling post from TC, requests made by the browser, and
making request for the roster.  This code comes from my code in
GoogleLtiServlet.java in project "google-drive-lti", and it simplified
to show a minimum a developer would need to write their own TP
servlet.

It does not include example of sending TcSessionData instance's unique
ID to the browser.  I leave that to the developer to handle as they
see fit.  Also, this code was not tested, and may not compile and run
as written here.


	// Constants used in these examples
	private static final String EXPECTED_LTI_MESSAGE_TYPE =
			"basic-lti-launch-request";
	private static final String EXPECTED_LTI_VERSION = "LTI-1p0";
	private static final String SESSION_ATTR_TC_DATA = "TcSessionData";


	// doPost() example when LTI is being launched by TC
	protected void doPost(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		if (verifyPost(request)) {
			TcSessionData tcSessionData = lockInSession(request);
		}
	}

	// doGet() example for browser request working with the roster
	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException
	{
		TcSessionData tcSessionData = retrieveLockFromSession(request);
		String tpId = request.getParameter("tp_id");
		if ((tcSessionData == null) || !tcSessionData.matchTpId(tpId)) {
			// TODO: throw exception
		}
		// Getting roster, probably not something to do with every browser request...
		List<String> emailAddresses = RosterClientUtils.getRoster(tcSessionData);
	}

	/**
	 * This verifies the post has matching signature and unique nonce.
	 */
	private boolean verifyPost(HttpServletRequest request)
	throws ServletException, IOException 
	{
		return RequestSignatureUtils.verifySignature(
				request,
				request.getParameter("oauth_consumer_key"),
				"secret");
	}


	/**
	 * This records TcSessionData holding LTI session specific data, and returns
	 * it for sharing it's unique key with the browser, ensuring requests coming
	 * from the browser carrying this key are for the correct LTI session.
	 * 
	 * @param request HttpServletRequest holding the session
	 */
	private TcSessionData lockInSession(HttpServletRequest request) {
		// Store TC data in session.
		TcSessionData result = new TcSessionData(request);
		// Throw IllegalStateException if critical data is missing, such as user's email address.
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		request.getSession().setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}


	/**
	 * 
	 * @param request HttpServletRequest holding the session
	 * @return TcSessionData for this session; null if there is none
	 */
	private TcSessionData retrieveLockFromSession(HttpServletRequest request) {
		TcSessionData result = (TcSessionData)request
				.getSession()
				.getAttribute(SESSION_ATTR_TC_DATA);
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}
