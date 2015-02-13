package de.neusta.persistence.dao;

import java.util.List;

import javax.annotation.Resource;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.entity.Config;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestConfigDao {
	
	@Resource
	ConfigDao configDao;
	

	@Test
	public void testGetAllConfig() throws Exception {

		// preparation
		Config config = new Config();
		config.setName("Generator");
		configDao.save(config);

		// execution
		List<Config> configs = configDao.findAll(Config.class, "", "order by name");
		
		// verification
		Assert.assertEquals(1, configs.size());
	}

}
