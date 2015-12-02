/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

/**
 * Command interface for the command framework. Commands may act with generic
 * command contexts.
 */
public interface Command<T extends Context> {

    /**
     * Executes the command.
     * 
     * @param context
     */
    void execute(T context) throws CommandException;

    /**
     * Executes a command as a chain. Best way to execute a command chain is to
     * execute it as a chain because exceptions are automatically handled.
     *
     * @param context
     * @return False if there was an error or true if the task is completed.
     */
    boolean executeAsChain(T context);

}