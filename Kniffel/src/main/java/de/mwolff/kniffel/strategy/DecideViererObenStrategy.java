package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Configuration;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class DecideViererObenStrategy extends DefaultCommand<KniffelContext> implements
		Command<KniffelContext> {

	private int whatVierer(Wurf wurf) {
		MaxCountAnalyzer maxCountAnalyzer = new MaxCountAnalyzer();
		maxCountAnalyzer.analyze(wurf);
		if (maxCountAnalyzer.Vierer1) return 1;
		if (maxCountAnalyzer.Vierer2) return 2;
		if (maxCountAnalyzer.Vierer3) return 3;
		if (maxCountAnalyzer.Vierer4) return 4;
		if (maxCountAnalyzer.Vierer5) return 5;
		if (maxCountAnalyzer.Vierer6) return 6;
		return 0;
	}

	@Override
	public void execute(KniffelContext context) throws Exception {

		Board board = context.ActBoard;
		Wurf wurf = context.ActWurf;
		
		int[] bonuses = board.getRowsForBonus();
		
		if (context.isVierer) {
			int auge = whatVierer(wurf);
			int augenzahl = wurf.getValues();

			// TODO Gucken, ob es zwingend eine Reihe gibt, die nur noch von
			//      dem Viererpasch ausgeglichen werden kann.
			int row = 0;
			for (int i : bonuses) {
				if (i < 0) {
					int value = board.get(Integer.valueOf(auge), row);
					if (value == 0) {
						board.setOben(Integer.valueOf(auge), 4 * auge, row, false);
						throw new Exception();
					}
				}
				row++;
			}

			// Wenn nicht: >= x ist immer Pasch
			if (augenzahl >= Configuration.ViererGrenze1 ) {
				return;
			}
			

			// sonst an der ersten freien Stelle einstellen
			int position = board.isFree(Integer.valueOf(auge));
			if (position < Constants.FULL) {
				board.setOben(Integer.valueOf(auge), 4 * auge, position, false);
				throw new Exception();
			}
		}
	}
}
