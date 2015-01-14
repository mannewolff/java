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
		final Properties properties = new PropertyLoader("/example.properties",
				Methods.CLASSPATH).getProperties();
		velocityMerger.addProperties(properties);
		final String result = velocityMerger.getMergedResult();
		Assert.assertEquals("Hallo meine Welt merged.", result);
	}
}
