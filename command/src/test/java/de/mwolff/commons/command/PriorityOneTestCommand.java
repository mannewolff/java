package de.mwolff.commons.command;

public class PriorityOneTestCommand implements Command {

	public void execute(Context context) {
		if (context != DefaultContext.NULLCONTEXT) {
			context.put("PriorityOneTestCommand", "PriorityOneTestCommand");
			String priorString = context.getAsString("priority");
			if ("NullObject".equals(priorString))
				priorString = "";
			priorString += "1-";
			context.put("priority", priorString);
		}
	}

	public boolean executeAsChain(Context context) {
		String priorString = context.getAsString("priority");
		priorString += "A-";
		context.put("priority", priorString);
		return true;
	}

}
