package de.mwolff.kniffel.common;

import de.mwolff.commons.command.CommandContainer;
import de.mwolff.commons.command.DefaultCommandContainer;
import de.mwolff.kniffel.context.KniffelContext;
import de.mwolff.kniffel.strategy.BigStrasseStrategy;
import de.mwolff.kniffel.strategy.CountStrategy;
import de.mwolff.kniffel.strategy.DecideDreierObenStrategy;
import de.mwolff.kniffel.strategy.DecideDreierPaschStrategy;
import de.mwolff.kniffel.strategy.DecideViererObenStrategy;
import de.mwolff.kniffel.strategy.DecideViererPaschStrategy;
import de.mwolff.kniffel.strategy.FullHouseStrategy;
import de.mwolff.kniffel.strategy.KniffelStrategy;
import de.mwolff.kniffel.strategy.LittleStrasseStrategy;

public class KniffelGame {

	public CommandContainer<KniffelContext> getCommandContainer() {

		CommandContainer<KniffelContext> commandContainer = new DefaultCommandContainer<KniffelContext>();
		
		KniffelStrategy kniffelStrategy = new KniffelStrategy();
		commandContainer.addCommand(1, kniffelStrategy);

		BigStrasseStrategy bigStrasseStrategy = new BigStrasseStrategy();
		commandContainer.addCommand(2, bigStrasseStrategy);
		
		LittleStrasseStrategy littleStrasseStrategy = new LittleStrasseStrategy();
		commandContainer.addCommand(3, littleStrasseStrategy);
		
		FullHouseStrategy fullHouseStrategy = new FullHouseStrategy();
		commandContainer.addCommand(4, fullHouseStrategy);
		
		CountStrategy countStrategy = new CountStrategy();
		commandContainer.addCommand(5, countStrategy);

		DecideViererObenStrategy decideViererObenStrategy = new DecideViererObenStrategy(); 
		commandContainer.addCommand(6, decideViererObenStrategy);

		DecideViererPaschStrategy decideViererPaschStrategy = new DecideViererPaschStrategy(); 
		commandContainer.addCommand(7, decideViererPaschStrategy);

		DecideDreierObenStrategy decideDreierObenStrategy = new DecideDreierObenStrategy(); 
		commandContainer.addCommand(8, decideDreierObenStrategy);

		DecideDreierPaschStrategy decideDreierPaschStrategy = new DecideDreierPaschStrategy(); 
		commandContainer.addCommand(9, decideDreierPaschStrategy);

		return commandContainer;
	}

}
