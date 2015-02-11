package de.neusta.mwolff.account;

public class NegativeBankAccountException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public NegativeBankAccountException() {
		super("Bank account must not be negative.");
	}

}
