package de.mwolff.kniffel.common;

/**
 * Method for preparing a 'Kniffle Wurf' means a sequence of 5 cubes.
 *
 */
public class CubeTester {

	static public Cube[] prepareCubeList(final int value1,
			final int value2, final int value3, final int value4,
			final int value5) {
		
		Cube cube1 = new Cube();
		Cube cube2 = new Cube();
		Cube cube3 = new Cube();
		Cube cube4 = new Cube();
		Cube cube5 = new Cube();

		cube1.setValue(value1);
		cube2.setValue(value2);
		cube3.setValue(value3);
		cube4.setValue(value4);
		cube5.setValue(value5);

		Cube[] cubelist = new Cube[Wurf.MAX_CUBE];
		cubelist[0] = cube1;
		cubelist[1] = cube2;
		cubelist[2] = cube3;
		cubelist[3] = cube4;
		cubelist[4] = cube5;
		return cubelist;
	}

}
