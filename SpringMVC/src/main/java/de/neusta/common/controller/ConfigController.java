package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.CONFIG_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.CONFIG_LIST;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.facades.ConfigFacade;
import de.neusta.persistence.entity.Config;
import de.neusta.persistence.entity.User;

@Controller
public class ConfigController extends AspectController {

	static Logger log = Logger.getLogger(ConfigController.class);

	@Resource
	ConfigFacade configFacade;

	/**
	 * Prepares the data for creating and updating a user.
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/preconfig")
	protected ModelAndView prepareConfigDataInput(@RequestParam Long id)
			throws Exception {

		// logging
		beginMethod(log, "prepareUserDataInput @RequestMapping=/preuser.");

		// performing
		final ModelAndView model = new ModelAndView(CONFIG_INPUT_PAGE);
		//User user = userFacade.createUser(id);
		//model.addObject("User", user);

		// logging
		endMethod();

		return model;
	}

	@RequestMapping("/addconfig")
	@Transactional
	protected ModelAndView addConfig(@ModelAttribute User user) throws Exception {

		// logging
		beginMethod(log, "addUser @RequestMapping=/adduser.");
		log.debug("User ist " + user);

		// performing
		/*
		if (user == null) {
			log.error("User in scope is null.");
			return prepareUserDataInput(0l);
		}
		
		userFacade.saveOrUpdate(user);
		*/

		// logging
		endMethod();

		return listConfigs();
	}

	@RequestMapping("/listconfigs")
	@Transactional
	protected ModelAndView listConfigs() throws Exception {

		// logging
		beginMethod(log, "listUser @RequestMapping=/listuser.");

		// performing
		
		final ModelAndView model = new ModelAndView(CONFIG_LIST);

		List<Config> configlist = configFacade.getAllConfig();
		model.addObject("Configlist", configlist);

		// logging
		endMethod();

		return model;

	}

	@RequestMapping("/deleteconfig")
	@Transactional
	protected ModelAndView deleteUser(@RequestParam Long id) throws Exception {

		// logging
		beginMethod(log, "listUser @RequestMapping=/deleteuser.");

		// performing

		// logging
		endMethod();

		return listConfigs();
	}
}
