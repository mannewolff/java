package de.neusta.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.person.PersonService;


@RunWith(MockitoJUnitRunner.class)
public class TestHelloWorldController {

	@InjectMocks
	HelloWorldController helloWorldController;

	@Mock
	PersonService personService;

	@Mock
	UserDao userDoa;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;
	
	@Mock
	User user;

	@Test
	public void testHelloWorldController() throws Exception {

		// test preparation
		Mockito.when(personService.getName()).thenReturn(
				"hello world autowired");

		// test execution
		ModelAndView model = helloWorldController.handleRequestInternal(
				request, response);

		// test verification
		Mockito.verify(personService, Mockito.times(1)).getName();
		Mockito.verify(userDoa).save(Mockito.any(User.class));
				
		// model verification
		Assert.assertEquals("hello world autowired", model.getModel().get("msg"));
		Assert.assertEquals("welcome.do erstellt User", ((User)model.getModel().get("user")).getName());
	}
}