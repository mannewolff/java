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
public interface GenericContext extends Context {

    /**
     * Saves an object to the key.
     * 
     * @param key
     * @param value
     */
    void put(String key, Object value);

    /**
     * Returns the object of the given key.
     * 
     * @param key
     * @return
     */
    Object get(String key);

    /**
     * Returns the object of the given key as String.
     * 
     * @param key
     * @return
     */
    String getAsString(String key);

}