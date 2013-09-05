package de.mwolff.commons.command;

import java.util.HashMap;
import java.util.Map;

public class DefaultContext implements Context {
	
	/**
	 * A null context to execute commands without a context.
	 */
	public static final Context NULLCONTEXT = null;

	private Map<String, Object> genericMap = new HashMap<String, Object>();
	
	/*
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Context#put(java.lang.String, java.lang.Object)
	 */
	public void put(String key, Object value) {
		genericMap.put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Context#get(java.lang.String)
	 */
	public Object get(String key) {
		return genericMap.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Context#getAsString(java.lang.String)
	 */
	public String getAsString(String key) {
		Object object = genericMap.get(key);
		if (object == null)
			object = "NullObject";
		return object.toString();
	}

	
}
