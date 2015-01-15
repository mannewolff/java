package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

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

import de.neusta.persistence.entity.User;
import de.neusta.service.user.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TestUserController {

	@InjectMocks
	UserController userController;
	
	@Mock
	UserService userService;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;
	
	@Mock
	User user;

	@Mock
	HttpSession session;

	@Test
	public void testaddUser() throws Exception {

		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		
		// execution
		final ModelAndView model = this.userController.addUser(user,
				this.request, this.response);
		
		// verifying
		//Mockito.verify(this.userService, Mockito.times(1)).saveUser(user);
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
		
		final ModelAndView model = this.userController.prepareUserDataInput(0l);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("", user.getName());
		assertEquals("", user.getPrename());

	}
	
	@Test
	public void testListUser() throws Exception {

		final ModelAndView model = this.userController.listUser();
		assertEquals(USER_LIST, model.getViewName());
		List<User> userlist = (List<User>) model.getModel().get("userlist");
		//assertEquals(true, userlist.size() > 0);
		
	}
}
