package de.neusta.service.person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)

public class TestPersonService {

	static Logger log =  Logger.getLogger(DefaultPersonService.class);
	
	@Resource
	PersonService personService;

	@Test
	public void testServiceExists() throws Exception {
		assertNotNull(personService);
	}
	
	@Test
	public void testServiceGetName() throws Exception {
		assertEquals("hello world autowired", personService.getName());
	}


}
