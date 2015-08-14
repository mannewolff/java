package de.mwolff.kniffel.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import de.mwolff.kniffel.common.Cube;
import de.mwolff.kniffel.common.CubeTester;

public class TestCubeTester {

	@Test
	public void testCubeTester() throws Exception {

		CubeTester ct = new CubeTester();
		assertNotNull(ct);

		Cube[] cubeList = CubeTester.prepareCubeList(1, 2, 3, 4, 5);
		Cube cube = cubeList[0];
		assertEquals(1, cube.getValue());
		cube = cubeList[1];
		assertEquals(2, cube.getValue());
		cube = cubeList[2];
		assertEquals(3, cube.getValue());
		cube = cubeList[3];
		assertEquals(4, cube.getValue());
		cube = cubeList[4];
		assertEquals(5, cube.getValue());
	}
}
