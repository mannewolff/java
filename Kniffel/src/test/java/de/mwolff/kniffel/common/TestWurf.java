package de.mwolff.kniffel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class TestWurf {

	@Test
	public void testInitialWurf() throws Exception {
	
		Wurf wurf = new Wurf();
		Cube[] cubes = wurf.shuffle(Wurf.MAX_CUBE);
		assertTrue(cubes.length == Wurf.MAX_CUBE);
	}

	@Test
	public void testGetFirstCubeValue() throws Exception {
		Wurf wurf = new Wurf();
		wurf.shuffle(Wurf.MAX_CUBE);
		int value = wurf.getValue(0);
		assertTrue((value >= 1) && (value <= 6));
	}
	
	@Test
	public void testGetSet() throws Exception {
		Cube cube = new Cube();
		cube.setValue(5);
		Cube[] cubes = new Cube[1];
		cubes[0] = cube;
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubes);
		
		Cube[] cubes2 = wurf.getCubeList();
		assertEquals(5, cubes2[0].getValue());
		
	}
	
	@Test
	public void testSave() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 1, 1, 2, 3);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		
		wurf.save(Constants.EINS);
		Cube[] saveCubes = wurf.savecubes;
		int saved = wurf.saved;
		
		assertEquals(1, saveCubes[0].getValue());
		assertEquals(1, saveCubes[1].getValue());
		assertEquals(1, saveCubes[2].getValue());
		assertEquals(0, saveCubes[3].getValue());
		assertEquals(0, saveCubes[4].getValue());
		assertEquals(0, wurf.cubes[0].getValue());
		assertEquals(0, wurf.cubes[1].getValue());
		assertEquals(0, wurf.cubes[2].getValue());
		assertEquals(0, wurf.cubes[3].getValue());
		assertEquals(0, wurf.cubes[4].getValue());
		assertEquals(3, saved);
	}
	@Test
	public void testSaveCubes() throws Exception {
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		
		Boolean[] bsaved = {Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE};
		
		wurf.save(bsaved);
		Cube[] saveCubes = wurf.savecubes;
		int saved = wurf.saved;
		
		assertEquals(1, saveCubes[0].getValue());
		assertEquals(2, saveCubes[1].getValue());
		assertEquals(3, saveCubes[2].getValue());
		assertEquals(0, saveCubes[3].getValue());
		assertEquals(0, saveCubes[4].getValue());
		assertEquals(3, saved);
		assertEquals(0, wurf.cubes[0].getValue());
		assertEquals(0, wurf.cubes[1].getValue());
		assertEquals(0, wurf.cubes[2].getValue());
		assertEquals(0, wurf.cubes[3].getValue());
		assertEquals(0, wurf.cubes[4].getValue());
	}

	@Test
	public void testShuffleSaves() throws Exception {
		
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		
		Boolean[] bsaved = {Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE};
		
		wurf.save(bsaved);

		wurf.shuffleSaved();
		
		assertEquals(1, wurf.cubes[0].getValue());
		assertEquals(2, wurf.cubes[1].getValue());
		assertEquals(3, wurf.cubes[2].getValue());
		assertTrue(wurf.cubes[3].getValue() > 0);
		assertTrue(wurf.cubes[4].getValue() > 0);
		
		
	}

	@Test
	public void testGetValues() throws Exception {
		
		Cube[] cubelist = CubeTester.prepareCubeList(1, 2, 3, 5, 6);
		Wurf wurf = new Wurf();
		wurf.setCubeList(cubelist);
		int values = wurf.getValues();
		assertEquals(17, values);
	}

}
