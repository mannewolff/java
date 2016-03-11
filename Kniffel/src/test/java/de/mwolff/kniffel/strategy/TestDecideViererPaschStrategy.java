package de.mwolff.kniffel.strategy;

import org.junit.Assert;
import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestDecideViererPaschStrategy {

	@Test
	public void testIsViererPaschSimple() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 6, 6, 6, 4);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		DecideViererPaschStrategy strategy = new DecideViererPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
		int value = board.get(Constants.SECHS, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.VIERER, 0);
		Assert.assertEquals(28, value);
	}

	@Test
	public void testIsViererPaschSimple2() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(4, 4, 4, 6, 4);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		DecideViererPaschStrategy strategy = new DecideViererPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
		int value = board.get(Constants.SECHS, 0);
		Assert.assertEquals(0, value);
		value = board.get(Constants.VIERER, 0);
		Assert.assertEquals(22, value);
	}

	@Test
	public void testIsViererPaschSimple3() throws Exception {
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(4, 4, 4, 3, 4);
		Board board = new Board();
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		context.ActBoard = board;
		context.isVierer = true;
		
		DecideViererPaschStrategy strategy = new DecideViererPaschStrategy();
		boolean result = strategy.executeAsChain(context);
		Assert.assertFalse(result);
		int value = board.get(Constants.VIER, 0);
		Assert.assertEquals(16, value);
		value = board.get(Constants.VIERER, 0);
		Assert.assertEquals(0, value);
	}

}
