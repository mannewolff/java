/**
 * Simple command framework.
 *
 * Framework for easy building software that fits the open-close-principle.
 * @author Manfred Wolff <wolff@manfred-wolff.de>
 *         (c) neusta software development
 */
package de.mwolff.commons.command;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of a generic context.
 */
public class DefaultContext implements GenericContext {

    /**
     * A null context to execute commands without a context.
     */
    public static final GenericContext NULLCONTEXT = null;

    /**
     * A generic map storing the key/value pairs.
     */
    private final Map<String, Object> genericMap = new HashMap<String, Object>();

    /**
     * (non-Javadoc)
     * 
     * @see de.mwolff.commons.command.GenericContext#put(java.lang.String,
     *      java.lang.Object)
     */
    @Override
    public void put(String key, Object value) {
        genericMap.put(key, value);
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.mwolff.commons.command.GenericContext#get(java.lang.String)
     */
    @Override
    public Object get(String key) {
        return genericMap.get(key);
    }

    /**
     * (non-Javadoc)
     * 
     * @see de.mwolff.commons.command.GenericContext#getAsString(java.lang.String)
     */
    @Override
    public String getAsString(String key) {
        Object object = genericMap.get(key);
        if (object == null) {
            object = "NullObject";
        }
        return object.toString();
    }
}
