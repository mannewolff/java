package de.mwolff.commons.command;

/**
 * Command interface for the command framework.
 * 
 * @author mwolff
 *
 */
public interface Command {

	/**
	 * Executes the command.
	 * @param context
	 */
	void execute(Context context) throws Exception;

	/**
	 * Executes a command as a chain.
	 * @param context
	 * @return False if there was an error or the task is completed.
	 */
	boolean executeAsChain(Context context);

}