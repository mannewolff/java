package de.mwolff.kniffel.strategy;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestDecideDreierObenStrategy {

	@Test
	public void testIsDreier() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(4, 4, 4, 2, 5);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		
		DecideDreierObenStrategy strategy = new DecideDreierObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
		int value = board.get(Constants.VIER, 0);
		Assert.assertEquals(12, value);
	}
	
	@Test
	public void testIsNoDreierFullHouse1() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 1, 2, 2);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = true;
		
		DecideDreierObenStrategy strategy = new DecideDreierObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertTrue(result);
		int value = board.get(Constants.EINS, 0);
		Assert.assertEquals(0, value);
	}

	@Test
	public void testIsNoDreierFullHouse2() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 2, 1, 1);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = true;
		
		DecideDreierObenStrategy strategy = new DecideDreierObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertTrue(result);
	}

	@Test
	public void testIsDreieAlthoughFullHouse() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(3, 3, 3, 1, 1);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isDreier = true;
		context.isFullHouse = true;
		
		DecideDreierObenStrategy strategy = new DecideDreierObenStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
	}

}
