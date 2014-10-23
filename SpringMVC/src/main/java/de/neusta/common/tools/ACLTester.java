package de.neusta.common.tools;

import java.util.Properties;

import de.neusta.common.tools.PropertyLoader.Methods;

public class ACLTester {

	private Properties properties;
	
	public ACLTester()  {
		PropertyLoader loader = null;
		loader = new PropertyLoader("/acl.properties", Methods.CLASSPATH);
		properties = loader.getProperties();
	}

	public String getACLForSite(String key) {
		return properties.getProperty(key);
	}

	public boolean isACLPossible(String indexPage, String acl) {
		String result = properties.getProperty(indexPage);
		return acl.equals(result);
	}

}
