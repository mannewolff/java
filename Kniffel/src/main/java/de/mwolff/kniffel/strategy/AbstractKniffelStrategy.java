package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.context.KniffelContext;

public abstract class AbstractKniffelStrategy<T extends KniffelContext> extends
		DefaultCommand<T> implements Command<T> {

	public abstract void markInBoard(T context);

}
