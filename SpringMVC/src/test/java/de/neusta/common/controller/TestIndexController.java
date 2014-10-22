package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.INDEX_PAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.common.tools.LoginSessionInformation;

@RunWith(MockitoJUnitRunner.class)
public class TestIndexController {

	@InjectMocks
	IndexController indexController;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;
	
	@Mock
	HttpSession session;

	@Test
	public void testControllerConstants() throws Exception {
		ControllerConstants controllerConstants = new ControllerConstants();
		assertEquals("index", controllerConstants.INDEX_PAGE);
	}

	@Test
	public void testRedirection() throws Exception {
		
		Mockito.when(request.getSession()).thenReturn(session);
		Mockito.when(session.getAttribute("Login")).thenReturn(new LoginSessionInformation());
		
		// test execution
		assertNotNull(indexController);
		ModelAndView model = indexController.handleRequestInternal(
				request, response);
		assertEquals(INDEX_PAGE, model.getViewName());
		
	}
	@Test
	public void testNoUser() throws Exception {
		
		Mockito.when(request.getSession()).thenReturn(session);
		Mockito.when(session.getAttribute("Login")).thenReturn(null);
		
		// test execution
		assertNotNull(indexController);
		ModelAndView model = indexController.handleRequestInternal(
				request, response);
		assertEquals(INDEX_PAGE, model.getViewName());
		
	}

}
