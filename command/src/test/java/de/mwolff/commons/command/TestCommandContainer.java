package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestCommandContainer {

	/*
	 * Remark: Adding commands without priority will mark all with
	 *        priority 0. So the execution is in natural order.
	 */
	@Test
	public void testAddNoPriorityInCommandContainer() throws Exception {
		Context context = new DefaultContext();
		CommandContainer commandContainer = new DefaultCommandContainer();
		commandContainer.addCommand(new PriorityOneTestCommand());
		commandContainer.addCommand(new PriorityTwoTestCommand());
		commandContainer.addCommand(new PriorityThreeTestCommand());
		commandContainer.execute(context);
		String priorString = context.getAsString("priority");
		assertEquals("1-2-3-", priorString);
		commandContainer.executeAsChain(context);
		priorString = context.getAsString("priority");
		assertEquals("1-2-3-A-B-C-", priorString);
	}

	/*
	 * Remark: If there are two commands with the same priority, the first
	 *         inserted Command wins ... etc.
	 */
	@Test
	public void testAddCommandWithPriorityInCommandContainer() throws Exception {
		Context context = new DefaultContext();
		CommandContainer commandContainer = new DefaultCommandContainer();
		commandContainer.addCommand(2, new PriorityThreeTestCommand());
		commandContainer.addCommand(1, new PriorityOneTestCommand());
		commandContainer.addCommand(1, new PriorityTwoTestCommand());
		commandContainer.execute(context);
		String priorString = context.getAsString("priority");
		assertEquals("1-2-3-", priorString);
		commandContainer.executeAsChain(context);
		priorString = context.getAsString("priority");
		assertEquals("1-2-3-A-B-C-", priorString);
	}
	
	/*
	 * Remark: You can add either commands or command lists.
	 */
	@Test
	public void testMixedModeInCommandContainer() throws Exception {

		Context context = new DefaultContext();
		CommandContainer commandContainer = new DefaultCommandContainer();
		commandContainer.addCommand(1, new PriorityOneTestCommand());
		commandContainer.addCommand(2, new PriorityTwoTestCommand());
		commandContainer.addCommand(3, new PriorityThreeTestCommand());
		
		CommandContainer mixedList = new DefaultCommandContainer();
		mixedList.addCommand(new SimpleTestCommand());
		mixedList.addCommand(commandContainer);
		
		mixedList.execute(context);
		String priorString = context.getAsString("priority");
		assertEquals("S-1-2-3-", priorString);
		mixedList.executeAsChain(context);
		priorString = context.getAsString("priority");
		assertEquals("S-1-2-3-S-A-B-C-", priorString);
	}
}
