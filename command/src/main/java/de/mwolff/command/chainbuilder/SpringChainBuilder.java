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
import de.mwolff.commons.command.Context;
import de.mwolff.commons.command.DefaultCommandContainer;

/**
 * Generic chain builder for configuration with the spring framework.
 *
 * Note: The test of this builder is in the command-example package because I
 * don't want to have any spring dependencies in this framework. 
*/
public class SpringChainBuilder implements ChainBuilder {

	private List<Command<Context>> commands = new ArrayList<Command<Context>>();

	/**
	 * Injection point for the dependency framework.
	 * 
	 * @param commands
	 */
	public void setComands(final List<Command<Context>> commands) {
		this.commands = commands;
	}

	private CommandContainer<Context> buildChain() {
		CommandContainer<Context> commandContainer = new DefaultCommandContainer<Context>();
		for (Command<Context> command : commands) {
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
	public boolean executeAsChain(final Context context) {
		return buildChain().executeAsChain(context);
	}

}
