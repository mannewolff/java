package de.mwolff.commons.command;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * CommandContainer that holds Command-objects. Should have the same behavior as a command (Composite Pattern).
 * 
 * @author mwolff
 *
 */
public class DefaultCommandContainer<T extends Context> implements CommandContainer<T> {
	
	private Map<Integer, Command<T>> commandList = new TreeMap<Integer, Command<T>>(new Comparator<Integer>() {
		public int compare(Integer o1, Integer o2) {
			// First wins if there are two commands with the same priority
			if (o1.intValue() >= o2.intValue()) {
				return 1;
			} else {
				return -1;
			} // returning 0 would merge keys
		}
	});

	public void addCommand(Command<T> command) {
		commandList.put(Integer.valueOf(0), command);
	}

	public void addCommand(int priority, Command<T> command) {
		commandList.put(Integer.valueOf(priority), command);
	}

	public void execute(T context) throws Exception {
		for (Command<T> command : commandList.values()) {
			((Command<T>) command).execute(context);
		}
	}

	public boolean executeAsChain(T context) {
		boolean result = true;
		for (Command<T> command : commandList.values()) {
			result = ((Command<T>) command).executeAsChain(context);
			if (result == false)
				break;
		}
		return result;
	}

}
