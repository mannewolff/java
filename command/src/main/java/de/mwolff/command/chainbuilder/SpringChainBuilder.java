/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.command.chainbuilder;

import java.util.ArrayList;
import java.util.List;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.CommandContainer;
import de.mwolff.commons.command.DefaultCommandContainer;
import de.mwolff.commons.command.DefaultContext;

/**
 * Generic chain builder for configuration with the spring framework.
 *
 * Note: The test of this builder is in the command-example package because I
 * don't want to have any spring dependencies in this framework. 
*/
public class SpringChainBuilder implements ChainBuilder {

	private List<Command<DefaultContext>> commands = new ArrayList<Command<DefaultContext>>();

	/**
	 * Injection point for the dependency framework.
	 * 
	 * @param commands
	 */
	public void setValidators(final List<Command<DefaultContext>> commands) {
		this.commands = commands;
	}

	private CommandContainer<DefaultContext> buildChain() {
		CommandContainer<DefaultContext> commandContainer = new DefaultCommandContainer<DefaultContext>();
		for (Command<DefaultContext> command : commands) {
			commandContainer.addCommand(command);
		}
		return commandContainer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.mwolff.command.chainbuilder.ChainBuilder#executeAsChain(de.mwolff.
	 * commons.command.DefaultContext)
	 */
	public boolean executeAsChain(final DefaultContext context) {
		return buildChain().executeAsChain(context);
	}

}
