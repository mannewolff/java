package de.neusta.freitag.ugly;

import static org.junit.Assert.*;

import org.junit.Test;


public class TestUglyLoginImplementation {
	
	@Test
	public void lengthOK() throws Exception {
		String input = "einLangesPasswort";
		UglyLoginImplementation ugly = new UglyLoginImplementation();
		assertTrue(ugly.validateLogin(input));
	}
	
	@Test
	public void lenthNotOK() throws Exception {
		String input = "einkurzsPw";
		UglyLoginImplementation ugly = new UglyLoginImplementation();
		assertFalse(ugly.validateLogin(input));
	}
	
	@Test
	public void noOneCapitalLetter() throws Exception {
		String input = "einlangespasswort";
		UglyLoginImplementation ugly = new UglyLoginImplementation();
		assertFalse(ugly.validateLogin(input));
	}
	
    @Test
	public void noMinimumXCapitalLetters() throws Exception {
		String input = "einlangesPasswort";
		UglyLoginImplementation ugly = new UglyLoginImplementation();
		assertFalse(ugly.validateLogin(input));
	}

}
