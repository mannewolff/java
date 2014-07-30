package de.neusta.login.validator;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLoginValidators {

	@Test
	public void testLoginContext() throws Exception {

		LoginContext loginContext = new LoginContext();
		loginContext.setName("mwolff");
		assertEquals("mwolff", loginContext.getName());

		loginContext.setOriginalPasswd("geheim");
		assertEquals("d5819db235972a3998f7c290ae583664",
				loginContext.getPasswd());

	}

	@Test(expected=Exception.class)
	public void testLengthValidator() throws Exception {

		LoginContext context = new LoginContext();
		LengthValidator<LoginContext> lengthValidator = new LengthValidator<LoginContext>();
		lengthValidator.setLength(14);
		context.setOriginalPasswd("12Characters");
		lengthValidator.execute(context);
		assertEquals("The password has to be as minimum 14 characters",
				context.getErrorMessage());

	}
	
	@Test
	public void testLengthValidatorAsChain() throws Exception {
	}

	@Test(expected=Exception.class)
	public void testCapitalValidator() throws Exception {
		LoginContext context = new LoginContext();
		CapitalValidator<LoginContext> capitalValidator = new CapitalValidator<LoginContext>();
		capitalValidator.setCountOfCapitalCharacters(1);
		context.setOriginalPasswd("12characters");
		capitalValidator.execute(context);
		assertEquals("The password has to be as minimum 1 capital character",
				context.getErrorMessage());
	}

}