package de.neusta.facades;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import de.neusta.framework.rules.MockRule;
import de.neusta.persistence.dao.ConfigDao;
import de.neusta.persistence.entity.Config;


public class TestDefaultConfigFacade {

	@Rule
	public TestRule mockRule = new MockRule(this);

	@InjectMocks
	DefaultConfigFacade configfacade;
	
	@Mock
	ConfigDao configDao;
	
	@Mock
	List<Config> configList;

	@Test
	public void testGetAllConfig() throws Exception {
		// preparation
		Mockito.when(configDao.findAll(Config.class, "", "order by name")).thenReturn(configList);
		Mockito.when(configList.size()).thenReturn(20);

		// execution
		List<Config> configurationList = configfacade.getAllConfig();

		// verification
		Assert.assertEquals(20, configurationList.size());
	}

}
