package de.neusta.common.tools;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Loads a property file from the various sources.
 *
 * @author Manfred Wolff
 * @since 1.0
 *
 */
public class PropertyLoader {

	public static enum Methods {
		CLASSPATH, DEFAULT
	}

	private final Properties properties = new Properties();

	static Logger log = Logger.getLogger(PropertyLoader.class);;

	public void initialize(final String resource, final Methods method)
			throws Exception {
		if (method == Methods.CLASSPATH)
			loadPerClathpath(resource);
	}

	public Properties getProperties() {
		return (Properties) this.properties.clone();
	}

	private void loadPerClathpath(final String resource) throws Exception {
		final InputStream is = this.getClass().getResourceAsStream(resource);
		if (is != null) {
			loadProperties(is);
		}
	}

	private void loadProperties(final InputStream is) throws Exception {
		this.properties.load(is);
	}

}
