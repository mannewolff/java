package de.mwolff.kniffel.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Constants;
import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class TestKniffelStrategy {

	@Test
	public void testIsNoKniffel() throws Exception {

		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;

		KniffelStrategy strategy = new KniffelStrategy();
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isKniffel);
	}

	@Test
	public void testSiebteKniffel() {

		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;

		Board board = new Board();
		context.ActBoard = board;

		KniffelStrategy strategy = new KniffelStrategy();
		boolean result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		result = strategy.executeAsChain(context);
		// In der aktuellen Implementierung wird der 7. Kniffel
		// nicht aufgeschrieben.
		assertTrue(result);
		assertFalse(context.isKniffel);
	}

	@Test
	public void testKniffelAutomaticMarkedInBoard() throws Exception {

		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;

		Board board = new Board();
		context.ActBoard = board;
		KniffelStrategy strategy = new KniffelStrategy();
		boolean result = strategy.executeAsChain(context);

		int position = board.isFree(Constants.KNIFFEL);
		assertEquals(1, position);
		assertFalse(result);
		assertTrue(context.isKniffel);

		// Unten muss 50 sein
		Integer value = board.get(Constants.GESAMTUNTEN, position - 1);
		assertEquals(Integer.valueOf(50), value);
	}

}
