package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.HELLO_WORLD_PAGE;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.user.UserService;

@Controller
public class HelloWorldController extends AbstractController {

	static Logger log = Logger.getLogger(HelloWorldController.class);

	@Resource
	UserDao userDao;

	@Resource
	UserService personService;

	private User addAUser() {
		final User user = new User();
		user.setName("welcome.do erstellt User");
		this.userDao.save(user);
		return user;
	}

	
	private ModelAndView getPersonenName() {
		final String message = this.personService.getName();
		final ModelAndView model = new ModelAndView(HELLO_WORLD_PAGE);
		model.addObject("msg", message);
		return model;
	}

	@Override
	@RequestMapping("/welcome")
	protected ModelAndView handleRequestInternal(
			final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {

		// logging
		final long time = System.currentTimeMillis();
		log.debug("Performing request mapping: /welcome");

		// performing
		final ModelAndView model = getPersonenName();
		final User user = addAUser();
		model.addObject("user", user);

		// logging
		final Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");

		return model;
	}

}