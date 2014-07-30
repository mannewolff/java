/**
 * Simple command framework.
 * 
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

/**
 * Simple context interface for pass values across commands.
 */
public interface GenericContext extends Context{

	void put(String key, Object value);

	Object get(String key);

	String getAsString(String key);

}