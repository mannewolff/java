/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestCommands {

    @Test
    public void testCommandInterface() throws Exception {
        final GenericContext context = new DefaultContext();
        final Command<GenericContext> command = new SimpleTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("SimpleTestCommand", context.getAsString("SimpleTestCommand"));
    }

    @Test
    public void testPriorityCommands() throws Exception {
        final GenericContext context = new DefaultContext();
        Command<GenericContext> command = new PriorityOneTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("PriorityOneTestCommand", context.getAsString("PriorityOneTestCommand"));
        command = new PriorityTwoTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
        command = new PriorityThreeTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
    }

    @Test
    public void testPriorTwoFirstCall() throws Exception {
        final GenericContext context = new DefaultContext();
        final Command<GenericContext> command = new PriorityTwoTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
    }

    @Test
    public void testPriorThreeFirstCall() throws Exception {
        final GenericContext context = new DefaultContext();
        final Command<GenericContext> command = new PriorityThreeTestCommand<GenericContext>();
        command.execute(context);
        assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
    }

    @Test
    public void testCommandWithResult() throws Exception {
        final Command<GenericContext> command = new SimpleTestCommand<GenericContext>();
        final boolean result = command.executeAsChain(DefaultContext.NULLCONTEXT);
        assertTrue(result);
    }

    @Test
    public void testException() throws Exception {
        final Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
        final DefaultContext context = new DefaultContext();
        assertFalse(command.executeAsChain(context));
    }

    @Test
    public void testDefaultBehaviorWithException() throws Exception {
        final Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
        final DefaultContext context = new DefaultContext();
        command.executeAsChain(context);
        final String result = context.getAsString("executed");
        assertEquals("true", result);
    }

    @Test
    public void testDefaultCommandAsChainFalse() throws Exception {
        final ExceptionCommand<GenericContext> defaultCommand = new ExceptionCommand<GenericContext>();
        assertFalse(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
    }

    @Test
    public void testDefaultCommandAsChainTrue() throws Exception {
        final DefaultCommand<GenericContext> defaultCommand = new DefaultCommand<GenericContext>();
        assertTrue(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
    }
}
