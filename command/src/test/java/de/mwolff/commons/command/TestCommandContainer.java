/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
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
		GenericContext context = new DefaultContext();
		CommandContainer<GenericContext> commandContainer = new DefaultCommandContainer<GenericContext>();
		commandContainer.addCommand(new PriorityOneTestCommand<GenericContext>());
		commandContainer.addCommand(new PriorityTwoTestCommand<GenericContext>());
		commandContainer.addCommand(new PriorityThreeTestCommand<GenericContext>());
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
		GenericContext context = new DefaultContext();
		CommandContainer<GenericContext> commandContainer = new DefaultCommandContainer<GenericContext>();
		commandContainer.addCommand(2, new PriorityThreeTestCommand<GenericContext>());
		commandContainer.addCommand(1, new PriorityOneTestCommand<GenericContext>());
		commandContainer.addCommand(1, new PriorityTwoTestCommand<GenericContext>());
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

		GenericContext context = new DefaultContext();
		CommandContainer<GenericContext> commandContainer = new DefaultCommandContainer<GenericContext>();
		commandContainer.addCommand(1, new PriorityOneTestCommand<GenericContext>());
		commandContainer.addCommand(2, new PriorityTwoTestCommand<GenericContext>());
		commandContainer.addCommand(3, new PriorityThreeTestCommand<GenericContext>());
		
		CommandContainer<GenericContext> mixedList = new DefaultCommandContainer<GenericContext>();
		mixedList.addCommand(new SimpleTestCommand<GenericContext>());
		mixedList.addCommand(commandContainer);
		
		mixedList.execute(context);
		String priorString = context.getAsString("priority");
		assertEquals("S-1-2-3-", priorString);
		mixedList.executeAsChain(context);
		priorString = context.getAsString("priority");
		assertEquals("S-1-2-3-S-A-B-C-", priorString);
	}
}
