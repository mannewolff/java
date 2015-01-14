package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;
import static org.junit.Assert.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.persistence.entity.User;

@RunWith(MockitoJUnitRunner.class)
public class TestUserController {

	@InjectMocks
	UserController userController;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	HttpSession session;

	@Mock
	User user;

	@Test
	public void testaddUser() throws Exception {

		final ModelAndView model = this.userController.addUser(new User(),
				this.request, this.response);
		assertEquals(USER_LIST, model.getViewName());
	}

	@SuppressWarnings("static-access")
	@Test
	public void testControllerConstant() throws Exception {
		ControllerConstants constants = new ControllerConstants();
		assertEquals("userdatainputpage.vm", constants.USER_INPUT_PAGE);
	}

	@Test
	public void testEmptyUserIsCreated() throws Exception {
		
		final ModelAndView model = this.userController.prepareUserDataInput(null);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);

	}
}
