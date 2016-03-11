package de.neusta.login.validator;

import de.mwolff.commons.command.AbstractDefaultChainCommand;
import de.mwolff.commons.command.iface.Command;
import de.mwolff.commons.command.iface.CommandException;

public class LengthValidator<T extends LoginContext> extends AbstractDefaultChainCommand<T>
		implements Command<T> {

	private int length;

	@Override
	public void execute(final T context) throws CommandException {

		final int actLength = context.getOriginalPasswd().length();
		if (actLength < this.length) {
			final String errorString = String.format(
					"The password has to be as minimum %d characters",
					this.length);
			context.setErrorMessage(errorString);
			throw new CommandException(errorString);
		}

	}

	public void setLength(final int length) {
		this.length = length;
	}

}
