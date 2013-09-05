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
public class DefaultCommandContainer implements CommandContainer {
	
	private Map<Integer, Command> commandList = new TreeMap<Integer, Command>(new Comparator<Integer>() {
		public int compare(Integer o1, Integer o2) {
			// First wins if there are two commands with the same priority
			if (o1.intValue() >= o2.intValue()) {
				return 1;
			} else {
				return -1;
			} // returning 0 would merge keys
		}
	});

	/* (non-Javadoc)
	 * @see de.mwolff.commons.command.CommandList#addCommand(de.mwolff.commons.command.Command)
	 */
	public void addCommand(Command command) {
		commandList.put(Integer.valueOf(0), command);
	}

	/* (non-Javadoc)
	 * @see de.mwolff.commons.command.CommandList#addCommand(int, de.mwolff.commons.command.Command)
	 */
	public void addCommand(int priority, Command command) {
		commandList.put(Integer.valueOf(priority), command);
	}

    /*
     * (non-Javadoc)
     * @see de.mwolff.commons.command.Command#execute(de.mwolff.commons.command.Context)
     */
	public void execute(Context context) {
		for (Command command : commandList.values()) {
			command.execute(context);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see de.mwolff.commons.command.Command#executeAsChain(de.mwolff.commons.command.Context)
	 */
	public boolean executeAsChain(Context context) {
		boolean result = true;
		for (Command command : commandList.values()) {
			result = command.executeAsChain(context);
			if (result == false)
				break;
		}
		return result;
	}
}
