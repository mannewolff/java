package de.neusta.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.entity.User;

@Controller
public class AddUser {//extends AbstractController {

	static Logger log = Logger.getLogger(AddUser.class);

	@RequestMapping("/adduser")
	protected ModelAndView handleRequestInternal(
			@ModelAttribute User user,
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /preUserDataInput.do.");

		// performing
		log.debug("Name in scope is: " + user.getName());

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return new ModelAndView("index.vm");
	}

}