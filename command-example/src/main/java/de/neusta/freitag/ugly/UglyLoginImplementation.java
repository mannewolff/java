package de.neusta.freitag.ugly;

public class UglyLoginImplementation {
	
	// Configuration
	private final int minimumCapital = 2;

	public boolean validateLogin(final String passwd) {
		
		boolean partResult = false;
		
		if (passwd.length() >= 12) {
			partResult = true;
		}
		
		if (partResult) {
			int capitalCount = 0;
			partResult = false;
			for (int i = 0; i < passwd.length(); i++) {
				char c = passwd.charAt(i);
				if (Character.isUpperCase(c)) {
					capitalCount++;
				}
			}
			if (capitalCount >= minimumCapital) {
				partResult = true;
			}
		}
		return partResult;
	}
}


