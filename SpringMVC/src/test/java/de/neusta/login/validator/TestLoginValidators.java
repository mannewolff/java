package de.neusta.login.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestLoginValidators {

	@Test(expected = RuntimeException.class)
	public void testIlligialMethod() throws Exception {
		final LoginContext context = new LoginContext();
		context.setOriginalPasswd("14CharactersOK", "MD8");
	}

	@Test
	public void testLengthValidatorCorrect() throws Exception {

		final LoginContext context = new LoginContext();
		final LengthValidator<LoginContext> lengthValidator = new LengthValidator<LoginContext>();
		lengthValidator.setLength(14);
		context.setOriginalPasswd("14CharactersOK", "MD5");
		lengthValidator.execute(context);
		assertNull(context.getErrorMessage());
	}

	@Test(expected = Exception.class)
	public void testLengthValidatorFail() throws Exception {

		final LoginContext context = new LoginContext();
		final LengthValidator<LoginContext> lengthValidator = new LengthValidator<LoginContext>();
		lengthValidator.setLength(14);
		context.setOriginalPasswd("12Characters", "MD5");
		lengthValidator.execute(context);
		assertEquals("The password has to be as minimum 14 characters",
				context.getErrorMessage());

	}

	@Test
	public void testLoginContext() throws Exception {

		final LoginContext loginContext = new LoginContext();
		loginContext.setName("mwolff");
		assertEquals("mwolff", loginContext.getName());

		loginContext.setOriginalPasswd("geheim", "MD5");
		assertEquals("d5819db235972a3998f7c290ae583664",
				loginContext.getPasswd());

	}

}