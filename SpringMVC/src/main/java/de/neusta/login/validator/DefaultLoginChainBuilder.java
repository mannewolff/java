package de.neusta.login.validator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.CommandContainer;
import de.mwolff.commons.command.Context;
import de.mwolff.commons.command.DefaultCommandContainer;
import de.mwolff.commons.command.DefaultContext;

public class DefaultLoginChainBuilder implements LoginChainBuilder {

	private List<Command<DefaultContext>> validators = new ArrayList<Command<DefaultContext>>();

	@Required
	public void setValidators(final List<Command<DefaultContext>> validators) {
		this.validators = validators;
	}

	protected List<Command<DefaultContext>> getValidators() {
		return validators;
	}

	private CommandContainer<DefaultContext> buildChain() {
		CommandContainer<DefaultContext> commandContainer = new DefaultCommandContainer<DefaultContext>();
		for (Command<DefaultContext> command : validators) {
			commandContainer.addCommand(command);
		}
		return commandContainer;
	}
	
	public boolean executeAsChain(final DefaultContext context) {
		return buildChain().executeAsChain(context);
	}
	
}
