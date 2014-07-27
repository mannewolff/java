package de.mwolff.commons.command;

/**
 * Command interface for the command framework.
 * 
 * @author mwolff
 *
 */
public interface Command<T extends Context> {

	/**
	 * Executes the command.
	 * @param context
	 */
	void execute(T context) throws Exception;

	/**
	 * Executes a command as a chain.
	 * @param context
	 * @return False if there was an error or the task is completed.
	 */
	boolean executeAsChain(T context);

}