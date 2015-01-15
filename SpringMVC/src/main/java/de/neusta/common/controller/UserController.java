package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.entity.User;

@Controller
public class UserController {

	static Logger log = Logger.getLogger(UserController.class);

	/**
	 * Prepares the data for creating and updating a user.
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/preUserDataInput")
	protected ModelAndView prepareUserDataInput(@RequestParam Long id) throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /preUserDataInput");

		// performing
		final ModelAndView model = new ModelAndView(USER_INPUT_PAGE);
		User user = new User();
		if ((id == null) || (id == 0)) {
			// Get Userdata from Service
		} else {
			
		}
		model.addObject("User", user);

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return model;
	}

	@RequestMapping("/adduser")
	protected ModelAndView addUser(@ModelAttribute User user,
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /adduser");

		// performing
		// Saves or modifies user

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return new ModelAndView(USER_LIST);
	}

}