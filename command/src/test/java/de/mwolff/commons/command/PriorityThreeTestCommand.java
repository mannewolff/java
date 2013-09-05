package de.mwolff.commons.command;

public class PriorityThreeTestCommand implements Command {

	public void execute(Context context) {
		context.put("PriorityThreeTestCommand", "PriorityThreeTestCommand");
		String priorString = context.getAsString("priority");
        if ("NullObject".equals(priorString))
        	priorString = "";
		priorString += "3-";
		context.put("priority", priorString);
	}

	public boolean executeAsChain(Context context) {
		String priorString = context.getAsString("priority");
		priorString += "C-";
		context.put("priority", priorString);
		return false;
	}

}
