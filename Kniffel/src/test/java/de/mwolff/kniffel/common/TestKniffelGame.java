package de.mwolff.kniffel.common;

import static org.junit.Assert.*;

import org.junit.Test;

import de.mwolff.commons.command.CommandContainer;
import de.mwolff.kniffel.context.KniffelContext;


public class TestKniffelGame {

	@Test
	public void testKniffelImErsten() throws Exception {
		
		KniffelGame kniffelGame = new KniffelGame();
		KniffelContext context = new KniffelContext();
		CommandContainer<KniffelContext> container = kniffelGame.getCommandContainer();
		Wurf wurf = new Wurf();

		Board board = new Board();
		context.ActBoard = board;
		
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 2, 2, 2);
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		boolean result = container.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isKniffel);
	}

	@Test
	public void testGrosseStrassseImErsten() throws Exception {
		
		KniffelGame kniffelGame = new KniffelGame();
		KniffelContext context = new KniffelContext();
		CommandContainer<KniffelContext> container = kniffelGame.getCommandContainer();
		Wurf wurf = new Wurf();

		Board board = new Board();
		context.ActBoard = board;
		
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 4, 5);
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		boolean result = container.executeAsChain(context);
		assertFalse(result);
		assertFalse(context.isKniffel);
		assertTrue(context.isBigStreet);
	}
	
	@Test
	public void testKleineStrassseImErsten() throws Exception {
		
		KniffelGame kniffelGame = new KniffelGame();
		KniffelContext context = new KniffelContext();
		CommandContainer<KniffelContext> container = kniffelGame.getCommandContainer();
		Wurf wurf = new Wurf();

		Board board = new Board();
		context.ActBoard = board;
		
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 3, 4, 5);
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		boolean result = container.executeAsChain(context);
		assertFalse(result);
		assertFalse(context.isKniffel);
		assertFalse(context.isBigStreet);
		assertTrue(context.isLittleStreet);
		assertTrue(context.isZweier);
	}

	@Test
	public void testFullHouseImErsten() throws Exception {
		
		KniffelGame kniffelGame = new KniffelGame();
		KniffelContext context = new KniffelContext();
		CommandContainer<KniffelContext> container = kniffelGame.getCommandContainer();
		Wurf wurf = new Wurf();

		Board board = new Board();
		context.ActBoard = board;
		
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 3, 3, 3);
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		boolean result = container.executeAsChain(context);
		assertFalse(result);
		assertFalse(context.isKniffel);
		assertFalse(context.isBigStreet);
		assertFalse(context.isLittleStreet);
		assertTrue(context.isZweier);
		assertTrue(context.isDreier);
		assertTrue(context.isFullHouse);
	}
}
