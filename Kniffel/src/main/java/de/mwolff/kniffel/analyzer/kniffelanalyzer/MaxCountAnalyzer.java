package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import de.mwolff.kniffel.analyzer.Analyzer;
import de.mwolff.kniffel.analyzer.CountAnalyzer;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Wurf;

public class MaxCountAnalyzer implements Analyzer {

	public boolean Kniffel1;
	public boolean Kniffel2;
	public boolean Kniffel3;
	public boolean Kniffel4;
	public boolean Kniffel5;
	public boolean Kniffel6;
	public boolean Kniffel;
	public boolean Vierer1;
	public boolean Vierer2;
	public boolean Vierer3;
	public boolean Vierer4;
	public boolean Vierer5;
	public boolean Vierer6;
	public boolean Vierer;
	public boolean Dreier1;
	public boolean Dreier2;
	public boolean Dreier3;
	public boolean Dreier4;
	public boolean Dreier5;
	public boolean Dreier6;
	public boolean Dreier;
	public boolean Zweier1;
	public boolean Zweier2;
	public boolean Zweier3;
	public boolean Zweier4;
	public boolean Zweier5;
	public boolean Zweier6;
	public boolean Zweier;

	/**
	 * Returns true if one of the values is greater/equal than 2
	 */
	public boolean analyze(Wurf wurf) {

		reset();
		CountAnalyzer countAnalyzer = new CountAnalyzer();
		countAnalyzer.analyze(wurf);

		if (countAnalyzer.Eins == 5)
			Kniffel1 = true;
		if (countAnalyzer.Zwei == 5)
			Kniffel2 = true;
		if (countAnalyzer.Drei == 5)
			Kniffel3 = true;
		if (countAnalyzer.Vier == 5)
			Kniffel4 = true;
		if (countAnalyzer.Fuenf == 5)
			Kniffel5 = true;
		if (countAnalyzer.Sechs == 5)
			Kniffel6 = true;

		if ((Kniffel1) || (Kniffel2) || (Kniffel3) || (Kniffel4) || (Kniffel5)
				|| (Kniffel6))
			Kniffel = true;

		if (countAnalyzer.Eins == 4)
			Vierer1 = true;
		if (countAnalyzer.Zwei == 4)
			Vierer2 = true;
		if (countAnalyzer.Drei == 4)
			Vierer3 = true;
		if (countAnalyzer.Vier == 4)
			Vierer4 = true;
		if (countAnalyzer.Fuenf == 4)
			Vierer5 = true;
		if (countAnalyzer.Sechs == 4)
			Vierer6 = true;

		if ((Vierer1) || (Vierer2) || (Vierer3) || (Vierer4) || (Vierer5)
				|| (Vierer6))
			Vierer = true;

		if (countAnalyzer.Eins == 3)
			Dreier1 = true;
		if (countAnalyzer.Zwei == 3)
			Dreier2 = true;
		if (countAnalyzer.Drei == 3)
			Dreier3 = true;
		if (countAnalyzer.Vier == 3)
			Dreier4 = true;
		if (countAnalyzer.Fuenf == 3)
			Dreier5 = true;
		if (countAnalyzer.Sechs == 3)
			Dreier6 = true;

		if ((Dreier1) || (Dreier2) || (Dreier3) || (Dreier4) || (Dreier5)
				|| (Dreier6))
			Dreier = true;

		if (countAnalyzer.Eins == 2)
			Zweier1 = true;
		if (countAnalyzer.Zwei == 2)
			Zweier2 = true;
		if (countAnalyzer.Drei == 2)
			Zweier3 = true;
		if (countAnalyzer.Vier == 2)
			Zweier4 = true;
		if (countAnalyzer.Fuenf == 2)
			Zweier5 = true;
		if (countAnalyzer.Sechs == 2)
			Zweier6 = true;
		if ((Zweier1) || (Zweier2) || (Zweier3) || (Zweier4) || (Zweier5)
				|| (Zweier6))
			Zweier = true;

		return true;
	}

	private void reset() {
		Kniffel1 = false;
		Kniffel2 = false;
		Kniffel3 = false;
		Kniffel4 = false;
		Kniffel5 = false;
		Kniffel6 = false;
		Vierer1 = false;
		Vierer2 = false;
		Vierer3 = false;
		Vierer4 = false;
		Vierer5 = false;
		Vierer6 = false;
		Dreier1 = false;
		Dreier2 = false;
		Dreier3 = false;
		Dreier4 = false;
		Dreier5 = false;
		Dreier6 = false;
		Zweier1 = false;
		Zweier2 = false;
		Zweier3 = false;
		Zweier4 = false;
		Zweier5 = false;
		Zweier6 = false;
	}

	public Integer getColumnOfVierer() {
		if (Vierer1) return Constants.EINS; else
			if (Vierer2) return Constants.ZWEI; else
				if (Vierer3) return Constants.DREI; else
					if (Vierer4) return Constants.VIER; else
						if (Vierer5) return Constants.FUENF; else
							if (Vierer6) return Constants.SECHS; else
							  return null;
	}

	public Integer getColumnOfDreier() {
		if (Dreier1) return Constants.EINS; else
			if (Dreier2) return Constants.ZWEI; else
				if (Dreier3) return Constants.DREI; else
					if (Dreier4) return Constants.VIER; else
						if (Dreier5) return Constants.FUENF; else
							if (Dreier6) return Constants.SECHS; else
							  return null;
	}
}