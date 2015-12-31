package de.mwolff.commons.command;

public class ProcessTestCommandEnd <T extends GenericContext> extends DefaultCommand<T> {

	public ProcessTestCommandEnd(String processID) {
		super(processID);
	}

	@Override
	public String executeAsProcess(String startCommand, T context) {
		String result = context.getAsString("result");
		result += this.processID + " - ";
		context.put("result", result);
		return super.executeAsProcess("", context);
	}
}
