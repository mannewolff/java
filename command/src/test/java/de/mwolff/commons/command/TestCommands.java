package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestCommands {

	@Test
	public void testCommandInterface() throws Exception {
		GenericContext context = new DefaultContext();
		Command<GenericContext> command = new SimpleTestCommand<GenericContext>();
		command.execute(context);
		assertEquals("SimpleTestCommand", context.getAsString("SimpleTestCommand"));
	}

	@Test
	public void testPriorityCommands() throws Exception {
		GenericContext context = new DefaultContext();
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
		GenericContext context = new DefaultContext();
		Command<GenericContext> command = new PriorityTwoTestCommand<GenericContext>();
		command.execute(context);
		assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
	}

	@Test
	public void testPriorThreeFirstCall() throws Exception {
		GenericContext context = new DefaultContext();
		Command<GenericContext> command = new PriorityThreeTestCommand<GenericContext>();
		command.execute(context);
		assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
	}

	@Test
	public void testCommandWithResult() throws Exception {
		Command<GenericContext> command = new SimpleTestCommand<GenericContext>();
		boolean result = command.executeAsChain(DefaultContext.NULLCONTEXT);
		assertTrue(result);
	}

	@Test
	public void testException() throws Exception {
		Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
		DefaultContext context = new DefaultContext();
		assertFalse(command.executeAsChain(context));
	}

	@Test
	public void testDefaultBehaviorWithException() throws Exception {
		Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
		DefaultContext context = new DefaultContext();
		command.executeAsChain(context);
		String result = context.getAsString("executed");
		assertEquals("true", result);
	}
	
	@Test
	public void testDefaultCommandAsChainFalse() throws Exception {
		ExceptionCommand<GenericContext> defaultCommand = new ExceptionCommand<GenericContext>();
		assertFalse(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
	}

	@Test
	public void testDefaultCommandAsChainTrue() throws Exception {
		DefaultCommand<GenericContext> defaultCommand = new DefaultCommand<GenericContext>();
		assertTrue(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
	}
}
