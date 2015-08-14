package de.mwolff.kniffel.strategy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class TestFullHouseStrategy {

	@Test
	public void testIsFullHouse() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 2, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		Board board = new Board();
		context.ActBoard = board;
		
		FullHouseStrategy strategy = new FullHouseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isFullHouse);
		assertTrue(context.isDreier);
	}

	@Test
	public void testIsNoFullHouse() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		FullHouseStrategy strategy = new FullHouseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isFullHouse);
	}

	@Test
	public void testSeventhFullHouse() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 2, 3, 3);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		FullHouseStrategy strategy = new FullHouseStrategy();
		
		Board board = new Board();
		context.ActBoard = board;
		
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		board.setOben(Constants.FULLHOUSE, 25, 0, true);
		
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isFullHouse);
		assertTrue(context.isDreier);
	}

}
