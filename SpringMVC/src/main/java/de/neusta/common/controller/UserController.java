package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.facades.UserFacade;
import de.neusta.persistence.entity.User;
import de.neusta.service.UserService;

@Controller
public class UserController extends AspectController {

	static Logger log = Logger.getLogger(UserController.class);

	@Resource
	UserService userService;

	@Resource
	UserFacade userFacade;

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
		User user = userFacade.createUser(id);
		model.addObject("User", user);

		// logging
		endMethod();

		return model;
	}

	@RequestMapping("/adduser")
	@Transactional
	protected ModelAndView addUser(@ModelAttribute User user) throws Exception {

		// logging
		beginMethod(log, "addUser @RequestMapping=/adduser.");
		log.debug("User ist " + user);

		// performing
		if (user == null) {
			log.error("User in scope is null.");
			return prepareUserDataInput(0l);
		}
		
		userFacade.saveOrUpdate(user);

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
		List<User> userlist = userFacade.getAllUser();
		model.addObject("Userlist", userlist);

		// logging
		endMethod();

		return model;

	}

	@RequestMapping("/deleteuser")
	@Transactional
	protected ModelAndView deleteUser(@RequestParam Long id) throws Exception {

		// logging
		beginMethod(log, "listUser @RequestMapping=/deleteuser.");

		// performing

		// logging
		endMethod();

		return listUser();
	}
}
