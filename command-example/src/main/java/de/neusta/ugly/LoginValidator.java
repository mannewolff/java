package de.neusta.ugly;

public class LoginValidator {

	private static final int LENGTH_CONFIG = 12;
	private static final String LENGTH_VALIDATOR_ERROR = "The password has to have at minimum 12 characters";
	private String passwd;
	private String errorMessage = "";

	public LoginValidator(String passwd) {
		this.passwd = passwd;
	}

	public boolean validate() {
		boolean result = true;
		result = validateLength();
		if (result) {
			result = validateCapitalCharacter();
		}
		return result;
	}


	private boolean validateLength() {
		boolean result = true;
		if (passwd.length() < LENGTH_CONFIG) {
			errorMessage = LENGTH_VALIDATOR_ERROR;
			result = false;
		}
		return result;
	}

	private boolean validateCapitalCharacter() {
		return true;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
