package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import de.mwolff.kniffel.analyzer.Analyzer;
import de.mwolff.kniffel.analyzer.CountAnalyzer;
import de.mwolff.kniffel.common.Wurf;

public class FullHouseAnalyzer implements Analyzer {

	public boolean analyze(Wurf wurf) {

		boolean first = false;
		boolean second = false;

		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		countanalyzer.analyze(wurf);

		// First condition
		first = (countanalyzer.Eins == 3 || countanalyzer.Zwei == 3
				|| countanalyzer.Drei == 3 || countanalyzer.Vier == 3
				|| countanalyzer.Fuenf == 3 || countanalyzer.Sechs == 3);

		// Second condition
		if (first) {
			second = (countanalyzer.Eins == 2 || countanalyzer.Zwei == 2
					|| countanalyzer.Drei == 2 || countanalyzer.Vier == 2
					|| countanalyzer.Fuenf == 2 || countanalyzer.Sechs == 2);

		}

		return ((first == true) && (second == true));
	}

}