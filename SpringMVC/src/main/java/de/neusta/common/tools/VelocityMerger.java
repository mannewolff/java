package de.neusta.common.tools;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Merges a velocity template with properties of the property file.
 * @author Manfred Wolff
 * @since 1.0
 *
 */
public class VelocityMerger {

	private String template;
	private Properties properties;

	public VelocityMerger(final String template) {
		this.template = template;
	}

	public void addProperties(final Properties properties) {
		this.properties = properties;
	}

	public String getMergedResult() {
		VelocityEngine velocityEngine = initializeVelocityEngine();
		VelocityContext context = new VelocityContext();
		movePropertiesToContext(context);
		StringWriter writer = mergeTemplateWithContext(velocityEngine, context);
		return writer.toString();
	}

	private StringWriter mergeTemplateWithContext(VelocityEngine velocityEngine,
			VelocityContext context) {
		Template veloTemplate = null;
		veloTemplate = velocityEngine.getTemplate(template);
		StringWriter writer = new StringWriter();
		veloTemplate.merge(context, writer);
		return writer;
	}

	private VelocityEngine initializeVelocityEngine() {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class",
				ClasspathResourceLoader.class.getName());
		return ve;
	}

	private void movePropertiesToContext(VelocityContext context) {
		for (String prop : properties.stringPropertyNames()) {
			String value = properties.getProperty(prop);
			context.put(prop, value);
		}
	}

}
