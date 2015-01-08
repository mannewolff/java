package de.neusta.common.tools;

import static de.neusta.common.controller.ControllerConstants.INDEX_PAGE;
import static de.neusta.common.tools.ACLConstants.ANONYMOUS;
import static de.neusta.common.tools.ACLConstants.LOGEDIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class TestACLTester {

	private final ACLTester aclTester = new ACLTester();

	@Test
	public void constants() throws Exception {
		new ACLConstants();
		assertEquals(ANONYMOUS, "ANONYMOUS");
		assertEquals(LOGEDIN, "LOGEDIN");
	}

	@Test
	public void constructorCreatesProperties() throws Exception {
		Properties props = null;
		props = (Properties) ReflectionTestUtils.getField(this.aclTester,
				"properties");
		assertNotNull(props);
	}

	@Test
	public void getACLListFor() throws Exception {
		final String acl = this.aclTester.getACLForSite(INDEX_PAGE);
		assertEquals(ANONYMOUS, acl);
	}

	@Test
	public void isACLNotPossible() throws Exception {
		final boolean possible = this.aclTester.isACLPossible(INDEX_PAGE,
				"NOT EXISTS");
		assertFalse(possible);
	}

	@Test
	public void isACLPossible() throws Exception {
		final boolean possible = this.aclTester.isACLPossible(INDEX_PAGE,
				ANONYMOUS);
		assertTrue(possible);
	}

}
