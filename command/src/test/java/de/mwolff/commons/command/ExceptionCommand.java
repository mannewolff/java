/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

public class ExceptionCommand<T extends Context> extends DefaultCommand<T> implements Command<T> {

	public void execute(T context) throws Exception {
		context.put("executed", "true");
		throw new Exception();

	}
}
