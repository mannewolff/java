package de.neusta.login.validator;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.mwolff.command.chainbuilder.ChainBuilder;
import de.mwolff.commons.command.DefaultContext;

@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSpringChainBuilder {

	@Resource
	ChainBuilder<DefaultContext> chainBuilder;

	
	@Test
	public void testBuilderExists() throws Exception {
		assertNotNull(chainBuilder);
	}
	
}
