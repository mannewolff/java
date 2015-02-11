package de.neusta.common.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class TestPropertyLoader {

	private PropertyLoader propertyLoader; 
	
	@Before
	public void init() {
		propertyLoader = new PropertyLoader();
	}
	
	@Test
	public void propertyLoadedFromClasspath() throws Exception {
		propertyLoader.initialize("/example.properties");
		assertEquals("value", propertyLoader.getProperties().getProperty("key"));
	}

	@Test
	public void propertyResourceNotExsists() throws Exception {
		propertyLoader.initialize("/notExists.properties");
		assertEquals(null, propertyLoader.getProperties().getProperty("key"));
	}
}
