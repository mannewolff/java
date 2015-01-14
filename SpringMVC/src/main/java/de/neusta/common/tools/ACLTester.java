package de.neusta.common.tools;

import java.util.Properties;

import de.neusta.common.tools.PropertyLoader.Methods;

public class ACLTester {

	private Properties properties;

	public ACLTester()  {
		initialize();
	}

	private void initialize()  {
		PropertyLoader loader = null;
		try {
			loader = new PropertyLoader("/acl.properties", Methods.CLASSPATH);
		} catch (Exception e) {
			// could never be
		}
		this.properties = loader.getProperties();
	}

	public String getACLForSite(final String key) {
		return this.properties.getProperty(key);
	}

	public boolean isACLPossible(final String indexPage, final String acl) {
		final String result = this.properties.getProperty(indexPage);
		return acl.equals(result);
	}

}
