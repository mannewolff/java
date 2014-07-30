/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

public class SimpleTestCommand<T extends GenericContext> implements Command<T> {

	/* (non-Javadoc)
	 * @see de.mwolff.commons.command.Command#execute()
	 */
	public void execute(T context) {
		context.put("SimpleTestCommand", "SimpleTestCommand");
		String priorString = context.getAsString("priority");
        if ("NullObject".equals(priorString))
        	priorString = "";
		priorString += "S-";
		context.put("priority", priorString);
	}

	public boolean executeAsChain(T context) {
		if (context == DefaultContext.NULLCONTEXT)
			return true;
		String priorString = context.getAsString("priority");
        if ("NullObject".equals(priorString))
        	priorString = "";
		priorString += "S-";
		context.put("priority", priorString);
		return true;
	}

}
