package de.neusta.service.user;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.Resource;

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
	public void testCreateUser() throws Exception {
		User user = new User();
		user.setName("Wolff");
		user.setPrename("Manfred");
		userService.createUser(user);
		Thread.sleep(100);
	    user = userdao.getUserPerName("Wolff");
	    assertNotNull(user);
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
		User user = userdao.getPerID(pk);
		assertNotNull(user);
		userService.deleteUser(pk);
		Thread.sleep(500);
		user = userdao.getPerID(pk);
		assertNull(user);
	}
	
	@Test
	public void testDeleteUser() throws Exception {
		User user = new User();
		user.setName("Meier");
		user.setPrename("Jan");
		userdao.save(user);
		Long pk = user.getId();
		Thread.sleep(100);
		assertNotNull(user);
		userService.deleteUser(user);
		Thread.sleep(1000);
		user = userdao.getPerID(pk);
		assertNull(user);
	}

	@Test
	public void testUpdateUser() throws Exception {
		
		Long pk = createUser("Rogge", "Helmut");
		User user = userdao.getPerID(pk);
		user.setName("Changed");
		userService.updateUser(user);
		Thread.sleep(1000);
		User userChanged = userdao.getPerID(pk);
		assertEquals("Changed", userChanged.getName());
	}
	
	private Long createUser(String name, String prename) {
		User user = new User();
		user.setName(name);
		user.setPrename(prename);
		userdao.save(user);
		Long pk = user.getId();
		return pk;
	}
}
