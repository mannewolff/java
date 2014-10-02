/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.command.chainbuilder;

import de.mwolff.commons.command.Context;

/**
 * A chain builder interface to build chains via a configuration file.
 */
public interface ChainBuilder<T extends Context> {

	/**
	 * Executes the chain build by this builder.
	 * 
	 * @param context An implementation of a context
	 * @return True if all commands returned true.
	 */
	boolean executeAsChain(final T context);

}