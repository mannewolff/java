package de.mwolff.kniffel.common;

public class Wurf {

	public static int MAX_CUBE = 5;
	Cube[] cubes = new Cube[MAX_CUBE];
	Cube[] savecubes = new Cube[MAX_CUBE];
	int saved;

	{
		for (int i = 0; i < MAX_CUBE; i++) {
			Cube cube = new Cube();
			cubes[i] = cube;
		}

		for (int i = 0; i < MAX_CUBE; i++) {
			Cube cube = new Cube();
			savecubes[i] = cube;
		}
	}

	public void save(final Integer integer) {

		int counter = 0;
		saved = 0;
		for (Cube cube : savecubes) {
			cube.setValue(0);
		}
		for (Cube cube : cubes) {
			if (cube.getValue() == integer.intValue()) {
				savecubes[counter].setValue(integer.intValue());
				saved++;
			}
			counter++;
		}

		for (int i = 0; i < MAX_CUBE; i++) {
			cubes[i].setValue(0);
		}
	}

	public void save(final Boolean[] bool) {

		saved = 0;

		for (Cube cube : savecubes) {
			cube.setValue(0);
		}

		for (int i = 0; i < MAX_CUBE; i++) {
			if (bool[i]) {
				savecubes[i].setValue(cubes[i].getValue());
				saved++;
			}
		}

		for (int i = 0; i < MAX_CUBE; i++) {
			cubes[i].setValue(0);
		}
	}

	public Cube[] shuffle(final int anz) {
		for (int i = 0; i < anz; i++) {
			cubes[i].shuffle();
		}
		return cubes;
	}

	public int getValue(int cube) {
		return cubes[cube].getValue();
	}
	
	public int getValues() {
		int values = 0;
		for (Cube cube : cubes) {
			values += cube.getValue();
		}
		return values;
	}

	public Cube[] getCubeList() {
		return cubes;
	}

	public void setCubeList(final Cube[] cubes) {
		this.cubes = cubes;
	}

	public void shuffleSaved() {

		int i = 0;
		for (Cube cube : savecubes) {
			if (cube.getValue() == 0) {
				cube.shuffle();
			}
			cubes[i] = savecubes[i];
			i++;
		}
	}
}
