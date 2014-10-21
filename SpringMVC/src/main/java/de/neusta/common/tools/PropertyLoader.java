package de.neusta.common.tools;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Loads a property file from the class path.
 * @author Manfred Wolff
 * @since 1.0
 *
 */
public class PropertyLoader {

	private Properties properties = new Properties();
	static Logger log =  Logger.getLogger(PropertyLoader.class);

	
	public PropertyLoader(String resource) {
		super();
		try {
			properties.load(this.getClass().getResourceAsStream(resource));
		} catch (IOException e) {
			log.error("error creating resource: " + resource);
		}
	}

	public Properties getProperties() {
		return (Properties) properties.clone();
	}

}
