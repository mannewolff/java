package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.MaxCountAnalyzer;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class CountStrategy extends DefaultCommand<KniffelContext> implements
		Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;
		MaxCountAnalyzer analyzer = new MaxCountAnalyzer();
		analyzer.analyze(wurf);

		if (analyzer.Vierer) {
			context.isVierer = true;
		} else if (analyzer.Dreier) {
			context.isDreier = true;
		} else if (analyzer.Zweier) {
			context.isZweier = true;
		}

		//prepareForNextWurf(context, analyzer);

		if (context.AnzWurf == 4) {
			// Fertig !
			throw new Exception();
		}

	}

}
