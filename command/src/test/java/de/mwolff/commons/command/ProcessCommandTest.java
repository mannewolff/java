package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProcessCommandTest {

	@Test
	public void executeAsProcessSimpleTest() throws Exception {
		ProcessTestCommandStart<GenericContext> processTestStartCommand = new ProcessTestCommandStart<GenericContext>("Start");
		GenericContext context = new DefaultContext();
		String result = processTestStartCommand.executeAsProcess("", context);
		String processflow = context.getAsString("result");
		assertEquals("Start - ", processflow);
		assertEquals("Next", result);
	}
	
	@Test
	public void getProcessNameTest() throws Exception {
		ProcessTestCommandStart<GenericContext> processTestStartCommand = new ProcessTestCommandStart<GenericContext>("Start");
		String result = processTestStartCommand.getProcessID();
		assertEquals("Start", result);
	}
	
	@Test
	public void executeTwoSimpleProcessesInARow() throws Exception {
		GenericContext context = new DefaultContext();
		ProcessTestCommandStart<GenericContext> processTestStartCommand = new ProcessTestCommandStart<GenericContext>("Start");
		ProcessTestCommandEnd<GenericContext> processTestEndCommand = new ProcessTestCommandEnd<GenericContext>("Next");
		DefaultCommandContainer<GenericContext> container = new DefaultCommandContainer<GenericContext>();
		container.addCommand(processTestStartCommand);
		container.addCommand(processTestEndCommand);
		container.executeAsProcess("Start", context);
		String processflow = context.getAsString("result");
		assertEquals("Start - Next - ", processflow);
	}

	@Test
	public void processANullContainer() throws Exception {
		GenericContext context = new DefaultContext();
		DefaultCommandContainer<GenericContext> container = new DefaultCommandContainer<GenericContext>();
		String result = container.executeAsProcess("Start", context);
		assertNull(result);
		result = container.getProcessID();
		assertNull(result);
	}
	
}
