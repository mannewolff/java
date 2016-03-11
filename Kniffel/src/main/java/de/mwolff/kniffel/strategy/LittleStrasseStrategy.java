package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.LittleStreetAnalyzer;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class LittleStrasseStrategy extends DefaultCommand<KniffelContext> implements Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;
		LittleStreetAnalyzer littelstreetanalyzer = new LittleStreetAnalyzer();
		boolean littleStreet = littelstreetanalyzer.analyze(wurf);
		context.isLittleStreet = false;
		context.onceMore = true;
		if (littelstreetanalyzer.IsMiddle)
			context.isKuemmerChance = true;
		
		MaxCountAnalyzer countanalyzer = new MaxCountAnalyzer();
		countanalyzer.analyze(wurf);
		if (countanalyzer.Zweier)
			context.isZweier = true;

		if (littleStreet) {
			context.isLittleStreet = true;
			throw new Exception();
		}
	}
}
