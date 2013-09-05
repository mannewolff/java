package de.mwolff.commons.command;

public class SimpleTestCommand implements Command {

	/* (non-Javadoc)
	 * @see de.mwolff.commons.command.Command#execute()
	 */
	public void execute(Context context) {
		context.put("SimpleTestCommand", "SimpleTestCommand");
		String priorString = context.getAsString("priority");
        if ("NullObject".equals(priorString))
        	priorString = "";
		priorString += "S-";
		context.put("priority", priorString);
	}

	public boolean executeAsChain(Context context) {
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
