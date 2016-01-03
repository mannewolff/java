/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

/**
 * Exception which works with this framework.
 *
 */
@SuppressWarnings("serial")
public class CommandException extends Exception {
    
    public CommandException() {
        super();
    }
     
    public CommandException(final String message) {
        super(message);
    }
    
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CommandException(Throwable cause) {
        super(cause);
    }
}
