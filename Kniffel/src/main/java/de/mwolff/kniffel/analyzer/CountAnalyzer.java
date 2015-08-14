package de.mwolff.kniffel.analyzer;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.Wurf;

/**
 * Analyzes a Wurf. Can return the number of ones, twos ..... sixes. Returns
 * also a sum of the whole Wurf.
 */
public class CountAnalyzer implements Analyzer {

	private static CountAnalyzer countanalyzer = new CountAnalyzer();

	public int Eins = 0;
	public int Zwei = 0;
	public int Drei = 0;
	public int Vier = 0;
	public int Fuenf = 0;
	public int Sechs = 0;
	public int Sum = 0;

	static public CountAnalyzer getInstance() {
		return countanalyzer;
	}

	public boolean analyze(Wurf wurf) {

		reset();
		Cube[] cubelist = wurf.getCubeList();

		for (Cube cube : cubelist) {
			if (cube.getValue() == 1) {
				Eins++;
				Sum+=1;
			}
			if (cube.getValue() == 2) {
				Zwei++;
				Sum+=2;
			}
			if (cube.getValue() == 3) {
				Drei++;
				Sum+=3;
			}
			if (cube.getValue() == 4) {
				Vier++;
				Sum+=4;
			}
			if (cube.getValue() == 5) {
				Fuenf++;
				Sum+=5;
			}
			if (cube.getValue() == 6) {
				Sechs++;
				Sum+=6;
			}
		}
		return true;
	}

	private void reset() {
		Eins = 0;
		Zwei = 0;
		Drei = 0;
		Vier = 0;
		Fuenf = 0;
		Sechs = 0;
		Sum = 0;
	}

}
