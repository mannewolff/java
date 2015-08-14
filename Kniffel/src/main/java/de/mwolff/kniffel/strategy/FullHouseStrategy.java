package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.FullHouseAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class FullHouseStrategy extends DefaultCommand<KniffelContext> implements Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;
		FullHouseAnalyzer fullhouseanalyzer = new FullHouseAnalyzer();
		boolean fullhouse = fullhouseanalyzer.analyze(wurf);
		context.isFullHouse = false;
		

		if (fullhouse) {
			context.isDreier = true;
			// look if there is any fullhouse in the board
			Board board = context.ActBoard;
			
			int position = board.isFree(Constants.FULLHOUSE);
			if (position == Constants.FULL) {
				context.isFullHouse = false;
			} else {
				context.isFullHouse = true;
				throw new Exception();
			}
		}
	}
}
