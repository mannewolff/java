package de.mwolff.kniffel.analyzer.kniffelanalyzer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;

public class TestLittleStreetAnalyzer {

	@Test
	public void testLitteStreetFront() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 4, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testLitteStreetMiddle() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(2, 3, 4, 5, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}
	
	@Test
	public void testLitteStreetTail() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		assertTrue(analyzer.analyze(wurf));
	}

	@Test
	public void testNoLittleStreet() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		Assert.assertFalse(analyzer.analyze(wurf));
	}

	@Test
	public void testGetBoolMatrixTail() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		Boolean[] bool = analyzer.getBoolMatrix(wurf);
		assertFalse(bool[0]);
		assertTrue(bool[1]);
		assertTrue(bool[2]);
		assertTrue(bool[3]);
		assertTrue(bool[4]);
	}

	@Test
	public void testGetBoolMatrixMiddle() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(2, 3, 4, 2, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		Boolean[] bool = analyzer.getBoolMatrix(wurf);
		assertTrue(bool[0]);
		assertTrue(bool[1]);
		assertTrue(bool[2]);
		assertFalse(bool[3]);
		assertTrue(bool[4]);
	}

	@Test
	public void testGetBoolMatrixHead() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(5, 6, 4, 3, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		LittleStreetAnalyzer analyzer = new LittleStreetAnalyzer();
		Boolean[] bool = analyzer.getBoolMatrix(wurf);
		assertTrue(bool[0]);
		assertTrue(bool[1]);
		assertTrue(bool[2]);
		assertTrue(bool[3]);
		assertFalse(bool[4]);
	}
}
