package de.neusta.common.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Loads a property file from the class path.
 * 
 * @author Manfred Wolff
 * @since 1.0
 * 
 */
public class PropertyLoader {

	private Properties properties = new Properties();
	static Logger log = Logger.getLogger(PropertyLoader.class);

	public PropertyLoader(String resource) throws IOException {
		super();
		InputStream is = this.getClass().getResourceAsStream(resource);
		if (is != null) {
		properties.load(is);
		}
	}

	public Properties getProperties() {
		return (Properties) properties.clone();
	}

}
