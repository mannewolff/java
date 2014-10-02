/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.command.chainbuilder.InjectionChainBuilder;

public class TestChainBuilder {

	@Test
	public void testSpringChainBuilder() throws Exception {
		InjectionChainBuilder<GenericContext> builder = new InjectionChainBuilder<GenericContext>();
		List<Command<GenericContext>> commandList = new ArrayList<Command<GenericContext>>();
		GenericContext context = new DefaultContext();
		Command<GenericContext> command = new ExceptionCommand<GenericContext>();
		commandList.add(command);
		builder.setCommands(commandList);
		boolean result = builder.executeAsChain(context);
		Assert.assertFalse(result);
	}

}