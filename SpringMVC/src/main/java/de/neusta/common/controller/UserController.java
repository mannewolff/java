package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.entity.User;
import de.neusta.service.user.UserService;

@Controller
public class UserController extends AspectController {

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
	protected ModelAndView prepareUserDataInput(@RequestParam Long id)
			throws Exception {

		// logging
		beginMethod(log, "prepareUserDataInput @RequestMapping=/preuser.");

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
			user = userService.getUser(id);
		}
		model.addObject("User", user);

		// logging
		endMethod();

		return model;
	}

	@RequestMapping("/adduser")
	@Transactional
	protected ModelAndView addUser(@ModelAttribute User user,
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		beginMethod(log, "addUser @RequestMapping=/adduser.");

		// performing
		if (user == null) {
			log.error("User in scope is null.");
		}
	
		if ("".equals(user.getId())) {
			log.debug("Saving user " + user.getId() + " " + user.getPrename()
					+ " " + user.getName());
			userService.saveUser(user);
		} else {
			log.debug("Merging user " + user.getId() + " " + user.getPrename()
					+ " " + user.getName());
			userService.mergeUser(user);
		}

		// logging
		endMethod();

		return listUser();
	}

	@RequestMapping("/listuser")
	@Transactional
	protected ModelAndView listUser() throws Exception {

		// logging
		beginMethod(log, "listUser @RequestMapping=/listuser.");

		// performing
		final ModelAndView model = new ModelAndView(USER_LIST);
		List<User> userlist = userService.getUserList();
		model.addObject("Userlist", userlist);

		// logging
		endMethod();

		return model;

	}

}