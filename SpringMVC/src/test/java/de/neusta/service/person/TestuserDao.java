package de.neusta.service.person;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestuserDao {

	@Resource
	UserDao userDao;
	
	@Test
	public void testUserDaoInjected() throws Exception {
		assertNotNull(userDao);
	}

	@Test
	public void testUserSaveNotExist() {
		User user = new User();
		user.setName("testSaveNotExist");
		userDao.save(user);
		User savedUser = userDao.findAllUsers().get(0);
		Assert.assertEquals("User should be equal to saved user", user,
				savedUser);
	}

	@Test
	public void testUserSaveExists() throws Exception {
		User user = new User();
		user.setName("testSaveExist");
		userDao.save(user);

		User savedUser = userDao.findAllUsers().get(0);
		user.setName("newname");
		userDao.save(savedUser);

		List<User> allUsers = userDao.findAllUsers();
		Assert.assertEquals("There should be one user", 1, allUsers.size());
		savedUser = userDao.findAllUsers().get(0);
		Assert.assertEquals("User name should be newname", "newname",
				savedUser.getName());
	}

	@Test
	public void testGetUserPerName() throws Exception {
		User user = new User();
		user.setName("user1");
		userDao.save(user);
		long saveId = user.getId();

		user = new User();
		user.setName("user2");
		userDao.save(user);

		user = userDao.getUserPerName("user1");
		assertEquals("User name should be user1", "user1", user.getName());
		assertEquals("User id should be the saved id", Long.valueOf(saveId),
				user.getId());
	}

	@Test
	public void testRemoveUser() throws Exception {
		User user = new User();
		user.setName("user1");
		userDao.save(user);

		userDao.remove(user);
		List<User> userList = userDao.findAllUsers();
		Assert.assertEquals("There should be no user in list.", 0,
				userList.size());

	}
}
