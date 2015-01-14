package de.neusta.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import de.neusta.persistence.entity.User;

@Controller
public class PreUserDataInputController extends AbstractController {

	static Logger log = Logger.getLogger(PreUserDataInputController.class);

	@Override
	@RequestMapping("/preUserDataInput")
	protected ModelAndView handleRequestInternal(
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /preUserDataInput.do.");

		// performing
		final ModelAndView model = new ModelAndView("UserDataInputPage.vm");
		User user = new User();
		user.setName("Default");
		model.addObject("User", user);

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return model;
	}

}