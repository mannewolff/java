package de.mwolff.commons.command;

public class PriorityTwoTestCommand<T extends Context> implements Command<T> {

	public void execute(T context) {
		if (context != DefaultContext.NULLCONTEXT) {
			context.put("PriorityTwoTestCommand", "PriorityTwoTestCommand");
			String priorString = context.getAsString("priority");
			if ("NullObject".equals(priorString))
				priorString = "";
			priorString += "2-";
			context.put("priority", priorString);
		}
	}

	public boolean executeAsChain(T context) {
		String priorString = context.getAsString("priority");
		priorString += "B-";
		context.put("priority", priorString);
		return true;
	}

}
