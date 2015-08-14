package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import de.mwolff.kniffel.analyzer.Analyzer;
import de.mwolff.kniffel.analyzer.CountAnalyzer;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.Wurf;

public class LittleStreetAnalyzer implements Analyzer {

	public boolean IsMiddle = false;

	public boolean analyze(Wurf wurf) {
		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		countanalyzer.analyze(wurf);

		boolean result = false;
		IsMiddle = false;

		result = (((countanalyzer.Eins >= 1) && (countanalyzer.Zwei >= 1)
				&& (countanalyzer.Drei >= 1) && (countanalyzer.Vier >= 1))
				|| ((countanalyzer.Zwei >= 1) && (countanalyzer.Drei >= 1)
						&& (countanalyzer.Vier >= 1) && (countanalyzer.Fuenf >= 1)) || ((countanalyzer.Drei >= 1)
				&& (countanalyzer.Vier >= 1) && (countanalyzer.Fuenf >= 1) && (countanalyzer.Sechs >= 1)));

		if ((countanalyzer.Zwei >= 1) && (countanalyzer.Drei >= 1)
				&& (countanalyzer.Vier >= 1) && (countanalyzer.Fuenf >= 1)) {
			IsMiddle = true;
		}

		return result;
	}

	public Boolean[] getBoolMatrix(final Wurf wurf) {

		Boolean[] bool = {Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE} ;
		
		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		countanalyzer.analyze(wurf);
		
		if ((countanalyzer.Eins >= 1) && (countanalyzer.Zwei >= 1)
		&& (countanalyzer.Drei >= 1) && (countanalyzer.Vier >= 1)) {
			boolean eins = false;
			boolean zwei = false;
			boolean drei = false;
			boolean vier = false;
			Cube[] cubelist = wurf.getCubeList();
			int count = 0;
			for (Cube cube : cubelist) {
				if ((cube.getValue() == 1) && !eins) {
					bool[count] = true;
					eins = true;
				}
				if ((cube.getValue() == 2) && !zwei) {
					bool[count] = true;
					zwei = true;
				}
				if ((cube.getValue() == 3) && !drei) {
					bool[count] = true;
					drei = true;
				}
				if ((cube.getValue() == 4) && !vier) {
					bool[count] = true;
					vier = true;
				}
				count++;
			}
		}
		
		if ((countanalyzer.Zwei >= 1) && (countanalyzer.Drei >= 1)
		&& (countanalyzer.Fuenf >= 1) && (countanalyzer.Vier >= 1)) {
			boolean zwei = false;
			boolean drei = false;
			boolean vier = false;
			boolean fuenf = false;
			Cube[] cubelist = wurf.getCubeList();
			int count = 0;
			for (Cube cube : cubelist) {
				if ((cube.getValue() == 5) && !fuenf) {
					bool[count] = true;
					fuenf = true;
				}
				if ((cube.getValue() == 2) && !zwei) {
					bool[count] = true;
					zwei = true;
				}
				if ((cube.getValue() == 3) && !drei) {
					bool[count] = true;
					drei = true;
				}
				if ((cube.getValue() == 4) && !vier) {
					bool[count] = true;
					vier = true;
				}
				count++;
			}
		}
		
		if ((countanalyzer.Fuenf >= 1) && (countanalyzer.Sechs >= 1)
		&& (countanalyzer.Drei >= 1) && (countanalyzer.Vier >= 1)) {
			boolean sechs = false;
			boolean drei = false;
			boolean vier = false;
			boolean fuenf = false;
			Cube[] cubelist = wurf.getCubeList();
			int count = 0;
			for (Cube cube : cubelist) {
				if ((cube.getValue() == 5) && !fuenf) {
					bool[count] = true;
					fuenf = true;
				}
				if ((cube.getValue() == 6) && !sechs) {
					bool[count] = true;
					sechs = true;
				}
				if ((cube.getValue() == 3) && !drei) {
					bool[count] = true;
					drei = true;
				}
				if ((cube.getValue() == 4) && !vier) {
					bool[count] = true;
					vier = true;
				}
				count++;
			}
		}
		return bool;
	}
}