package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;
import static org.junit.Assert.*;

import java.util.List;

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
	User user;

	@Mock
	HttpSession session;
	
	@Mock
	List<User> userList;

	@SuppressWarnings("static-access")
	@Test
	public void testControllerConstant() throws Exception {
		ControllerConstants constants = new ControllerConstants();
		assertEquals("userdatainputpage.vm", constants.USER_INPUT_PAGE);
	}

	// *******************************************************************
	// URL /preuser
	// *******************************************************************
	@Test
	public void testPrepareUserDataInputWithZeroValue() throws Exception {

		ModelAndView model = this.userController.prepareUserDataInput(0l);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("", user.getName());
		assertEquals("", user.getPrename());

	}

	@Test
	public void testPrepareUserDataInputWithNullValue() throws Exception {
		ModelAndView model = this.userController.prepareUserDataInput(null);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("", user.getName());
		assertEquals("", user.getPrename());
	}

	@Test
	public void testPrepareUserDataInputWithUserId() throws Exception {
		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		Mockito.when(this.user.getId()).thenReturn(1l);
		Mockito.when(this.userService.getUser(1l)).thenReturn(user);

		ModelAndView model = this.userController.prepareUserDataInput(1l);
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals("Wolff", user.getName());
		assertEquals("Manne", user.getPrename());
	}

	// *******************************************************************
	// URL /adduser
	// *******************************************************************
	@Test
	public void testaddUserWithGivenID() throws Exception {

		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		Mockito.when(this.user.getId()).thenReturn(1l);

		// execution
		final ModelAndView model = this.userController.addUser(user);

		// verifying
		Mockito.verify(this.userService, Mockito.times(1)).mergeUser(user);
		assertEquals(USER_LIST, model.getViewName());
	}

	@Test
	public void testaddNullUser() throws Exception {

		// execution
		final ModelAndView model = this.userController.addUser(null);

		// verifying
		assertEquals(USER_INPUT_PAGE, model.getViewName());
	}

	@Test
	public void testadd0User() throws Exception {

		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		Mockito.when(this.user.getId()).thenReturn(0l);

		// execution
		final ModelAndView model = this.userController.addUser(user);

		// verifying
		Mockito.verify(this.userService, Mockito.times(1)).saveUser(user);
		assertEquals(USER_LIST, model.getViewName());
	}

	// *******************************************************************
	// URL //listuser
	// *******************************************************************
	@SuppressWarnings("unchecked")
	@Test
	public void testListUser() throws Exception {
		
		// preparation
		user.setName("Wolff");
		user.setPrename("Manfred");
		userList.add(user);
		Mockito.when(this.userService.getUserList()).thenReturn(userList);

		// execution
		final ModelAndView model = this.userController.listUser();
		
		// verifying
		assertEquals(USER_LIST, model.getViewName());
		List<User> myUserList = (List<User>) model.getModel().get("Userlist");
		assertSame(myUserList, userList);
	}
}
