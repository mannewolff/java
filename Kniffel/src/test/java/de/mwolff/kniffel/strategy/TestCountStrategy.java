package de.mwolff.kniffel.strategy;

import static org.junit.Assert.*;

import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;


public class TestCountStrategy {

	@Test
	public void testIsVierer() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 1, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		CountStrategy strategy = new CountStrategy();
		strategy.executeAsChain(context);
		assertTrue(context.isVierer);
		assertFalse(context.isDreier);
		assertFalse(context.isZweier);
		
	}

	@Test
	public void testIsDreier() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 5, 1, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		CountStrategy strategy = new CountStrategy();
		strategy.executeAsChain(context);
		assertFalse(context.isVierer);
		assertTrue(context.isDreier);
		assertFalse(context.isZweier);
		
	}

	@Test
	public void testIsZweier() throws Exception {
		
		KniffelContext context = new KniffelContext();
		Cube[] cubelist = CubeTester.prepareCubeList(6, 5, 4, 1, 1);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		context.ActWurf = wurf;
		context.AnzWurf = 1;
		
		CountStrategy strategy = new CountStrategy();
		strategy.executeAsChain(context);
		assertFalse(context.isVierer);
		assertFalse(context.isDreier);
		assertTrue(context.isZweier);
		
	}

}
