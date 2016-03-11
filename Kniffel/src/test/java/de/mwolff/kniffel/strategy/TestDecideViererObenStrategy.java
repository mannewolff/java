package de.mwolff.kniffel.strategy;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestDecideViererObenStrategy {

	@Test
	public void testIsViererViererPasch() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 4);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		DecideViererObenStrategy strategy = new DecideViererObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertTrue(result);
	}

	@Test
	public void testIsViererKeinViererPasch() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 1);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		DecideViererObenStrategy strategy = new DecideViererObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
	}

	@Test
	public void testReiheMussAusgeglichenWerden() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 1);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.DREI, 9, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		board.setOben(Constants.EINS, 1, 1, false);
		board.setOben(Constants.ZWEI, 6, 1, false);
		board.setOben(Constants.DREI, 9, 1, false);
		board.setOben(Constants.FUENF, 15, 1, false);
		
		DecideViererObenStrategy strategy = new DecideViererObenStrategy();
		boolean result = strategy.executeAsChain(context);
		
		int value = board.get(Constants.SECHS, 1);
		Assert.assertEquals(24, value);
		value = board.get(Constants.GESAMTOBEN, 1);
		Assert.assertEquals(55, value);
		value = board.get(Constants.TENDENZ, 1);
		Assert.assertEquals(4, value);
		Assert.assertFalse(result);
	}

	@Test
	public void testErsteFreieZeile() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 1);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.DREI, 9, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		board.setOben(Constants.EINS, 3, 1, false);
		board.setOben(Constants.ZWEI, 6, 1, false);
		board.setOben(Constants.DREI, 9, 1, false);
		board.setOben(Constants.FUENF, 15, 1, false);
		
		DecideViererObenStrategy strategy = new DecideViererObenStrategy();
		boolean result = strategy.executeAsChain(context);
		
		int value = board.get(Constants.SECHS, 0);
		Assert.assertEquals(24, value);
		value = board.get(Constants.GESAMTOBEN, 0);
		Assert.assertEquals(57, value);
		value = board.get(Constants.TENDENZ, 0);
		Assert.assertEquals(6, value);
		Assert.assertFalse(result);
	}

	@Test
	public void testisViererPasch() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 5);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		board.setOben(Constants.EINS, 3, 0, false);
		board.setOben(Constants.ZWEI, 6, 0, false);
		board.setOben(Constants.DREI, 9, 0, false);
		board.setOben(Constants.FUENF, 15, 0, false);
		board.setOben(Constants.EINS, 3, 1, false);
		board.setOben(Constants.ZWEI, 6, 1, false);
		board.setOben(Constants.DREI, 9, 1, false);
		board.setOben(Constants.FUENF, 15, 1, false);
		
		DecideViererObenStrategy strategy = new DecideViererObenStrategy();
		boolean result = strategy.executeAsChain(context);
		
		Assert.assertTrue(result);
	}

}
