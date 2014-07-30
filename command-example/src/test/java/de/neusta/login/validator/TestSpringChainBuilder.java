package de.neusta.login.validator;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.mwolff.command.chainbuilder.ChainBuilder;

@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSpringChainBuilder {

	@Resource
	ChainBuilder chainBuilder;

	@Resource
	LengthValidator<LoginContext> lengthValidator;

	@Resource
	CapitalValidator<LoginContext> capitalValidator;
	
	@Test
	public void testBuilderExists() throws Exception {
		assertNotNull(chainBuilder);
	}
	
	@Test
	public void testLengthValidatorExists() throws Exception {
		assertNotNull(lengthValidator);
		//assertEquals(14, lengthValidator.getLength());
	}
	
	public void testValidatorListIsSet() throws Exception {
		//int size = builder.getCommands().size();
		//assertTrue(size == 2);
	}
	
	@Test
	public void testCapitalValidatorExists() throws Exception {
		assertNotNull(capitalValidator);
	}
}
