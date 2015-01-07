package de.neusta.mwolff.account;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class AccountTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public Timeout timeout = new Timeout(100); // timeout after 100ms

	private Account bankAccount;

	@Before
	public void init() {
		bankAccount = new BankAccount();
	}

	@Test
	public void balanceZeroAfterCreation() throws Exception {
		assertEquals(0.0, bankAccount.balance().doubleValue(), 0.0);
	}

	@Test
	public void positiveAfterCredit() throws Exception {
		bankAccount.credit(100.0);
		assertEquals(100.0, bankAccount.balance(), 0.0);
	}

	@Test
	public void subtractionWithPositivBalance() throws Exception {
		bankAccount.credit(100.0);
		bankAccount.debit(50.0);
		assertEquals(50.0, bankAccount.balance(), 0.0);
	}

	@Test
	public void exceptionIfBalanceIsNegative() throws Exception {
		thrown.expect(NegativeBankAccountException.class);
		thrown.expectMessage("Bank account must not be negative.");
		bankAccount.debit(50.0);
		assertEquals(0.0, bankAccount.balance(), 0.0);
	}

	// added because of mutation test !
	@Test
	public void balanceIsExactlyZero() throws Exception {
		bankAccount.credit(100.0);
		bankAccount.debit(100.0);
		assertEquals(0.0, bankAccount.balance(), 0.0);
	}

	@Test(expected = de.neusta.mwolff.account.NegativeBankAccountException.class)
	public void exceptionIfBalanceIsNegativeOldStyle() throws Exception {
		bankAccount.debit(50.0);
		assertEquals(0.0, bankAccount.balance(), 0.0);
	}
}
