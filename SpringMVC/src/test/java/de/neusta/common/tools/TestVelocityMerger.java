package de.neusta.common.tools;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import de.neusta.common.tools.PropertyLoader.Methods;

public class TestVelocityMerger {

	@Test
	public void mergeSimpleKey() throws Exception {
		final VelocityMerger velocityMerger = new VelocityMerger(
				"simplemerge.vm");
		final PropertyLoader propLoader = new PropertyLoader();
		propLoader.initialize("/example.properties",	Methods.CLASSPATH);
		final Properties properties = propLoader.getProperties();
		velocityMerger.addProperties(properties);
		final String result = velocityMerger.getMergedResult();
		Assert.assertEquals("Hallo meine Welt merged.", result);
	}
}
