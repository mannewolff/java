package de.neusta.service;

import org.junit.Assert;
import org.junit.Test;

import de.neusta.persistence.entity.User;

public class TestDefaultUserService {
	
	@Test
	public void testCreateUserObject() throws Exception {

		// preparation
		UserService userService = new DefaultUserService();

		// execution
		User user = userService.createuserObject();

		// verification
		Assert.assertNotNull(user);
		Assert.assertEquals(null, user.getId());
		
	}
}


