package de.mwolff.kniffel.strategy;

import static org.junit.Assert.*;

import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestBigStrasseStrategy {

	@Test
	public void testIsBigStreetHead() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 4, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		Board board = new Board();
		context.ActBoard = board;
		BigStrasseStrategy strategy = new BigStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isBigStreet);
	}

	@Test
	public void testIsBigStreetTail() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		Board board = new Board();
		context.ActBoard = board;
		
		BigStrasseStrategy strategy = new BigStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isBigStreet);
	}

	@Test
	public void testIsNoBigStreet() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		BigStrasseStrategy strategy = new BigStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isBigStreet);
	}

	@Test
	public void testSeventhBigStreet() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		Board board = new Board();
		
		BigStrasseStrategy strategy = new BigStrasseStrategy();
		
		board.setOben(Constants.BIG, 40, 0, false);
		board.setOben(Constants.BIG, 40, 1, false);
		board.setOben(Constants.BIG, 40, 2, false);
		board.setOben(Constants.BIG, 40, 3, false);
		board.setOben(Constants.BIG, 40, 4, false);
		board.setOben(Constants.BIG, 40, 5, false);
		
		context.ActBoard = board;
		
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isBigStreet);
		
	}
}
