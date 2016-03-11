package de.neusta.facades;

import java.util.List;



import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import de.neusta.framework.rules.MockRule;
import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.DefaultUserService;


public class TestDefaultUserFacade {

	@Rule
	public TestRule mockRule = new MockRule(this);

	@InjectMocks
	DefaultUserFacade defaultUserFacade;
	
	@Mock
	DefaultUserService userService;

	@Mock
	UserDao userDao;

	@Mock
	User user;
	
	@Mock
	List<User> userList;
	
	@Test
	public void testSaveOrUpdateNewUser() throws Exception {
		
		// preparation
		Mockito.when(this.user.getId()).thenReturn(null);

		// execution
		defaultUserFacade.saveOrUpdate(user);
		
		// verification
		Mockito.verify(this.userDao, Mockito.times(1)).save(user);
		Mockito.verify(this.userDao, Mockito.never()).update(user);
		
	}

	@Test
	public void testSaveOrUpdateGivenUser() throws Exception {

		// preparation
		Mockito.when(this.user.getId()).thenReturn(1l);
		Mockito.when(this.userDao.update(user)).thenReturn(user);

		// execution
		defaultUserFacade.saveOrUpdate(user);
		
		// verification
		Mockito.verify(this.userDao, Mockito.times(1)).update(user);
		Mockito.verify(this.userDao, Mockito.never()).save(user);
	}

	@Test
	public void testGetAllUser() throws Exception {
		
		// preparation
		Mockito.when(userDao.findAll(User.class, "", "order by name")).thenReturn(userList);
		Mockito.when(userList.size()).thenReturn(20);

		// execution
		List<User> userList = defaultUserFacade.getAllUser();

		// verification
		Assert.assertEquals(20, userList.size());
	}

	@Test
	public void testCreateUserNew() throws Exception {
		
		// preparation
		Mockito.when(user.getId()).thenReturn(null);
		Mockito.when(user.getName()).thenReturn("");
		Mockito.when(user.getPrename()).thenReturn("");
		Mockito.when(userService.createuserObject()).thenReturn(user);

		// execution
		User user = defaultUserFacade.createUser(null);

		// verification
		Assert.assertNull(user.getId());
		Assert.assertEquals("", user.getName());
	}

	@Test
	public void testCreateUserGiven() throws Exception {
		
		// preparation
		Mockito.when(user.getId()).thenReturn(1l);
		Mockito.when(user.getName()).thenReturn("Wolff");
		Mockito.when(user.getPrename()).thenReturn("Manfred");
		Mockito.when(userDao.getPerID(1l, User.class)).thenReturn(user);

		// execution
		User user = defaultUserFacade.createUser(1l);

		// verification
		Assert.assertEquals(Long.valueOf(1),user.getId());
		Assert.assertEquals("Wolff", user.getName());
		Assert.assertEquals("Manfred", user.getPrename());
	}

}
