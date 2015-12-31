package de.mwolff.commons.command;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessCommandTest {

	@Test
	public void executeAsProcessSimpleTest() throws Exception {
		ProcessTestCommand<GenericContext> processTestCommand = new ProcessTestCommand<GenericContext>("Start");
		GenericContext context = new DefaultContext();
		String result = processTestCommand.executeAsProcess(context);
		assertEquals("End", result);
	}

}
