package de.neusta.mwolff.account;

/**
 * Simple banc account. Account must not have negative balance.
 * 
 * @author mwolff
 * 
 */
public interface Account {

	/**
	 * Returns the balance of the account. Should be 0.0 after a new Account is
	 * created.
	 * 
	 * @return The balance of the account.
	 */
	Double balance();

	/**
	 * Debit something from the account.
	 * 
	 * @param value
	 */
	void debit(final Double value) throws NegativeBankAccountException;

	/**
	 * Credit something to the account.
	 * 
	 * @param value
	 */
	void credit(final Double value);
}
