package de.mwolff.commons.command;

/**
 * Simple context for pass values across commands.
 * 
 * @author mwolff
 *
 */
public interface Context {

	/**
	 * Puts a value as key in the context
	 * @param key 
	 * @param value
	 */
	void put(String key, Object value);

	/**
	 * Gets a value from a given key.
	 * @param key
	 * @return
	 */
	Object get(String key);

	/**
	 * Gets a value from a given key as a string.
	 * @param key
	 * @return
	 */
	String getAsString(String key);

}