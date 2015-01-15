package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.entity.User;
import de.neusta.service.user.UserService;

@Controller
public class UserController {

	static Logger log = Logger.getLogger(UserController.class);
	
	@Resource
	UserService userService;

	/**
	 * Prepares the data for creating and updating a user.
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/preuser")
	protected ModelAndView prepareUserDataInput(@RequestParam Long id) throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /preuser.");

		// performing
		final ModelAndView model = new ModelAndView(USER_INPUT_PAGE);
		log.debug("Creating new User.");
		User user = new User();
		if ((id == null) || (id == 0)) {
			log.debug("Nothing else to do.");
			user.setName("");
			user.setPrename("");
			user.setLogin("");
			user.setPassword("");
		} else {
			log.debug("Fetching existing user from database.");
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
		if (user == null) {
			log.error("User in scope is null.");
			
		}
		log.debug("Saving user " + user.getPrename() + " " + user.getName());
		userService.saveUser(user); 

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return new ModelAndView(USER_LIST);
	}

}