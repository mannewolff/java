package de.neusta.login.validator;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestLoginValidators {

	@Test
	public void testLoginContext() throws Exception {

		LoginContext loginContext = new LoginContext();
		loginContext.setName("mwolff");
		assertEquals("mwolff", loginContext.getName());

		loginContext.setOriginalPasswd("geheim", "MD5");
		assertEquals("d5819db235972a3998f7c290ae583664",
				loginContext.getPasswd());

	}

	@Test(expected = Exception.class)
	public void testLengthValidatorFail() throws Exception {

		LoginContext context = new LoginContext();
		LengthValidator<LoginContext> lengthValidator = new LengthValidator<LoginContext>();
		lengthValidator.setLength(14);
		context.setOriginalPasswd("12Characters", "MD5");
		lengthValidator.execute(context);
		assertEquals("The password has to be as minimum 14 characters",
				context.getErrorMessage());

	}
	
	@Test
	public void testLengthValidatorCorrect() throws Exception {

		LoginContext context = new LoginContext();
		LengthValidator<LoginContext> lengthValidator = new LengthValidator<LoginContext>();
		lengthValidator.setLength(14);
		context.setOriginalPasswd("14CharactersOK", "MD5");
		lengthValidator.execute(context);
		assertNull(context.getErrorMessage());
	}
	
	@Test(expected=RuntimeException.class)
	public void testIlligialMethod() throws Exception {
		LoginContext context = new LoginContext();
		context.setOriginalPasswd("14CharactersOK", "MD8");
	}

}