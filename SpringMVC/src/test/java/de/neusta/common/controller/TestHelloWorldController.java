package de.neusta.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
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
	UserDao userDao;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	User user;

	@Test
	public void testHelloWorldController() throws Exception {

		// test preparation
		Mockito.when(this.personService.getName()).thenReturn(
				"hello world autowired");

		// test execution
		final ModelAndView model = this.helloWorldController
				.handleRequestInternal(this.request, this.response);

		// test verification
		Mockito.verify(this.personService, Mockito.times(1)).getName();
		Mockito.verify(this.userDao).save(Matchers.any(User.class));

		// model verification
		Assert.assertEquals("hello world autowired", model.getModel()
				.get("msg"));
		Assert.assertEquals("welcome.do erstellt User", ((User) model
				.getModel().get("user")).getName());
	}
}
