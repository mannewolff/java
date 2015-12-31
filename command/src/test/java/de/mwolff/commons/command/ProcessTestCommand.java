package de.mwolff.commons.command;

public class ProcessTestCommand <T extends GenericContext> implements Command<T> {

	public ProcessTestCommand(String commandId) {
	}

	@Override
	public void execute(T context) throws CommandException {
		
	}

	@Override
	public boolean executeAsChain(T context) {
		return false;
	}

	@Override
	public String executeAsProcess(T context) {
		return "End";
	}

}
