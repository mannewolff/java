/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * CommandContainer that holds Command-objects. Should have the same behavior as
 * a command (Composite Pattern).
 *
 */
public class DefaultCommandContainer<T extends Context> implements
		CommandContainer<T> {

	private Map<Integer, Command<T>> commandList = new TreeMap<Integer, Command<T>>(
			new Comparator<Integer>() {
				public int compare(Integer o1, Integer o2) {
					// First wins if there are two commands with the same
					// priority
					if (o1.intValue() >= o2.intValue()) {
						return 1;
					} else {
						return -1;
					} // returning 0 would merge keys
				}
			});

	/**
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.CommandContainer#addCommand(de.mwolff.commons.command.Command)
	 */
	public void addCommand(Command<T> command) {
		commandList.put(Integer.valueOf(0), command);
	}

	/**
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.CommandContainer#addCommand(int, de.mwolff.commons.command.Command)
	 */
	public void addCommand(int priority, Command<T> command) {
		commandList.put(Integer.valueOf(priority), command);
	}

	/**
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Command#execute(de.mwolff.commons.command.Context)
	 */
	public void execute(T context) throws Exception {
		for (Command<T> command : commandList.values()) {
			((Command<T>) command).execute(context);
		}
	}

	/**
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Command#executeAsChain(de.mwolff.commons.command.Context)
	 */
	public boolean executeAsChain(T context) {
		boolean result = true;
		for (Command<T> command : commandList.values()) {
			result = ((Command<T>) command).executeAsChain(context);
			if (result == false)
				break;
		}
		return result;
	}
}
