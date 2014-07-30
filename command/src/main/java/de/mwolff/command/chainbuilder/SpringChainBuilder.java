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
import de.mwolff.commons.command.GenericContext;
import de.mwolff.commons.command.DefaultCommandContainer;

/**
 * Generic chain builder for configuration with the spring framework.
 *
 * Note: The test of this builder is in the command-example package because I
 * don't want to have any spring dependencies in this framework. 
*/
public class SpringChainBuilder implements ChainBuilder {

	private List<Command<GenericContext>> commands = new ArrayList<Command<GenericContext>>();

	/**
	 * Injection point for the dependency framework.
	 * 
	 * @param commands
	 */
	public void setComands(final List<Command<GenericContext>> commands) {
		this.commands = commands;
	}

	private CommandContainer<GenericContext> buildChain() {
		CommandContainer<GenericContext> commandContainer = new DefaultCommandContainer<GenericContext>();
		for (Command<GenericContext> command : commands) {
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
	public boolean executeAsChain(final GenericContext context) {
		return buildChain().executeAsChain(context);
	}

}
