package de.neusta.login.validator;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;

public class CapitalValidator<T extends LoginContext> extends DefaultCommand<T>
		implements Command<T> {

	private static final String ERROR_MESSAGE = "The password has to be as minimum %d capital character";
	private int countOfCharacters;

	public void setCountOfCapitalCharacters(final int countOfCharacters) {
		this.countOfCharacters = countOfCharacters;
	}

	@Override
	public void execute(T context) throws Exception {
		boolean result = false;
		int upperChar = 0;
		String passwd = context.getOriginalPasswd();
		for (int i = 0; i < passwd.length(); i++) {
			char c = passwd.charAt(i);
			if (Character.isUpperCase(c)) {
				upperChar++;
			}
			if (upperChar >= countOfCharacters) {
				result = true;
				break;
			}
		}
		if (!result) {
			String errorString = String.format(
					ERROR_MESSAGE, countOfCharacters);
			context.setErrorMessage(errorString);
			throw new Exception(errorString);

		}
	}
}
