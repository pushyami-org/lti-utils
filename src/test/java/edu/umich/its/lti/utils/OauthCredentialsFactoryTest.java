package edu.umich.its.lti.utils;


//import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.StringReader;
import java.util.Properties;
//import static org.hamcrest.CoreMatchers.notNullValue;
//import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OauthCredentialsFactoryTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	// test that can get properties
	// test that getting unset properties is ok.
	// test that need secret and key but url is optional
	// test that properties modified after the fact aren't
	// reflected in a factory created before the property is changed.

	static String simpleProperties = "testb.secret=MYCROFT\ntestb.url=SOMEWHEREafterall";

	@Test
	public void testOauthCredentialsFactoryNullProperties() {
		OauthCredentialsFactory oacf = new OauthCredentialsFactory(null);
		assertThat(oacf,is(notNullValue()));
		OauthCredentials oac = oacf.getOauthCredentials("HOWDY");
		assertThat(oac,is(notNullValue()));
		assertThat(oac.getSecret(),is(nullValue()));
	}

	@Test
	public void testOauthCredentialsFactoryEmptyProperties() {
		OauthCredentialsFactory oacf = new OauthCredentialsFactory(new Properties());
		assertThat(oacf,is(notNullValue()));
		OauthCredentials oac = oacf.getOauthCredentials("HOWDY");
		assertThat(oac,is(notNullValue()));
		assertThat(oac.getSecret(),is(nullValue()));
	}

	@Test
	public void testGetOauthCredentialsOne() {

		Properties p = new Properties();
		p.put("testa.secret","shhhhhhh");
		OauthCredentialsFactory oacf = new OauthCredentialsFactory(p);
		OauthCredentials oac = oacf.getOauthCredentials("testa");
		assertThat(oac.getKey(),is("testa"));
		assertThat(oac.getSecret(),is("shhhhhhh"));
		assertThat(oac.getUrl(),is(nullValue()));
	}

	@Test
	public void testGetOauthCredentialsOneIsolated() {

		Properties p = new Properties();
		p.put("testa.secret","shhhhhhh");
		OauthCredentialsFactory oacf = new OauthCredentialsFactory(p);
		p.put("testa.secret","YELLING");
		OauthCredentials oac = oacf.getOauthCredentials("testa");
		assertThat(oac.getKey(),is("testa"));
		assertThat(oac.getSecret(),is("shhhhhhh"));
		assertThat(oac.getUrl(),is(nullValue()));
	}

	@Test
	public void testGetOauthCredentialsOneFromText() throws IOException {

		InputStream is = new ByteArrayInputStream(simpleProperties.getBytes());

		Properties p = new Properties();
		p.load(is);
		OauthCredentialsFactory oacf = new OauthCredentialsFactory(p);
		OauthCredentials oac = oacf.getOauthCredentials("testb");
		assertThat(oac.getKey(),is("testb"));
		assertThat(oac.getSecret(),is("MYCROFT"));
	}

	@Test
	public void testGetOauthCredentialsThree() {

		Properties p = new Properties();

		p.put("testa.secret","shhhhhhh");
		p.put("zaphod.secret","mayfly");
		p.put("three.secret","666");

		OauthCredentialsFactory oacf = new OauthCredentialsFactory(p);
		OauthCredentials oacA = oacf.getOauthCredentials("testa");
		OauthCredentials oacB = oacf.getOauthCredentials("three");
		OauthCredentials oacC = oacf.getOauthCredentials("zaphod");

		assertThat(oacA.getKey(),is("testa"));
		assertThat(oacA.getSecret(),is("shhhhhhh"));

		assertThat(oacB.getKey(),is("three"));
		assertThat(oacB.getSecret(),is("666"));

		assertThat(oacC.getKey(),is("zaphod"));
		assertThat(oacC.getSecret(),is("mayfly"));

	}

}
