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
		List<Command<GenericContext>> commandList = new ArrayList<Command<GenericContext>>();
		GenericContext context = new DefaultContext();
		Command<GenericContext> command = new ExceptionCommand<GenericContext>();
		commandList.add(command);
		builder.setComands(commandList);
		
		boolean result = builder.executeAsChain(context);
		Assert.assertFalse(result);
	}

}