package de.neusta.common.controller;

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
import de.neusta.service.person.PersonService;

@Controller
public class HelloWorldController extends AbstractController {

	static Logger log = Logger.getLogger(HelloWorldController.class);

	@Resource
	UserDao userDao;

	@Resource
	PersonService personService;


	@Override
	@RequestMapping("/welcome.do")
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// logging
		long time = System.currentTimeMillis(); 
		log.debug("Performing request mapping: /welcome.do.");
		
		// performing
		ModelAndView model = getPersonenName();
		User user = addAUser();
		model.addObject("user", user);

		// logging
		Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");
		
		return model;
	}

	private User addAUser() {
		User user = new User();
		user.setName("welcome.do erstellt User");
		userDao.save(user);
		return user;
	}

	private ModelAndView getPersonenName() {
		String message = personService.getName();
		ModelAndView model = new ModelAndView("HelloWorldPage");
		model.addObject("msg", message);
		return model;
	}

}