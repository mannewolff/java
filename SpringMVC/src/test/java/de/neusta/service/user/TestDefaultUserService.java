package de.neusta.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestDefaultUserService {

	@Resource
	UserService userService;

	@Resource
	UserDao userdao;

	@Test
	public void testGetUserId() throws Exception {
		assertNotNull(userService);
		Long pk = createUser("Wolff", "Manfred");
		User user = userService.getUser(pk);
		assertEquals("Wolff", user.getName());
	}

	@Test
	public void testGetUserList() throws Exception {

		createUser("Rosenbaum", "Gabi");
		createUser("Willems", "Bianca");
		Thread.sleep(100);
		List<User> userlist = userService.getUserList();
		assertEquals(2, userlist.size());
	}

	@Test
	public void testDeleteUserPerId() throws Exception {
		Long pk = createUser("Knoop", "Jan");
		Thread.sleep(100);
		User user = userdao.getPerID(pk, User.class);
		assertNotNull(user);
		userService.deleteUser(pk);
		Thread.sleep(500);
		user = userdao.getPerID(pk, User.class);
		assertNull(user);
	}

	@Test
	public void testDeleteUser() throws Exception {
		// No exception should occur
		User nullUser = null;
		userService.deleteUser(nullUser);
		userService.deleteUser(0l);
		
		// the real test
		User user = new User();
		user.setName("Meier");
		user.setPrename("Jan");
		userdao.save(user);
		Long pk = user.getId();
		Thread.sleep(100);
		assertNotNull(user);
		userService.deleteUser(user);
		Thread.sleep(1000);
		user = userdao.getPerID(pk, User.class);
		assertNull(user);
	}

	@Test
	public void testSaveUser() throws Exception {
		
		// No exception should occur
		User nulluser = null;
		userService.saveUser(nulluser);
		
		// the real test
		Long pk = createUser("Rogge", "Helmut");
		User user = userdao.getPerID(pk, User.class);
		user.setName("Changed");
		userService.saveUser(user);
		Thread.sleep(1000);
		User userChanged = userdao.getPerID(pk, User.class);
		assertEquals("Changed", userChanged.getName());
	}
	
	@Test
	public void testGetUserAdresses() throws Exception {

		User user = new User();
		user.setName("Meier");
		user.setPrename("Jan");
		userService.saveUser(user);
		Address adress = new Address();
		adress.setCity("Bremen");
		userdao.setAddress(user, adress);
		List<Address> addresses = userService.getUserAddresses(user);
		assertEquals(1, addresses.size());

	}
	
	@Test
	public void testAddAddress() throws Exception {
		User user = new User();
		user.setName("Meier");
		user.setPrename("Jan");
		userService.saveUser(user);
		Address adress = new Address();
		adress.setCity("Bremen");
		userService.addAddress(user, adress);
		List<Address> addresses = userService.getUserAddresses(user);
		assertEquals(1, addresses.size());
	}

	private Long createUser(String name, String prename) {
		User user = new User();
		user.setName(name);
		user.setPrename(prename);
		userdao.save(user);
		Long pk = user.getId();
		return pk;
	}

	@Test
	public void testMergeUser() throws Exception {
		User user = new User();
		user.setName("a");
		user.setPrename("b");
		userService.saveUser(user);
		Long pk = user.getId();
		user.setPrename("c");
		userService.mergeUser(user);
		User perID = userdao.getPerID(pk, User.class);
		assertEquals("c", perID.getPrename());;
	}
	
	
}
