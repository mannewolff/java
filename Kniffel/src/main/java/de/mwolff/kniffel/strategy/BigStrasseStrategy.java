package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.BigStreetAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class BigStrasseStrategy extends DefaultCommand<KniffelContext>
		implements Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;
		BigStreetAnalyzer countanalyzer = new BigStreetAnalyzer();
		boolean bigStreet = countanalyzer.analyze(wurf);
		context.isBigStreet = false;

		if (bigStreet) {
			// look if there is any bigstreet in the board
			Board board = context.ActBoard;
			int position = board.isFree(Constants.BIG);
			if (position == Constants.FULL) {
				context.isBigStreet = false;
			} else {
				context.isBigStreet = true;
				throw new Exception();
			}
		}
	}
}
