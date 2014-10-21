package de.neusta.login.validator;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;

public class LengthValidator<T extends LoginContext> extends DefaultCommand<T> implements Command<T> {

	private int length;
	
	@Override
	public void execute(T context) throws Exception {

		int actLength = context.getOriginalPasswd().length();
		if (actLength < length) {
			String errorString = String.format("The password has to be as minimum %d characters", length);
			context.setErrorMessage(errorString);
			throw new Exception(errorString);
		}
	
	}
	
	public void setLength(final int length) {
		this.length = length;
	}


}
