/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

/**
 * Interface of a command container.
 */
public interface CommandContainer<T extends Context> extends Command<T> {

	/**
	 * Adds a <code>Command</code> to the list. Because a
	 * <code>CommandList</code> is a <code>Command</code> you can add
	 * <code>CommandList</code> objects as well.
	 * 
	 * @param command
	 */
	void addCommand(Command<T> command);

	/**
	 * Adds a <code>Command</code> to the list via priority. Because a
	 * <code>CommandList</code> is a <code>Command</code> you can add
	 * <code>CommandList</code> objects as well.
	 * 
	 * @param priority
	 * @param command
	 */
	void addCommand(int priority, Command<T> command);
}