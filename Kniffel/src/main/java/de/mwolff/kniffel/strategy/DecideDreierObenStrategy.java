package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class DecideDreierObenStrategy extends DefaultCommand<KniffelContext> implements
		Command<KniffelContext> {

	private int whatDreier(Wurf wurf) {
		MaxCountAnalyzer maxCountAnalyzer = new MaxCountAnalyzer();
		maxCountAnalyzer.analyze(wurf);
		if (maxCountAnalyzer.Dreier1) return 1;
		if (maxCountAnalyzer.Dreier2) return 2;
		if (maxCountAnalyzer.Dreier3) return 3;
		if (maxCountAnalyzer.Dreier4) return 4;
		if (maxCountAnalyzer.Dreier5) return 5;
		if (maxCountAnalyzer.Dreier6) return 6;
		return 0;
	}

	@Override
	public void execute(KniffelContext context) throws Exception {

		Board board = context.ActBoard;
		Wurf wurf = context.ActWurf;
		
		if (context.isDreier) {
			decideDreier(context, board, wurf);
		}
	}

	private void decideDreier(KniffelContext context, Board board, Wurf wurf)
			throws Exception {

		int auge = whatDreier(wurf);

		// TODO Konfiguration
		if (context.isFullHouse) {
			if (auge < 3) {
				return;
			}	
		}
		
		// TODO Konfiguration
		int values = wurf.getValues();
		if (values > 26) {
			return;
		}
		
		// Eintragen
		int position = board.isFree(Integer.valueOf(auge));
		if (position < Constants.FULL) {
			board.setOben(Integer.valueOf(auge), 3*auge, 0, true);
			throw new Exception();
		}
	}
}