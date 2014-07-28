package de.mwolff.commons.command;

public class DefaultCommand<T extends Context> implements Command<T> {

	public void execute(T context) throws Exception {
	}

	public boolean executeAsChain(T context) {
		boolean result = true;
		try {
			execute(context);
		} catch (Exception e) {
			result = false;
		}
		return result;
	}
}
