package de.mwolff.commons.command;

public class PriorityTwoTestCommand implements Command {

	public void execute(Context context) {
		if (context != DefaultContext.NULLCONTEXT) {
			context.put("PriorityTwoTestCommand", "PriorityTwoTestCommand");
			String priorString = context.getAsString("priority");
			if ("NullObject".equals(priorString))
				priorString = "";
			priorString += "2-";
			context.put("priority", priorString);
		}
	}

	public boolean executeAsChain(Context context) {
		String priorString = context.getAsString("priority");
		priorString += "B-";
		context.put("priority", priorString);
		return true;
	}

}
