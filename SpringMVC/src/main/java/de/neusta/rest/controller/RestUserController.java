package de.neusta.rest.controller;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RestUserController {

	static Logger log = Logger.getLogger(RestUserController.class);
	private static final String INVALID = "-1";

	@RequestMapping(value="/rest/{uid}")
	protected String prepareUserDataInput(@PathVariable("uid") final String uid) {

		
		// performing
		log.debug("REST --> ");
		//log.debug("REST --> " + uid);
		//log.debug("REST --> " + pwd);

		// logging

		return null;
	}

}