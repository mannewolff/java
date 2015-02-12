package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.GEN_INDEX_PAGE;

import org.apache.log4j.Logger;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class GeneratorIndexController extends AspectController{
	
	static Logger log = Logger.getLogger(GeneratorIndexController.class);
	
	
	@RequestMapping("/generatorindex")
	protected ModelAndView handleIndex() throws Exception {

		// logging
		beginMethod(log, "prepareUserDataInput @RequestMapping=/index.");
		
		// processing
		final ModelAndView model = new ModelAndView(GEN_INDEX_PAGE);
		
		// logging
		endMethod();

		return model;
		
	}
}
