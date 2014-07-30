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

public class TestExampleCommand {

		/*
		 * Simplest example. Put all commands in a container and execute it. All
		 * commands in the container will be executed in the sequence they were
		 * inserted.
		 */
		@Test
		public void testExecuteCommandsWithoutContext() throws Exception {
			CommandContainer<GenericContext> container = new DefaultCommandContainer<GenericContext>();
			container.addCommand(new PriorityOneTestCommand<GenericContext>());
			container.addCommand(new PriorityTwoTestCommand<GenericContext>());
			container.execute(DefaultContext.NULLCONTEXT);
		}
	
		/*
		 * Simple example. Put all commands in a container and execute it by
		 * bypassing a context. All commands in the container will be executed in
		 * the sequence they were inserted.
		 */
		@Test
		public void testExecuteCommandsWithContext() throws Exception {
			GenericContext context = new DefaultContext();
			CommandContainer<GenericContext> container = new DefaultCommandContainer<GenericContext>();
			container.addCommand(new PriorityOneTestCommand<GenericContext>());
			container.addCommand(new PriorityTwoTestCommand<GenericContext>());
			container.execute(context);
			assertEquals("1-2-", context.getAsString("priority"));
		}
	
		/*
		 * Priority example. Put all commands in a container by adding a priority.
		 * All commands in the container will be executed in order of the priority.
		 */
		@Test
		public void testExecuteCommandsWithContextAndPriority() throws Exception {
			GenericContext context = new DefaultContext();
			CommandContainer<GenericContext> container = new DefaultCommandContainer<GenericContext>();
			container.addCommand(3, new PriorityThreeTestCommand<GenericContext>());
			container.addCommand(2, new PriorityOneTestCommand<GenericContext>());
			container.addCommand(1, new PriorityTwoTestCommand<GenericContext>());
			container.execute(context);
			assertEquals("2-1-3-", context.getAsString("priority"));
		}
	
		/*
		 * Composite example. You can add commands as well as command containers in
		 * a simple container.
		 */
		@Test
		public void testExecuteCommandsWithMixedContent() throws Exception {
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
		}
	
		/*
		 * Chain example. You can execute commands as a chain. The execution is
		 * stopped if one command returns false.
		 */
		@Test
		public void testExecuteCommandsAsChain() throws Exception {
	
			GenericContext context = new DefaultContext();
			CommandContainer<GenericContext> commandContainer = new DefaultCommandContainer<GenericContext>();
			commandContainer.addCommand(1, new PriorityOneTestCommand<GenericContext>());
			commandContainer.addCommand(2, new PriorityTwoTestCommand<GenericContext>());
			commandContainer.addCommand(3, new PriorityThreeTestCommand<GenericContext>());
	
			CommandContainer<GenericContext> mixedList = new DefaultCommandContainer<GenericContext>();
			mixedList.addCommand(1, new SimpleTestCommand<GenericContext>());
			mixedList.addCommand(2, commandContainer);
	
			mixedList.executeAsChain(context);
			String priorString = context.getAsString("priority");
			assertEquals("S-A-B-C-", priorString);
		}

}
