package de.mwolff.kniffel.common;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.mwolff.kniffel.analyzer.kniffelanalyzer.BigStreetAnalyzer;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.FullHouseAnalyzer;
import de.mwolff.kniffel.analyzer.kniffelanalyzer.LittleStreetAnalyzer;

public class TestCube {

	private Cube wuerfel;
	
	@Before
	public void setUp() {
		wuerfel = new Cube();
	}

	@Test
	public void testWuerfelZahl() throws Exception {
		int result = wuerfel.shuffle();
		assertTrue(result >= 1);
		assertTrue(result <= 6);
	}

	@Test
	public void testGetValue() throws Exception {
		int result = wuerfel.shuffle();
		assertTrue(result == wuerfel.getValue());
	}

	@Test
	public void testSetValue() throws Exception {
		wuerfel.setValue(6);
		assertTrue(6 == wuerfel.getValue());
	}

	@Test
	@Ignore
	public void testMillionFullHouse() throws Exception {
		
		int fullhouse = 0;
		
		for (long i = 0; i < 1000000; i++) {

			Wurf wurf = new Wurf();
			wurf.shuffle(Wurf.MAX_CUBE);
			FullHouseAnalyzer fha = new FullHouseAnalyzer();
			if (fha.analyze(wurf)) fullhouse++;
		}
		
	    System.out.println(fullhouse);
	    System.out.println("Full House: " + new Float(fullhouse*100/1000000).toString() + " %");
	}
	
	@Test
	@Ignore
	public void testMillionBigStreet() throws Exception {
		
		int bigStreet = 0;
		
		for (long i = 0; i < 1000000; i++) {

			Wurf wurf = new Wurf();
			wurf.shuffle(Wurf.MAX_CUBE);
			BigStreetAnalyzer bsa = new BigStreetAnalyzer();
			if (bsa.analyze(wurf)) bigStreet++;
		}
		
	    System.out.println(bigStreet);
	    System.out.println("Grosse Strassen: " + new Float(bigStreet*100/1000000).toString() + " %");
	}

	@Test
	@Ignore
	public void testMillionLittleStreet() throws Exception {
		
		int littleStreet = 0;
		
		for (long i = 0; i < 1000000; i++) {

			Wurf wurf = new Wurf();
			wurf.shuffle(Wurf.MAX_CUBE);
			LittleStreetAnalyzer bsa = new LittleStreetAnalyzer();
			if (bsa.analyze(wurf)) littleStreet++;
		}
		
	    System.out.println(littleStreet);
	    System.out.println("Kleine Strassen: " + new Float(littleStreet*100/1000000).toString() + " %");
	}

	@Test
	@Ignore
	public void testMillionCube() throws Exception {

		int anz1 = 0, anz2 = 0, anz3 = 0, anz4 = 0, anz5 = 0, anz6 = 0;

		for (long i = 0; i < 1000000; i++) {
			Cube wuerfel = new Cube();
			int result = wuerfel.shuffle();
			if (result == 1)
				anz1++;
			if (result == 2)
				anz2++;
			if (result == 3)
				anz3++;
			if (result == 4)
				anz4++;
			if (result == 5)
				anz5++;
			if (result == 6)
				anz6++;

		}
		// Bei 1.000.000 Wuerfen sollte das Ergebnis einigermassen gleich verteilt
		// sein
		assertTrue((anz1 > 160000) && (anz1 < 170000));
		assertTrue((anz2 > 160000) && (anz2 < 170000));
		assertTrue((anz3 > 160000) && (anz3 < 170000));
		assertTrue((anz4 > 160000) && (anz4 < 170000));
		assertTrue((anz5 > 160000) && (anz5 < 170000));
		assertTrue((anz6 > 160000) && (anz6 < 170000));
	}
}
