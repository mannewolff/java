package de.neusta.testing.rules;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import de.neusta.framework.rules.MockRule;
import de.neusta.framework.rules.SpringContextRule;

import static org.mockito.Mockito.*;

public class TestMockRule {

    @Rule
    public TestRule contextRule = new SpringContextRule(new String[]{"file:src/test/resources/applicationcontext.xml"}, this);

    @Rule
    public TestRule mockRule = new MockRule(this);

    @Autowired
    public String bar;

    @Mock
    public List<String> baz;

    @Test
    public void testBar() throws Exception {
        assertEquals("bar", bar);
    }

    @Test
    public void testBaz() throws Exception {
        when(baz.size()).thenReturn(2);
        assertEquals(2, baz.size());
    }
}
