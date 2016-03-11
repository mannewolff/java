package de.mwolff.kniffel.strategy;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestDecideDreierPaschStrategy {

	// Groesser 26
	@Test
	public void testGreaterThan26() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 5, 5);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = true;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		
		DecideDreierPaschStrategy strategy = new DecideDreierPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		
		Assert.assertFalse(result);
		int value = board.get(Constants.DREI, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.FULLHOUSE, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.DREIER, 0);
		Assert.assertEquals(28, value);
		
	}

	// Kleiner 26 aber keine DREI mehr frei
	@Test
	public void testLessThan26ButNo3Free() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 1, 2);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = false;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		board.setOben(Constants.SECHS, 18, 0, true);
		board.setOben(Constants.SECHS, 18, 0, true);
		board.setOben(Constants.SECHS, 18, 0, true);
		board.setOben(Constants.SECHS, 18, 0, true);
		board.setOben(Constants.SECHS, 18, 0, true);
		board.setOben(Constants.SECHS, 18, 0, true);
		
		DecideDreierPaschStrategy strategy = new DecideDreierPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		
		Assert.assertFalse(result);
		int value = board.get(Constants.SECHS, 5);
		Assert.assertEquals(18, value);
		value = board.get(Constants.FULLHOUSE, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.DREIER, 0);
		Assert.assertEquals(21, value);
		
	}
	
	// Grosser 26 aber kein Dreierpasch mehr frei
	@Test
	public void testGreaterThan26ButNoDreierFree() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 5, 4);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = false;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		board.setNextUnten(context, Constants.DREIER, 24);
		board.setNextUnten(context, Constants.DREIER, 24);
		board.setNextUnten(context, Constants.DREIER, 24);
		board.setNextUnten(context, Constants.DREIER, 24);
		board.setNextUnten(context, Constants.DREIER, 24);
		board.setNextUnten(context, Constants.DREIER, 24);
		
		DecideDreierPaschStrategy strategy = new DecideDreierPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		
		Assert.assertFalse(result);
		int value = board.get(Constants.SECHS, 0);
		Assert.assertEquals(18, value);
		value = board.get(Constants.FULLHOUSE, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.DREIER, 0);
		Assert.assertEquals(24, value);
		
	}
	
	// Ist ein kleines Fullhouse
	
	// Kein DREI und kein Pasch mehr frei und ist auch kein Fullhouse
	
}
