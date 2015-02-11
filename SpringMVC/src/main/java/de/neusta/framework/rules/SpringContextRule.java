package de.neusta.framework.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringContextRule implements TestRule {

	/** A list of class-path contexts. */
	private final String[] locations;

	/** The target test. */
	private final Object target;

	public SpringContextRule(final String[] locations, final Object target) {
		this.locations = locations;
		this.target = target;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				final ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
						SpringContextRule.this.locations);
				context.getAutowireCapableBeanFactory().autowireBean(
						SpringContextRule.this.target);
				context.start();
				base.evaluate();
				context.close();
			}
		};
	}
}
