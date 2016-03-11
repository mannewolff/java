package de.mwolff.kniffel.strategy;

import static org.junit.Assert.*;

import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestLittleStrasseStrategy {

	@Test
	public void testIsLittleStreetHead() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 4, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		LittleStrasseStrategy strategy = new LittleStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isLittleStreet);
		assertTrue(context.onceMore);
	}

	@Test
	public void testIsLittleStreetTail() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(1, 3, 4, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		LittleStrasseStrategy strategy = new LittleStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isLittleStreet);
		assertTrue(context.onceMore);
	}

	@Test
	public void testIsLittleStreetHeadMiddle() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 2, 3, 4, 5);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		LittleStrasseStrategy strategy = new LittleStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertFalse(result);
		assertTrue(context.isLittleStreet);
		assertTrue(context.onceMore);
		assertTrue(context.isZweier);
		assertTrue(context.isKuemmerChance);
	}

	@Test
	public void testIsNoKniffel() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(2, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		LittleStrasseStrategy strategy = new LittleStrasseStrategy();
		boolean result = strategy.executeAsChain(context);
		assertTrue(result);
		assertFalse(context.isLittleStreet);
		assertTrue(context.onceMore);
	}
}
