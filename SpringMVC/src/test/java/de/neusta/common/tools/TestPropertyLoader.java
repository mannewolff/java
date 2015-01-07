package de.neusta.common.tools;

import static org.junit.Assert.*;

import org.junit.Test;

import de.neusta.common.tools.PropertyLoader.Methods;

public class TestPropertyLoader {

	@Test
	public void propertyResourceNotExsists() throws Exception {
		PropertyLoader propertyLoader = new PropertyLoader("/notExists.properties", Methods.CLASSPATH);
		assertEquals(null, propertyLoader.getProperties().getProperty("key"));
	}

	@Test
	public void propertyLoadedFromClasspath() throws Exception {
		PropertyLoader propertyLoader = new PropertyLoader(
				"/example.properties", Methods.CLASSPATH);
		assertEquals("value", propertyLoader.getProperties().getProperty("key"));
	}

	@Test
	public void propertyLoadedNOTFromClasspath() throws Exception {
		PropertyLoader propertyLoader = new PropertyLoader(
				"/example.properties", Methods.DEFAULT);
		assertNull("value", propertyLoader.getProperties().getProperty("key"));
	}
}
