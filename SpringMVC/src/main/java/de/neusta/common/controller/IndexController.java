package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.INDEX_PAGE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import de.neusta.common.tools.LoginSessionInformation;
import de.neusta.common.tools.SessionSupport;

@Controller
public class IndexController extends AbstractController {

	static Logger log = Logger.getLogger(IndexController.class);

	@Override
	@RequestMapping("/index")
	protected ModelAndView handleRequestInternal(
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Performing request mapping: /index.do.");
		}

		validateLogin(request);

		final ModelAndView model = new ModelAndView(INDEX_PAGE);

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		if (log.isDebugEnabled()) {
			log.debug("Operation took " + actTime.toString() + " milliseconds");
		}

		return model;
	}

	public void validateLogin(final HttpServletRequest request) {

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