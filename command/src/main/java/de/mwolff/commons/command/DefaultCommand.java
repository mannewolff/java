/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

/**
 * Default implementation for a command. You may use <code>executeAsChain</code>
 * for all executions of the <code>commmand</code> or <code>commandContainer</code>.
 */
public class DefaultCommand<T extends Context> implements Command<T> {

	/**
	 * {@inheritDoc}
	 */
	public void execute(T context) throws Exception {
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean executeAsChain(T context) {
		boolean result = true;
		try {
			execute(context);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}
}
