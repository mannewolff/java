package de.neusta.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import static de.neusta.common.controller.ControllerConstants.*;

@Controller
public class IndexController extends AbstractController {

	static Logger log = Logger.getLogger(HelloWorldController.class);

	@Override
	@RequestMapping("/index.do")
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// logging
		long time = System.currentTimeMillis(); 
		log.debug("Performing request mapping: /index.do.");

		ModelAndView model = new ModelAndView(index_page);

		// logging
		Long actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");
		
		return model;
	}
}