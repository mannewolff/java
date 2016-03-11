package de.mwolff.kniffel.analyzer.kniffelanalyzer;


import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;


public class TestMaxCountAnalyzer {

	@Test
	public void testAnalyzeZweier() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 4, 2, 3);
		Assert.assertTrue(analyzer.Zweier == true);
		analyzer = createAnalyzer(1, 2, 2, 4, 3);
		Assert.assertTrue(analyzer.Zweier == true);
		analyzer = createAnalyzer(1, 2, 3, 3, 4);
		Assert.assertTrue(analyzer.Zweier == true);
		analyzer = createAnalyzer(1, 4, 4, 5, 6);
		Assert.assertTrue(analyzer.Zweier == true);
		analyzer = createAnalyzer(1, 5, 5, 2, 3);
		Assert.assertTrue(analyzer.Zweier == true);
		analyzer = createAnalyzer(1, 6, 6, 2, 3);
		Assert.assertTrue(analyzer.Zweier == true);
	}
	
	@Test
	public void testAnalyzeDreier() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 1, 2, 3);
		Assert.assertTrue(analyzer.Dreier == true);
		analyzer = createAnalyzer(1, 2, 2, 2, 3);
		Assert.assertTrue(analyzer.Dreier == true);
		analyzer = createAnalyzer(1, 2, 3, 3, 3);
		Assert.assertTrue(analyzer.Dreier == true);
		analyzer = createAnalyzer(1, 4, 4, 4, 6);
		Assert.assertTrue(analyzer.Dreier == true);
		analyzer = createAnalyzer(1, 5, 5, 5, 3);
		Assert.assertTrue(analyzer.Dreier == true);
		analyzer = createAnalyzer(1, 6, 6, 6, 3);
		Assert.assertTrue(analyzer.Dreier == true);
	}

	@Test
	public void testAnalyzeVierer() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 1, 1, 3);
		Assert.assertTrue(analyzer.Vierer == true);
		analyzer = createAnalyzer(1, 2, 2, 2, 2);
		Assert.assertTrue(analyzer.Vierer == true);
		analyzer = createAnalyzer(1, 3, 3, 3, 3);
		Assert.assertTrue(analyzer.Vierer == true);
		analyzer = createAnalyzer(4, 4, 4, 4, 6);
		Assert.assertTrue(analyzer.Vierer == true);
		analyzer = createAnalyzer(5, 5, 5, 5, 3);
		Assert.assertTrue(analyzer.Vierer == true);
		analyzer = createAnalyzer(6, 6, 6, 6, 3);
		Assert.assertTrue(analyzer.Vierer == true);
	}

	@Test
	public void testAnalyzeKniffel() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 1, 1, 1);
		Assert.assertTrue(analyzer.Kniffel == true);
		analyzer = createAnalyzer(2, 2, 2, 2, 2);
		Assert.assertTrue(analyzer.Kniffel == true);
		analyzer = createAnalyzer(3, 3, 3, 3, 3);
		Assert.assertTrue(analyzer.Kniffel == true);
		analyzer = createAnalyzer(4, 4, 4, 4, 4);
		Assert.assertTrue(analyzer.Kniffel == true);
		analyzer = createAnalyzer(5, 5, 5, 5, 5);
		Assert.assertTrue(analyzer.Kniffel == true);
		analyzer = createAnalyzer(6, 6, 6, 6, 6);
		Assert.assertTrue(analyzer.Kniffel == true);
	}

	private MaxCountAnalyzer createAnalyzer(final int a, final int b, final int c, final int d, final int e) {
		Cube[] cubelist = CubeTester.prepareCubeList(a, b, c, d, e);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		MaxCountAnalyzer analyzer = new MaxCountAnalyzer();
		analyzer.analyze(wurf);
		return analyzer;
	}

	@Test
	public void testGetColumnOfVierer() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 1, 1, 5);
		Integer column = analyzer.getColumnOfVierer();
		assertEquals(Constants.EINS, column);
	}

	@Test
	public void testGetColumnOfDreier() throws Exception {
		MaxCountAnalyzer analyzer = createAnalyzer(1, 1, 1, 4, 5);
		Integer column = analyzer.getColumnOfDreier();
		assertEquals(Constants.EINS, column);
	}
}
