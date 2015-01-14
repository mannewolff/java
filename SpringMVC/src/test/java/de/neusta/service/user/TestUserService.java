package de.neusta.service.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.neusta.service.user.DefaultUserService;
import de.neusta.service.user.UserService;

@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestUserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Resource
	UserService personService;

	@Test
	public void testServiceExists() throws Exception {
		assertNotNull(this.personService);
	}

	@Test
	public void testServiceGetName() throws Exception {
		assertEquals("hello world autowired", this.personService.getName());
	}

}
