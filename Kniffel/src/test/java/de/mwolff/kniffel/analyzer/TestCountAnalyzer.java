package de.mwolff.kniffel.analyzer;


import static org.junit.Assert.*;

import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;


public class TestCountAnalyzer {

	@Test
	public void test3One() throws Exception {
		Cube[] cubeList = CubeTester.prepareCubeList(1, 1, 1, 2, 2);
		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubeList);
		countanalyzer.analyze(wurf);
		assertEquals(3, countanalyzer.Eins);
		assertEquals(2, countanalyzer.Zwei);
	}

	@Test
	public void testSum() throws Exception {
		Cube[] cubeList = CubeTester.prepareCubeList(1, 2, 4, 5, 6);
		CountAnalyzer countanalyzer = CountAnalyzer.getInstance();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubeList);
		countanalyzer.analyze(wurf);
		assertEquals(18, countanalyzer.Sum);
		
	}
}
