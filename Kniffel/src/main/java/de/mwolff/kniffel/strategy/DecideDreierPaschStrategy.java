package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class DecideDreierPaschStrategy extends DefaultCommand<KniffelContext>
		implements Command<KniffelContext> {

	private int whatDreier(Wurf wurf) {
		MaxCountAnalyzer maxCountAnalyzer = new MaxCountAnalyzer();
		maxCountAnalyzer.analyze(wurf);
		if (maxCountAnalyzer.Dreier1)
			return 1;
		if (maxCountAnalyzer.Dreier2)
			return 2;
		if (maxCountAnalyzer.Dreier3)
			return 3;
		if (maxCountAnalyzer.Dreier4)
			return 4;
		if (maxCountAnalyzer.Dreier5)
			return 5;
		if (maxCountAnalyzer.Dreier6)
			return 6;
		return 0;
	}

	@Override
	public void execute(KniffelContext context) throws Exception {

		Board board = context.ActBoard;
		Wurf wurf = context.ActWurf;

		int auge = whatDreier(wurf);
		int augenzahl = wurf.getValues();

		// TODO Konfiguration
		// > 26 sollte eingetragen werden
		if (augenzahl > 26) {
			markAsDreier(context, board, augenzahl);
		}

		// Wenn kein Fullhouse mehr offen ist
		int fullhousepos = board.isFree(Constants.FULLHOUSE);
		if ((fullhousepos == Constants.FULL) && (context.isFullHouse)) {
			// geht es dann doch noch oben
			int position = board.isFree(Integer.valueOf(auge));
			if (position < Constants.FULL) {
				board.setOben(Integer.valueOf(auge), 3 * auge, 0, true);
				throw new Exception();
			}
		}

		// Dann doch oben
		int position = board.isFree(Integer.valueOf(auge));
		if (position < Constants.FULL) {
			board.setOben(Integer.valueOf(auge), 3 * auge, 0, true);
			throw new Exception();
		}
		
		// Dann doch unten
		markAsDreier(context, board, augenzahl);
	}

	private void markAsDreier(KniffelContext context, Board board, int augenzahl)
			throws Exception {
		int position = board.isFree(Constants.DREIER);
		if (position < Constants.FULL) {
			board.setNextUnten(context, Constants.DREIER, augenzahl);
			throw new Exception();
		}
	}
}
