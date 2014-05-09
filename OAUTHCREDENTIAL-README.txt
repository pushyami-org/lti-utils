PROPERTIES AND OAUTHCREDENTIALS

Use of the following code is demonstrated in the method configureOauthCredentials in the Qualtrics LtiProviderVelocityViewServlet class.

To allow flexible specification of the Oauth credentials required in an LTI tool lti-utils
has the following classes:

OAUTHCREDENTIALS

This binds together all the oauth credentials for a specific key.  This object is the format expected by other  classes in the lti-utils package.  Its methods are just to access the key, secret, and optional url.


OAUTHCREDENTIALSFACTORY

The factory manages dealing with potentially many different
key / secret combinations. This factory object has a single method: 
getOauthCredentials(String consumerKey) which returns an
OauthCredentials object specific to that key. Requests for different keys will return 
different OauthCredential objects. 

The factory constructor is OauthCredentialsFactory(Properties props).
The properties in this object can specify the keys and secrets for multiple consumers.  
The properties are specified in the format below.   The format is written as it would 
be written in a java properties file.

<consumerKey>.<propertyname>=<value>

e.g 
lmsng.school.edu.secret=secret
lmsng.school.edu.url=Howdy://there.jpg


PROPERTYUTILITIES CLASS

The PropertiesUtilities static class is to help dealing with properties. It has a couple of useful utility methods:

String getStringProperty(String defaultValue,String propertyName,Properties props)

This will lookup and return a String value for the property with the requested name. 
In order of precedence the value will be taken from:
- a System property set on invocation of the JVM.
- the properties object.
- the default value.

Properties getPropertiesObjectFromURL(String propertiesFileURL)
 
This will return a properties object based on reading the file specified by the URL. The 
URL must be one that the Java URL class can handle.
