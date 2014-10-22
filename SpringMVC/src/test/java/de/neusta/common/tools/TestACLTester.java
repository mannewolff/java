package de.neusta.common.tools;

import static de.neusta.common.controller.ControllerConstants.INDEX_PAGE;
import static de.neusta.common.tools.ACLConstants.*;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class TestACLTester {

	private ACLTester aclTester = new ACLTester();

	@Test
	public void constants() throws Exception {
		new ACLConstants();
		assertEquals(ANONYMOUS, "ANONYMOUS");
		assertEquals(LOGEDIN, "LOGEDIN");
	}
	
	@Test
	public void constructorCreatesProperties() throws Exception {
		Properties props = null;
		props = (Properties) ReflectionTestUtils.getField(aclTester, "properties");
		assertNotNull(props);
	}
	
	@Test
	public void getACLListFor() throws Exception {
		String acl = aclTester.getACLForSite(INDEX_PAGE);
		assertEquals(ANONYMOUS, acl);
	}
	
	@Test
	public void isACLPossible() throws Exception {
		boolean possible = aclTester.isACLPossible(INDEX_PAGE, ANONYMOUS);
		assertTrue(possible);
	}

	@Test
	public void isACLNotPossible() throws Exception {
		boolean possible = aclTester.isACLPossible(INDEX_PAGE, "NOT EXISTS");
		assertFalse(possible);
	}

}
