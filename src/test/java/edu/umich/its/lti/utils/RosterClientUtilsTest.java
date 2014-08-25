package edu.umich.its.lti.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class RosterClientUtilsTest {


	// test that a roster with names from a roster hash.
	@Test
	public void testRosterWithNamesToCsvWith3() {
		HashMap<String, HashMap<String, String>> roster = new HashMap<String,HashMap<String,String>>();
		addUserToRoster("me","Obama","Bob",roster);
		addUserToRoster("YOU","Mcconnal","Pitch",roster);
		addUserToRoster("them","Party","Tea",roster);

		List<String> csv = RosterClientUtils.buildCSVEmailFirstnameLastnameFromRoster(roster);

		assertThat(csv,is(notNullValue()));
		assertThat(csv.size(),equalTo(3));

		// check that the contents of the two are the same independent of the order.
		assertThat("contains all expected entries",csv,containsInAnyOrder("me,Obama,Bob","them,Party,Tea","YOU,Mcconnal,Pitch"));
	}

	// test getting a empty roster
	@Test
	public void testRosterWithNamesToCsvWith0() {
		HashMap<String, HashMap<String, String>> roster = new HashMap<String,HashMap<String,String>>();
		List<String> csv = RosterClientUtils.buildCSVEmailFirstnameLastnameFromRoster(roster);
		assertThat(csv,is(notNullValue()));
		assertThat(csv.size(),equalTo(0));
	}

	// support method
	void addUserToRoster(String email, String given, String family,HashMap<String, HashMap<String, String>> roster) {
		HashMap<String,String> u = new HashMap<String,String>();
		u.put("person_name_family", family);
		u.put("person_name_given", given);
		u.put("person_contact_email_primary", email);
		roster.put(email, u);
	}


}
