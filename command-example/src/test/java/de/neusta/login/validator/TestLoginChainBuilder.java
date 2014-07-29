package de.neusta.login.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.mwolff.commons.command.Context;

@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestLoginChainBuilder {

	@Resource
	LoginChainBuilder builder;

	@Resource
	LengthValidator<LoginContext> lengthValidator;
	
	@Test
	public void testBuilderExists() throws Exception {
		assertNotNull(builder);
	}
	
	@Test
	public void testLengthValidatorExists() throws Exception {
		assertNotNull(lengthValidator);
		
		//assertEquals(14, lengthValidator.getLength());
	}
	
	public void testValidatorListIsSet() throws Exception {
		//int size = builder.getValidators().size();
		//assertTrue(size > 0);
	}
}
