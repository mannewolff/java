package de.neusta.common.tools;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class TestPropertyLoader {

	@Test
	public void propertyResourceNotExsists() throws Exception {
		@SuppressWarnings("unused")
		PropertyLoader propertyLoader = new PropertyLoader("/notExists.properties");
	}
	
	@Test
	public void propertyLoadedFromClasspath() throws Exception {
		PropertyLoader propertyLoader = new PropertyLoader("/example.properties");
		assertEquals("value", propertyLoader.getProperties().getProperty("key"));
	}

}
