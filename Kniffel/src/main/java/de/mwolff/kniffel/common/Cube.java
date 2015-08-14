/**
 * Kniffel 1.0
 */
package de.mwolff.kniffel.common;

/**
 * @author Manfred Wolff
 * R: Emulates the behavior of a cube.
 */
public class Cube {
	
	int value;

	public int shuffle() {
		int result = 0;
		while(result == 0)
			result = (int) (Math.random()*7);
        value = result;
		return value; 
	}

	public int getValue() {
		return value;
	}
	
	// Only for test purpose
	void setValue(final int value) {
		this.value = value;
	}

}
