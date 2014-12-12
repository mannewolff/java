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

	public static enum Methods {
		CLASSPATH
	};

	public PropertyLoader(final String resource, final Methods method) {
		super();
		if (method == Methods.CLASSPATH) {
			loadPerClathpath(resource);
		}
	}

	public Properties getProperties() {
		return (Properties) properties.clone();
	}

	private void loadPerClathpath(final String resource) {
		final InputStream is = this.getClass().getResourceAsStream(resource);
		if (is != null) {
			loadProperties(is);
		}
	}

	private void loadProperties(final InputStream is) {
		try {
			properties.load(is);
		} catch (IOException e) {
			log.error("Error loading property file per classpath " + is.toString());
		}
	}

}
