package de.neusta.service;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import de.neusta.framework.rules.MockRule;
import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;

public class TestDefaultUserService {
	
	@Rule
	public TestRule mockRule = new MockRule(this);

	@InjectMocks
	DefaultUserService userService;

	@Mock
	UserDao userdao;

	@Test
	public void testCreateUserObject() throws Exception {

		// preparation
		userService = new DefaultUserService();

		// execution
		User user = userService.createuserObject();

		// verification
		Assert.assertNotNull(user);
		Assert.assertEquals(null, user.getId());
		
	}
	
}


