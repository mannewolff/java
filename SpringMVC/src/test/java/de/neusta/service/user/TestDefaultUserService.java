package de.neusta.service.user;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import de.neusta.framework.rules.MockRule;
import de.neusta.persistence.dao.UserDao;

public class TestDefaultUserService {
	
	@Rule
	public TestRule mockRule = new MockRule(this);

	@InjectMocks
	DefaultUserService userService;

	@Mock
	UserDao userdao;

	@Test
	public void testGetUserDao() throws Exception {

		// preparation
		
		// execution
		UserDao dao = userService.getUserDao();
		
		// verification
		Assert.assertSame(dao, userdao);
	}

}


