/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

public class PriorityThreeTestCommand<T extends GenericContext> implements Command<T> {

	public void execute(T context) {
		context.put("PriorityThreeTestCommand", "PriorityThreeTestCommand");
		String priorString = context.getAsString("priority");
        if ("NullObject".equals(priorString))
        	priorString = "";
		priorString += "3-";
		context.put("priority", priorString);
	}

	public boolean executeAsChain(T context) {
		String priorString = context.getAsString("priority");
		priorString += "C-";
		context.put("priority", priorString);
		return false;
	}

}
