package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.context.KniffelContext;

public class PrintBoardStrategy extends DefaultCommand<KniffelContext> implements
		Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {
	}
}
