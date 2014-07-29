package de.mwolff.commons.command;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.command.chainbuilder.SpringChainBuilder;

public class TestChainBuilder {

	@Test
	public void testSpringChainBuilder() throws Exception {
		
		SpringChainBuilder builder = new SpringChainBuilder();
		List<Command<Context>> commandList = new ArrayList<Command<Context>>();
		Context context = new DefaultContext();
		Command<Context> command = new ExceptionCommand<Context>();
		commandList.add(command);
		builder.setComands(commandList);
		
		boolean result = builder.executeAsChain(context);
		Assert.assertFalse(result);
	}

}