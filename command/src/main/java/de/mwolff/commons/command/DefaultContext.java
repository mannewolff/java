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

	private Map<String, Object> genericMap = new HashMap<String, Object>();

	public void put(String key, Object value) {
		genericMap.put(key, value);
	}

	public Object get(String key) {
		return genericMap.get(key);
	}

	public String getAsString(String key) {
		Object object = genericMap.get(key);
		if (object == null)
			object = "NullObject";
		return object.toString();
	}

}
