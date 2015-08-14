package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;

public class TestFullHouseAnalyzer {


	@Test
	public void testFullHouse1() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 1, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse2() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 2, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	
	@Test
	public void testFullHouse3() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(3, 3, 3, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse4() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(4, 4, 4, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}
	
	@Test
	public void testFullHouse5() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(5, 5, 5, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse6() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse7() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse8() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 3, 3);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testFullHouse9() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 4, 4);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testNotFullHouse1() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 0, 2, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertFalse(analyzer.analyze(wurf));
	}

	@Test
	public void testNotFullHouse2() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 3, 1, 2);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		FullHouseAnalyzer analyzer = new FullHouseAnalyzer();
		assertFalse(analyzer.analyze(wurf));
	}

}
