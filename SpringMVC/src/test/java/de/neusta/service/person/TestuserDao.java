package de.neusta.service.person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.login.validator.LoginContext;
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
	public void testGetUserPerLogin() throws Exception {
		User user = new User();
		user.setLogin("user1");
		this.userDao.save(user);
		final long saveId = user.getId();

		user = new User();
		user.setLogin("user2");
		this.userDao.save(user);

		user = this.userDao.getUserPerLogin("user1");
		assertEquals("User name should be user1", "user1", user.getLogin());
		assertEquals("User id should be the saved id", Long.valueOf(saveId),
				user.getId());
	}

	@Test
	public void testGetUserPerName() throws Exception {
		User user = new User();
		user.setName("user1");
		this.userDao.save(user);
		final long saveId = user.getId();

		user = new User();
		user.setName("user2");
		this.userDao.save(user);

		user = this.userDao.getUserPerName("user1");
		assertEquals("User name should be user1", "user1", user.getName());
		assertEquals("User id should be the saved id", Long.valueOf(saveId),
				user.getId());
	}

	@Test
	public void testRemoveUser() throws Exception {
		final User user = new User();
		user.setName("user1");
		this.userDao.save(user);

		this.userDao.remove(user);
		final List<User> userList = this.userDao.findAll();
		Assert.assertEquals("There should be no user in list.", 0,
				userList.size());

	}

	@Test
	public void testUserDaoInjected() throws Exception {
		assertNotNull(this.userDao);
	}

	@Test
	public void testUserPassword() throws Exception {
		final LoginContext loginContext = new LoginContext();
		loginContext.setOriginalPasswd("halloWelt", "MD5");
		User user = new User();
		user.setLogin("Manne");
		user.setPassword(loginContext.getPasswd());
		this.userDao.save(user);
		user = this.userDao.getUserPerLogin("Manne");
		assertEquals("User name should be Manne", "Manne", user.getLogin());
		assertEquals("User passwd should be the saved passwd",
				loginContext.getPasswd(), user.getPassword());
	}

	@Test
	public void testUserSaveExists() throws Exception {
		final User user = new User();
		user.setName("testSaveExist");
		this.userDao.save(user);

		User savedUser = this.userDao.findAll().get(0);
		user.setName("newname");
		this.userDao.save(savedUser);
		user.setLogin("newname");

		final List<User> allUsers = this.userDao.findAll();
		Assert.assertEquals("There should be one user", 1, allUsers.size());
		savedUser = this.userDao.findAll().get(0);
		Assert.assertEquals("User name should be newname", "newname",
				savedUser.getName());
	}

	@Test
	public void testUserSaveNotExist() {
		final User user = new User();
		user.setName("testSaveNotExist");
		this.userDao.save(user);
		final User savedUser = this.userDao.findAll().get(0);
		Assert.assertEquals("User should be equal to saved user", user,
				savedUser);
	}
}
