package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.framework.rules.MockRule;
import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.user.UserService;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestUserController {


	@Rule
	public TestRule mockRule = new MockRule(this);
	
	@InjectMocks
	UserController userController;
	
	@Mock
	UserService userService;

	@Mock
	UserDao userDao;

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
		Mockito.when(this.user.getId()).thenReturn(1l);
		
		// execution
		final ModelAndView model = this.userController.addUser(user,
				this.request, this.response);
		
		// verifying
		//Mockito.verify(this.userService, Mockito.times(1)).saveUser(user);
		assertEquals(USER_LIST, model.getViewName());
	}

	@Test
	public void testaddNullUser() throws Exception {

		// execution
		final ModelAndView model = this.userController.addUser(null,
				this.request, this.response);
		
		// verifying
		assertEquals(null, model);
	}

	@Test
	public void testadd0User() throws Exception {

		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		Mockito.when(this.user.getId()).thenReturn(0l);
		
		// execution
		final ModelAndView model = this.userController.addUser(user,
				this.request, this.response);
		
		// verifying
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
		
		ModelAndView model = this.userController.prepareUserDataInput(0l);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("", user.getName());
		assertEquals("", user.getPrename());

		model = this.userController.prepareUserDataInput(null);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("", user.getName());
		assertEquals("", user.getPrename());

	}

	@Test
	public void testNotEmptyUserIsCreated() throws Exception {
	
		User user = new User();
		user.setId(1l);
		user.setName("a");
		user.setPrename("b");
		userService.saveUser(user);
	
		ModelAndView model = this.userController.prepareUserDataInput(1l);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		user = (User) model.getModel().get("User");
	}
	
	@Test
	public void testListUser() throws Exception {

		final ModelAndView model = this.userController.listUser();
		assertEquals(USER_LIST, model.getViewName());
		List<User> userlist = (List<User>) model.getModel().get("userlist");
		//assertEquals(true, userlist.size() > 0);
		
	}
}
