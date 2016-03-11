package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;

public class TestBigStreetAnalyzer {

	@Test
	public void testBigStreetFront() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 4, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		BigStreetAnalyzer analyzer = new BigStreetAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testBigStreetTail() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(2, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		BigStreetAnalyzer analyzer = new BigStreetAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testNoBigStreet() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		BigStreetAnalyzer analyzer = new BigStreetAnalyzer();
		Assert.assertFalse(analyzer.analyze(wurf));
	}
}
