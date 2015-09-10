This is README.txt for project lti-utils

[Purpose]
=========

This project provides utilities for LTI communication between TC (Tool
Consumer) and TP (Tool Provider), along with giving TP information it
can store for communicating with the browser.  It supplies utiltites
for communicating the inital LTI launch and also for communication
with the LTI roster and settings services.

[Security]
==========

Oauth is used to verify that launch requests to the LTI tool are authorized.

HTTP operations within these utilities do not verify SSL information.
The operating assumption is that SSL is implemented by the environment
running the LTI tool.  I.e. and a load balancer or a Apache http
server is managing SSL.

[Setup]
=======

Step 1: Add the LTI-Utils dependency to the application that you would like to 
convert to an LTI Tool by modifying the application's pom.xml file using the 
the latest version:

   <dependency>
      <groupId>lti</groupId>
      <artifactId>lti-utils</artifactId>
      <version>1.4</version>
      <type>jar</type>
      <scope>compile</scope>
   </dependency>

Step 2: Create a new servlet that will utilize lti-utils in the LTI Tool 
application. This servlet should have a doPost method similar to the following:

   	private String key =    "12345";
	private String secret = "secret";
	private String url =    "http://localhost:8082/sectionsUtilityTool/ltiservlet/";  //url leading to the LTI servlet for application

   protected void doPost(HttpServletRequest request,HttpServletResponse response){
      M_log.debug("doPOST: Called");
      try {
         boolean myRequest = RequestSignatureUtils.verifySignature(request, key, secret, url);
         System.out.println("MyRequest: " + myRequest);
      }catch(Exception e) {
         M_log.error("POST request has some exceptions",e);
      }		
   }

Step 3: Build the application (usually done with the mvn install command) and 
deploy it on Tomcat.

Step 4: Once the application is deployed, visit one of the following sites:
*http://www.imsglobal.org/developers/LTI/test/v1p1/lms.php
*http://jsfiddle.net/lsloan/va04o89n/

These sites link to tools that can test to verify the handshake between Tool 
Consumer (the LMS like Canvas or CTools) and Tool Provider (the application 
like Google Drive or something built in house like Canvas Course Manager).

Step 5: Test your handshake.

To test the handshake between TC (Tool Consumer) and TP (Tool Provider) simply 
modify the top three fields on the page (Launch URL, Key, Secret) on either of 
the above pages mentioned in the links mentioned in step 4. The fields must be 
modified to match the parameters in Step 2 specific to the method 
verifySignature.

NOTE: Aside from matching, the URL needs to be set to the URL that is declared 
in the web.xml of your project.

NOTE: Specifically for the IMS Global site, when the parameters on the page 
are updated the "Recompute Launch Data" button needs to be clicked for the 
changes to take affect.

Using the System.out.println in the code for step 2, you should be able to see 
the following in your catalina.out log when you press "Launch" (jsfiddle) or 
"Press to Launch" (IMS Global):

   MyRequest: true

