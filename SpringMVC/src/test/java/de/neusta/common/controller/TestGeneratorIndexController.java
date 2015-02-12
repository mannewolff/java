package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.GEN_INDEX_PAGE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

@RunWith(MockitoJUnitRunner.class)
public class TestGeneratorIndexController {

	@InjectMocks
	GeneratorIndexController generatorIndex;
	
	@Test
	public void testIndex() throws Exception {
		
		// preparation

		// execution
		final ModelAndView model = this.generatorIndex.handleIndex();

		// verification
		assertEquals(GEN_INDEX_PAGE, model.getViewName());

	}
	
}
