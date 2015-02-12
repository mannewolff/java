package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.USER_INPUT_PAGE;
import static de.neusta.common.controller.ControllerConstants.USER_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.facades.UserFacade;
import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TestUserController {

	@InjectMocks
	UserController userController;

	@Mock
	UserService userService;

	@Mock
	UserFacade userFacade;

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

		// preparation
		Mockito.when(this.user.getId()).thenReturn(0l);
		Mockito.when(userFacade.createUser(0l)).thenReturn(user);
		
		// execution
		ModelAndView model = this.userController.prepareUserDataInput(0l);
		
		// validation
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertEquals(Long.valueOf(0), Long.valueOf(user.getId()));

	}

	@Test
	public void testPrepareUserDataInputWithNullValue() throws Exception {
		// preparation
		Mockito.when(this.user.getId()).thenReturn(null);
		Mockito.when(userFacade.createUser(null)).thenReturn(user);

		// execution
		ModelAndView model = this.userController.prepareUserDataInput(null);
		
		// validation
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		User user = (User) model.getModel().get("User");
		assertNotNull(user);
		assertNull(user.getId());
	}

	@Test
	public void testPrepareUserDataInputWithUserId() throws Exception {
		// preparation
		Mockito.when(this.user.getName()).thenReturn("Wolff");
		Mockito.when(this.user.getPrename()).thenReturn("Manne");
		Mockito.when(this.user.getId()).thenReturn(1l);
		Mockito.when(userFacade.createUser(1l)).thenReturn(user);
		
		// execution
		ModelAndView model = this.userController.prepareUserDataInput(1l);

		// validation
		assertEquals(USER_INPUT_PAGE, model.getViewName());
		user = (User) model.getModel().get("User");
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
		Mockito.when(userDao.findAll(User.class, "", "order by name")).thenReturn(userList);
		Mockito.when(userList.size()).thenReturn(20);


		// execution
		final ModelAndView model = this.userController.addUser(user);

		// verifying
		Mockito.verify(this.userFacade, Mockito.times(1)).saveOrUpdate(user);
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
		Mockito.when(userDao.findAll(User.class, "", "order by name")).thenReturn(userList);
		Mockito.when(userList.size()).thenReturn(20);

		// execution
		final ModelAndView model = this.userController.addUser(user);

		// verifying
		Mockito.verify(this.userFacade, Mockito.times(1)).saveOrUpdate(user);
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
		Mockito.when(userFacade.getAllUser()).thenReturn(userList);
		Mockito.when(userList.size()).thenReturn(20);

		// execution
		final ModelAndView model = this.userController.listUser();
		
		// verifying
		assertEquals(USER_LIST, model.getViewName());
		List<User> myUserList = (List<User>) model.getModel().get("Userlist");
		assertSame(myUserList, userList);
	}

	@Test
	public void testDeleteUser() throws Exception {

		// execution
		final ModelAndView model = this.userController.deleteUser(1l);
		
	}

}
