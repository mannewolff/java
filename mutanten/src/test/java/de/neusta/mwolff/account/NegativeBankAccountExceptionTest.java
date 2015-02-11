package de.neusta.mwolff.account;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NegativeBankAccountExceptionTest {

	@Test
	public void exceptionMessage() throws Exception {
		NegativeBankAccountException negativeBankAccountException = new NegativeBankAccountException();
		assertEquals("Bank account must not be negative.", negativeBankAccountException.getMessage());
	}

}
