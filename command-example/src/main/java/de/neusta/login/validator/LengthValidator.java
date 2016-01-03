package de.neusta.login.validator;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.CommandException;
import de.mwolff.commons.command.DefaultCommand;
import de.neusta.freitag.context.LoginContext;

public class LengthValidator<T extends LoginContext> extends DefaultCommand<T> implements Command<T> {

	private static final String ERROR_MESSAGE = "The password has to be as minimum %d characters";
	private int length;
	
	@Override
	public void execute(T context) throws CommandException {

		int actLength = context.getOriginalPasswd().length();
		if (actLength < length) {
			String errorString = String.format(ERROR_MESSAGE, length);
			context.setErrorMessage(errorString);
			throw new CommandException(errorString);
		}
	}
	
	public void setLength(final int length) {
		this.length = length;
	}


}
