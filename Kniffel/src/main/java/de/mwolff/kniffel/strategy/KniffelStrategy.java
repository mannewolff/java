package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class KniffelStrategy extends AbstractKniffelStrategy<KniffelContext>
		implements Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;
		MaxCountAnalyzer countanalyzer = new MaxCountAnalyzer();
		countanalyzer.analyze(wurf);
		context.isKniffel = false;

		if (countanalyzer.Kniffel) {
			context.isKniffel = true;
			markInBoard(context);
			if (context.isKniffel)
				throw new Exception();
		}
	}

	@Override
	public void markInBoard(KniffelContext context) {
		// Kniffel wird IMMER eingetragen. Sind alle Kniffel belegt werden
		// 100 als Bonus hinzugefügt und
		// a. der Kniffel oben eingetragen.
		// b. der Kniffel als 4er Pasch eingetragen
		// c. der Kniffel als 3er pasch eingetragen
		// d. der Kniffel als Full House eingetragen
		// e. der Kniffel als Grosse eingetragen
		// f. der Kniffel als Kleine eingetragen
		// g. der Kniffel als Chance eingetragen

		Board board = context.ActBoard;
		int position = board.isFree(Constants.KNIFFEL);
		System.out.println("Position = " + position);
		if (position < Constants.FULL) {
			board.setNextUnten(context, Constants.KNIFFEL, 50);
		} else {
			// später
			context.isKniffel = false;
		}
	}
}