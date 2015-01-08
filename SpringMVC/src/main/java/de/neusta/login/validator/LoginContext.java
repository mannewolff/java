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

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public String getName() {
		return this.name;
	}

	public String getOriginalPasswd() {
		return this.originalPasswd;
	}

	public String getPasswd() {
		return this.passwd;
	}

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setOriginalPasswd(final String originalPasswd,
			final String method) {

		this.originalPasswd = originalPasswd;

		try {
			final long current = 12341234l;
			final byte[] now = new Long(current).toString().getBytes();
			final MessageDigest md = MessageDigest.getInstance(method);

			md.update(originalPasswd.getBytes());
			md.update(now);

			this.passwd = toHex(md.digest());
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("Illegial Algrotithm.");
		}
	}

	private String toHex(final byte[] buffer) {
		final StringBuffer sb = new StringBuffer(buffer.length * 2);

		for (final byte element : buffer) {
			sb.append(Character.forDigit((element & 0xf0) >> 4, 16));
			sb.append(Character.forDigit(element & 0x0f, 16));
		}

		return sb.toString();
	}

}
