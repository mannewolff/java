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
 * 
 * @author Manfred Wolff
 * @since 1.0
 *
 */
public class VelocityMerger {

	private final String template;
	private Properties properties;

	public VelocityMerger(final String template) {
		this.template = template;
	}

	public void addProperties(final Properties properties) {
		this.properties = properties;
	}

	public String getMergedResult() {
		final VelocityEngine velocityEngine = initializeVelocityEngine();
		final VelocityContext context = new VelocityContext();
		movePropertiesToContext(context);
		final StringWriter writer = mergeTemplateWithContext(velocityEngine,
				context);
		return writer.toString();
	}

	private VelocityEngine initializeVelocityEngine() {
		final VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class",
				ClasspathResourceLoader.class.getName());
		return ve;
	}

	private StringWriter mergeTemplateWithContext(
			final VelocityEngine velocityEngine, final VelocityContext context) {
		Template veloTemplate = null;
		veloTemplate = velocityEngine.getTemplate(this.template);
		final StringWriter writer = new StringWriter();
		veloTemplate.merge(context, writer);
		return writer;
	}

	private void movePropertiesToContext(final VelocityContext context) {
		for (final String prop : this.properties.stringPropertyNames()) {
			final String value = this.properties.getProperty(prop);
			context.put(prop, value);
		}
	}

}
