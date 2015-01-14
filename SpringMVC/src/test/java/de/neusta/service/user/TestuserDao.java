package de.neusta.service.user;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.login.validator.LoginContext;
import de.neusta.persistence.dao.AddressDao;
import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestuserDao {
	
	@Resource
	UserDao userDao;
	
	@Resource
	AddressDao addressDao;

	@Test
	public void testGetUserPerLogin() throws Exception {
		User user = null;
		final long saveId = initializeTwoUser();

		user = this.userDao.getUserPerLogin("mannewolff");
		assertEquals("User name should be mannewolff", "mannewolff", user.getLogin());
		assertEquals("User id should be the saved id", Long.valueOf(saveId),
				user.getId());
	}

	private long initializeTwoUser() {
		User user;
		user = new User();
		user.setPrename("Manfred");
		user.setName("Wolff");
		user.setLogin("mannewolff");
		this.userDao.save(user);
		final long saveId = user.getId();

		user = new User();
		user.setPrename("Gabi");
		user.setName("Rosenbaum");
		user.setLogin("grosserose");
		this.userDao.save(user);
		
		user = new User();
		user.setId(12l);
		Long l = user.getId();

		return saveId;
	}

	@Test
	public void testGetUserPerName() throws Exception {

		User user = null;
		final long saveId = initializeTwoUser();

		user = this.userDao.getUserPerName("Wolff");
		assertEquals("User name should be Wolff", "Wolff", user.getName());
		assertEquals("User prename should be Manfred", "Manfred", user.getPrename());
		assertEquals("User id should be the saved id", Long.valueOf(saveId),
				user.getId());
	}

	@Test
	public void testRemoveUser() throws Exception {
		User user = null;
		user = new User();
		user.setPrename("Manfred");
		user.setName("Wolff");
		user.setLogin("mannewolff");
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
	
	@Test
	public void testUserHasAdresses() throws Exception {
			Address address;
			address = new Address();
			address.setZipcode("28176");
			address.setCity("Bremen");
			address.setStreet("Woltmershauser Str. 22");
			address.setPhone("0421 555123");
			address.setMobile("0151 3234555");
			address.setEmailhome("anonymous@email.com");
			address.setEmailbusiness("business@email.com");
			addressDao.save(address);
			
			User user = new User();
			user.setName("Wolff");
			userDao.save(user);
			
			userDao.setAddress(user, address);

			address = new Address();
			address.setZipcode("34567");
			address.setCity("Hannover");
			address.setStreet("Limmer Str. 12");
			address.setPhone("0421 555444");
			address.setMobile("0151 5443555");
			address.setEmailhome("anonymous@email.com");
			address.setEmailbusiness("business@email.com");
			addressDao.save(address);

			Set<Address> addresses = user.getAddresses();
			addresses.add(address);
			user.setAddresses(addresses);

			addresses = user.getAddresses();
			Assert.assertEquals(2, addresses.size());
	}
}
