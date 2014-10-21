package de.neusta.common.controller;

import static org.junit.Assert.*;
import static de.neusta.common.controller.ControllerConstants.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

@RunWith(MockitoJUnitRunner.class)
public class TestIndexController {

	@InjectMocks
	IndexController indexController;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;
	
	@Test
	public void testControllerConstants() throws Exception {
		ControllerConstants controllerConstants = new ControllerConstants();
		assertEquals("index", controllerConstants.index_page);
	}

	@Test
	public void testRedirection() throws Exception {
		
		// test execution
		assertNotNull(indexController);
		ModelAndView model = indexController.handleRequestInternal(
				request, response);
		assertEquals(index_page, model.getViewName());
		
	}
}
