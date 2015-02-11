package de.neusta.common.tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionSupport {

	public static boolean validateSessionOnLogon(
			final HttpServletRequest request) {
		final boolean result = false;
		final HttpSession session = request.getSession();
		if (session != null) {
			final LoginSessionInformation loginInformation = (LoginSessionInformation) session
					.getAttribute("Login");
			if (loginInformation != null) {
				return true;
			}
		}
		return result;

	}
}
