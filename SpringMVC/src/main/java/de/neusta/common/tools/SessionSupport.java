package de.neusta.common.tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionSupport {

	public static boolean validateSessionOnLogon(HttpServletRequest request) {
		boolean result = false;
		HttpSession session = request.getSession();
		if (session != null) {
			LoginSessionInformation loginInformation = (LoginSessionInformation) session
					.getAttribute("Login");
			if (loginInformation != null) {
				return true;
			}
		}
		return result;

	}
}
