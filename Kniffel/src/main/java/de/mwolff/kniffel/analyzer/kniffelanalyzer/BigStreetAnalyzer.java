package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import de.mwolff.kniffel.analyzer.Analyzer;
import de.mwolff.kniffel.analyzer.CountAnalyzer;
import de.mwolff.kniffel.common.Wurf;

public class BigStreetAnalyzer implements Analyzer {

	public boolean analyze(Wurf wurf) {
		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		countanalyzer.analyze(wurf);

		boolean result = false;

		result = (((countanalyzer.Eins == 1) && (countanalyzer.Zwei == 1)
				&& (countanalyzer.Drei == 1) && (countanalyzer.Vier == 1) 
				&& (countanalyzer.Fuenf == 1)) || ((countanalyzer.Zwei == 1)
				&& (countanalyzer.Drei == 1)  && (countanalyzer.Vier == 1)
				&& (countanalyzer.Fuenf == 1) && (countanalyzer.Sechs == 1)));

		return result;
	}
}