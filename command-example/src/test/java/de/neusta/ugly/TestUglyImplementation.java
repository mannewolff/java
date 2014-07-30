package de.neusta.ugly;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestUglyImplementation {

	@Test
	public void testLengthMethod() throws Exception {
		
		LoginValidator loginValidator = new LoginValidator("geheim");
		boolean result = loginValidator.validateLength();
		assertFalse(result);
		if (! result) {
			String error = loginValidator.getErrorMessage();
			assertEquals("The password has to have at minimum 12 characters", error);
		}
	}
}
