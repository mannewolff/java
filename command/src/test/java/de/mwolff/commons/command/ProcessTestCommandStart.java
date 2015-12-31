package de.mwolff.commons.command;

public class ProcessTestCommandStart <T extends GenericContext> extends DefaultCommand<T> {

	public ProcessTestCommandStart(String processID) {
		super(processID);
	}

	@Override
	public String executeAsProcess(String startCommand, T context) {
		context.put("result", this.processID + " - ");
		return "Next";
	}
}
