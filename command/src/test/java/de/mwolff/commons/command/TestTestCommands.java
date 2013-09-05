package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestTestCommands {

	@Test
	public void testCommandInterface() throws Exception {
		Context context = new DefaultContext();
		Command command = new SimpleTestCommand();
		command.execute(context);
		assertEquals("SimpleTestCommand", context.getAsString("SimpleTestCommand"));
	}

	@Test
	public void testPriorityCommands() throws Exception {
		Context context = new DefaultContext();
		Command command = new PriorityOneTestCommand();
		command.execute(context);
		assertEquals("PriorityOneTestCommand", context.getAsString("PriorityOneTestCommand"));
		command = new PriorityTwoTestCommand();
		command.execute(context);
		assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
		command = new PriorityThreeTestCommand();
		command.execute(context);
		assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
	}
	
	@Test
	public void testPriorTwoFirstCall() throws Exception {
		Context context = new DefaultContext();
		Command command = new PriorityTwoTestCommand();
		command.execute(context);
		assertEquals("PriorityTwoTestCommand", context.getAsString("PriorityTwoTestCommand"));
	}

	@Test
	public void testPriorThreeFirstCall() throws Exception {
		Context context = new DefaultContext();
		Command command = new PriorityThreeTestCommand();
		command.execute(context);
		assertEquals("PriorityThreeTestCommand", context.getAsString("PriorityThreeTestCommand"));
	}

	@Test
	public void testCommandWithResult() throws Exception {
		Command command = new SimpleTestCommand();
		boolean result = command.executeAsChain(DefaultContext.NULLCONTEXT);
		assertTrue(result);
	}
}
