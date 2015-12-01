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
 *
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
     * (non-Javadoc)
     * 
     * @see de.mwolff.commons.command.CommandContainer#addCommand(int,
     *      de.mwolff.commons.command.Command)
     */
    @Override
    public void addCommand(int priority, Command<T> command) {
        commandList.put(Integer.valueOf(priority), command);
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.mwolff.commons.command.Command#execute(de.mwolff.commons.command.Context)
     */
    @Override
    public void execute(T context) throws Exception {
        for (final Command<T> command : commandList.values()) {
            command.execute(context);
        }
    }

    /**
     * (non-Javadoc)
     * 
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
}
