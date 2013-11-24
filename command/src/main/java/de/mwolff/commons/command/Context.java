package de.mwolff.commons.command;

/**
 * Simple context for pass values across commands.
 * 
 * @author mwolff
 *
 */
public interface Context {

	void put(String key, Object value);

	Object get(String key);

	String getAsString(String key);

}