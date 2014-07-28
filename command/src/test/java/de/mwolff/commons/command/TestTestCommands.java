package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestTestCommands {

	@Test
	public void testCommandInterface() throws Exception {
		Context context = new DefaultContext();
		Command<Context> command = new SimpleTestCommand<Context>();
		command.execute(context);
		assertEquals("SimpleTestCommand", context.getAsString("SimpleTestCommand"));
	}

	@Test
	public void testPriorityCommands() throws Exception {
		Context context = new DefaultContext();
		Command<Context> command = new PriorityOneTestCommand<Context>();
		command.execute(context);
		assertEquals("PriorityOneTestCommand", context.getAsString("PriorityOneTestCommand"));
		command = new PriorityTwoTestCommand<Context>();
		command.execute(context);
		assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
		command = new PriorityThreeTestCommand<Context>();
		command.execute(context);
		assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
	}
	
	@Test
	public void testPriorTwoFirstCall() throws Exception {
		Context context = new DefaultContext();
		Command<Context> command = new PriorityTwoTestCommand<Context>();
		command.execute(context);
		assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
	}

	@Test
	public void testPriorThreeFirstCall() throws Exception {
		Context context = new DefaultContext();
		Command<Context> command = new PriorityThreeTestCommand<Context>();
		command.execute(context);
		assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
	}

	@Test
	public void testCommandWithResult() throws Exception {
		Command<Context> command = new SimpleTestCommand<Context>();
		boolean result = command.executeAsChain(DefaultContext.NULLCONTEXT);
		assertTrue(result);
	}

	@Test(expected=Exception.class)
	public void testException() throws Exception {
		Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
		DefaultContext context = new DefaultContext();
		command.execute(context);
	}

	@Test
	public void testDefaultBehaviorWithException() throws Exception {
		Command<DefaultContext> command = new ExceptionCommand<DefaultContext>();
		DefaultContext context = new DefaultContext();
		try {
			command.execute(context);
		} catch (Exception e) {
			// Not the exception is tested !
		}
		String result = context.getAsString("executed");
		assertEquals("true", result);
	}
	
	@Test
	public void testDefaultCommandAsChainFalse() throws Exception {
		ExceptionCommand<Context> defaultCommand = new ExceptionCommand<Context>();
		assertFalse(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
	}

	@Test
	public void testDefaultCommandAsChainTrue() throws Exception {
		DefaultCommand<Context> defaultCommand = new DefaultCommand<Context>();
		assertTrue(defaultCommand.executeAsChain(DefaultContext.NULLCONTEXT));
	}
}
