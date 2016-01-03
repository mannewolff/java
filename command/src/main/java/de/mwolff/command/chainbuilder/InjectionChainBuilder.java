/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.command.chainbuilder;

import java.util.ArrayList;
import java.util.List;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.CommandContainer;
import de.mwolff.commons.command.Context;
import de.mwolff.commons.command.DefaultCommandContainer;

/**
 * Generic chain builder for configuration with the spring framework.
 */
public class InjectionChainBuilder<T extends Context> implements ChainBuilder<T> {

    private List<Command<T>> commands = new ArrayList<Command<T>>();

    /**
     * Injection point for the dependency framework.
     *
     * @param commands
     */
    public void setCommands(final List<Command<T>> commands) {
        this.commands = commands;
    }

    /**
     * @see de.mwolff.command.chainbuilder.ChainBuilder#executeAsChain(de.mwolff.commons.command.Context)
     */
    @Override
    public boolean executeAsChain(final T context) {
        return buildChain().executeAsChain(context);
    }

    /**
     * Builder method.
     * 
     * @return
     */
    private CommandContainer<T> buildChain() {

        final CommandContainer<T> commandContainer = new DefaultCommandContainer<T>();
        for (final Command<T> command : commands) {
            commandContainer.addCommand(command);
        }
        return commandContainer;
    }
}
