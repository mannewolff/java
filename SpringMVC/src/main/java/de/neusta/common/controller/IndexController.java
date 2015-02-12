package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.INDEX_PAGE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.common.tools.LoginSessionInformation;
import de.neusta.common.tools.SessionSupport;

@Controller
public class IndexController extends AspectController {

	static Logger log = Logger.getLogger(IndexController.class);

	@RequestMapping("/index")
	protected ModelAndView handleIndex(
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		beginMethod(log, "prepareUserDataInput @RequestMapping=/index.");

		validateLogin(request);

		final ModelAndView model = new ModelAndView(INDEX_PAGE);

		// logging
		endMethod();

		return model;
	}

	private void validateLogin(final HttpServletRequest request) {

		final boolean result = SessionSupport.validateSessionOnLogon(request);
		if (result) {
			if (log.isDebugEnabled()) {
				log.debug("Login in session is set.");
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No login in session is set.");
			}
			request.getSession().setAttribute("Login",
					new LoginSessionInformation());
		}
	}
}