package de.neusta.persistence.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.entity.User;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestGenericDao {
	
	@Resource
	UserDao userdao;

	@Test
	@Transactional
	public void testFindAll() throws Exception {
		
		User user1 = new User();
		user1.setName("Manfred");
		this.userdao.save(user1);
		
		User user2 = new User();
		user2.setName("Gabi");
		userdao.save(user2);
		
		List<User> userlist = userdao.findAll(User.class, "", "");
		Assert.assertEquals(2, userlist.size());
	}

	@Test
	public void testGetPerId() throws Exception {

		User user1 = new User();
		user1.setName("Manfred");
		this.userdao.save(user1);
		
		Long id = user1.getId();
		
		User user2 = userdao.getPerID(id, User.class);
		assertEquals(user1.getName(), user2.getName());
	}
	
	@Test
	public void testGetPerIdNull() throws Exception {
		User user = userdao.getPerID(22l, User.class);
		assertNull(user);
	}
	
	@Test
	public void testRemove() throws Exception {

		User user1 = new User();
		user1.setName("Manfred");
		this.userdao.save(user1);
		
		User user = userdao.getPerID(user1.getId(), User.class);
		assertNotNull(user);
		
		userdao.remove(user1);
		
		user = userdao.getPerID(user1.getId(), User.class);
		assertNull(user);
	}
	
	@Test
	public void testMerge() throws Exception {
		
		User user1 = new User();
		user1.setName("Manfred");
		this.userdao.save(user1);

		user1.setName("Helmut");
		userdao.update(user1);
		
	}
}
