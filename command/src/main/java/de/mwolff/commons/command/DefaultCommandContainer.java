/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * CommandContainer that holds Command-objects. Should have the same behavior as
 * a command (Composite Pattern).
 */
public class DefaultCommandContainer<T extends Context> implements CommandContainer<T> {

    private final Map<Integer, Command<T>> commandList = new TreeMap<Integer, Command<T>>(new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            // First wins if there are two commands with the same
            // priority
            if (o1.intValue() >= o2.intValue()) {
                return 1;
            } else {
                return -1;
            } // returning 0 would merge keys
        }
    });

    /**
     * @see de.mwolff.commons.command.CommandContainer#addCommand(de.mwolff.commons.command.Command)
     */
    @Override
    public void addCommand(Command<T> command) {
        commandList.put(Integer.valueOf(0), command);
    }

    /**
     * @see de.mwolff.commons.command.CommandContainer#addCommand(int,
     *      de.mwolff.commons.command.Command)
     */
    @Override
    public void addCommand(int priority, Command<T> command) {
        commandList.put(Integer.valueOf(priority), command);
    }

    /**
     * @see de.mwolff.commons.command.Command#execute(de.mwolff.commons.command.Context)
     */
    @Override
    public void execute(T context) throws CommandException {
        for (final Command<T> command : commandList.values()) {
            command.execute(context);
        }
    }

    /**
     * @see de.mwolff.commons.command.Command#executeAsChain(de.mwolff.commons.command.Context)
     */
    @Override
    public boolean executeAsChain(T context) {
        boolean result = true;
        for (final Command<T> command : commandList.values()) {
            result = command.executeAsChain(context);
            if (!result) {
                break;
            }
        }
        return result;
    }

    /**
     * @see de.mwolff.commons.command.Command#executeAsProcess(de.mwolff.commons.command.Context)
     */
    @Override
    public String executeAsProcess(String startCommand, T context) {

        Command<T> command = null;

        for (final Command<T> actcommand : commandList.values()) {
            String processID = actcommand.getProcessID();
            if (processID.equals(startCommand)) {
                command = actcommand;
                break;
            }
        }

        if (command == null)
            return null;

        String next = command.executeAsProcess(startCommand, context);

        if ("End".equals(next))
            return "End";

        return executeAsProcess(next, context);
    }

    @Override
    public String getProcessID() {
        return null;
    }
}
