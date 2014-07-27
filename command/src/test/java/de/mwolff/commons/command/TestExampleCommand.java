
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
			CommandContainer<Context> container = new DefaultCommandContainer<Context>();
			container.addCommand(new PriorityOneTestCommand<Context>());
			container.addCommand(new PriorityTwoTestCommand<Context>());
			container.execute(DefaultContext.NULLCONTEXT);
		}
	
		/*
		 * Simple example. Put all commands in a container and execute it by
		 * bypassing a context. All commands in the container will be executed in
		 * the sequence they were inserted.
		 */
		@Test
		public void testExecuteCommandsWithContext() throws Exception {
			Context context = new DefaultContext();
			CommandContainer<Context> container = new DefaultCommandContainer<Context>();
			container.addCommand(new PriorityOneTestCommand<Context>());
			container.addCommand(new PriorityTwoTestCommand<Context>());
			container.execute(context);
			assertEquals("1-2-", context.getAsString("priority"));
		}
	
		/*
		 * Priority example. Put all commands in a container by adding a priority.
		 * All commands in the container will be executed in order of the priority.
		 */
		@Test
		public void testExecuteCommandsWithContextAndPriority() throws Exception {
			Context context = new DefaultContext();
			CommandContainer<Context> container = new DefaultCommandContainer<Context>();
			container.addCommand(3, new PriorityThreeTestCommand<Context>());
			container.addCommand(2, new PriorityOneTestCommand<Context>());
			container.addCommand(1, new PriorityTwoTestCommand<Context>());
			container.execute(context);
			assertEquals("2-1-3-", context.getAsString("priority"));
		}
	
		/*
		 * Composite example. You can add commands as well as command containers in
		 * a simple container.
		 */
		@Test
		public void testExecuteCommandsWithMixedContent() throws Exception {
			Context context = new DefaultContext();
			CommandContainer<Context> commandContainer = new DefaultCommandContainer<Context>();
			commandContainer.addCommand(1, new PriorityOneTestCommand<Context>());
			commandContainer.addCommand(2, new PriorityTwoTestCommand<Context>());
			commandContainer.addCommand(3, new PriorityThreeTestCommand<Context>());
	
			CommandContainer<Context> mixedList = new DefaultCommandContainer<Context>();
			mixedList.addCommand(new SimpleTestCommand<Context>());
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
	
			Context context = new DefaultContext();
			CommandContainer<Context> commandContainer = new DefaultCommandContainer<Context>();
			commandContainer.addCommand(1, new PriorityOneTestCommand<Context>());
			commandContainer.addCommand(2, new PriorityTwoTestCommand<Context>());
			commandContainer.addCommand(3, new PriorityThreeTestCommand<Context>());
	
			CommandContainer<Context> mixedList = new DefaultCommandContainer<Context>();
			mixedList.addCommand(1, new SimpleTestCommand<Context>());
			mixedList.addCommand(2, commandContainer);
	
			mixedList.executeAsChain(context);
			String priorString = context.getAsString("priority");
			assertEquals("S-A-B-C-", priorString);
		}

}
