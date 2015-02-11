package de.neusta.common.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import de.neusta.common.tools.PropertyLoader.Methods;

public class TestPropertyLoader {

	private PropertyLoader propertyLoader; 
	
	@Before
	public void init() {
		propertyLoader = new PropertyLoader();
	}
	
	@Test
	public void propertyLoadedFromClasspath() throws Exception {
		propertyLoader.initialize("/example.properties", Methods.CLASSPATH);
		assertEquals("value", propertyLoader.getProperties().getProperty("key"));
	}

	@Test
	public void propertyLoadedNOTFromClasspath() throws Exception {
		propertyLoader.initialize("/example.properties", Methods.DEFAULT);
		assertNull("value", propertyLoader.getProperties().getProperty("key"));
	}

	@Test
	public void propertyResourceNotExsists() throws Exception {
		propertyLoader.initialize("/notExists.properties", Methods.CLASSPATH);
		assertEquals(null, propertyLoader.getProperties().getProperty("key"));
	}
}
