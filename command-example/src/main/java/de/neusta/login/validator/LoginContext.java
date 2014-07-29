package de.neusta.login.validator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.mwolff.commons.command.Context;
import de.mwolff.commons.command.DefaultContext;

public class LoginContext extends DefaultContext implements Context {

	private String name;
	private String passwd;
	private String originalPasswd;
	private String errorMessage;

	public String getName() {
		return name;
	}

	public String getOriginalPasswd() {
		return originalPasswd;
	}

	public void setOriginalPasswd(String originalPasswd) {
		
		this.originalPasswd = originalPasswd;
		
		try {
            long current = 12341234l;
			byte[] now = new Long(current).toString().getBytes();
			MessageDigest md = MessageDigest.getInstance("MD5");

			md.update(originalPasswd.getBytes());
			md.update(now);

			this.passwd = toHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			//
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswd() {
		return this.passwd;
	}

	private String toHex(byte[] buffer) {
		StringBuffer sb = new StringBuffer(buffer.length * 2);

		for (int i = 0; i < buffer.length; i++) {
			sb.append(Character.forDigit((buffer[i] & 0xf0) >> 4, 16));
			sb.append(Character.forDigit(buffer[i] & 0x0f, 16));
		}

		return sb.toString();
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
