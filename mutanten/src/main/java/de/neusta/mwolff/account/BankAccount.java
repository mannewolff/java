package de.neusta.mwolff.account;

/**
 * Implementation of a bank account.
 * 
 * @author mwolff
 * 
 */
public class BankAccount implements Account {

	private Double actBalance = new Double(0.0);

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.neusta.mwolff.account.Account#balance()
	 */
	public Double balance() {
		return actBalance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.neusta.mwolff.account.Account#debit(java.lang.Double)
	 */
	public void debit(Double value) throws NegativeBankAccountException {
		Double tempBalance = actBalance;

		tempBalance -= value;
		if (balanceIsNegative(tempBalance)) {
			throw new NegativeBankAccountException();
		}
		actBalance = tempBalance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.neusta.mwolff.account.Account#credit(java.lang.Double)
	 */
	public void credit(Double value) {
		actBalance += value;
	}

	private boolean balanceIsNegative(Double tempBalance) {
		return tempBalance < 0.0;
	}
}
