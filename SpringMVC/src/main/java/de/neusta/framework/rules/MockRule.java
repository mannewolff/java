package de.neusta.framework.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

public class MockRule implements TestRule {

	private final Object target;

	public MockRule(final Object target) {
		this.target = target;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				MockitoAnnotations.initMocks(MockRule.this.target);
				base.evaluate();
			}
		};
	}
}