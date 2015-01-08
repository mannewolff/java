package de.neusta.testing.rules;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import de.neusta.framework.rules.MockRule;
import de.neusta.framework.rules.SpringContextRule;

public class TestMockRule {

	@Rule
	public TestRule contextRule = new SpringContextRule(
			new String[] { "file:src/test/resources/applicationcontext.xml" },
			this);

	@Rule
	public TestRule mockRule = new MockRule(this);

	@Autowired
	public String bar;

	@Mock
	public List<String> baz;

	@Test
	public void testBar() throws Exception {
		assertEquals("bar", this.bar);
	}

	@Test
	public void testBaz() throws Exception {
		when(this.baz.size()).thenReturn(2);
		assertEquals(2, this.baz.size());
	}
}
