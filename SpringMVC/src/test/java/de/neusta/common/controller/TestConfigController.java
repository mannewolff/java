package de.neusta.common.controller;

import static de.neusta.common.controller.ControllerConstants.CONFIG_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;

import de.neusta.facades.ConfigFacade;
import de.neusta.persistence.dao.ConfigDao;
import de.neusta.persistence.entity.Config;


@RunWith(MockitoJUnitRunner.class)
public class TestConfigController {

	@InjectMocks
	ConfigController configController;

	@Mock
	ConfigFacade configFacade;

	@Mock
	ConfigDao configDao;

	@Mock
	Config config;

	@Mock
	List<Config> configList;

	@Test
	public void testListConfigs() throws Exception {
		
		// preparation
		Mockito.when(config.getName()).thenReturn("Generator");
		Mockito.when(configFacade.getAllConfig()).thenReturn(configList);
		Mockito.when(configList.size()).thenReturn(20);

		// execution
		final ModelAndView model = this.configController.listConfigs();
		
		// verifying
		assertEquals(CONFIG_LIST, model.getViewName());
		@SuppressWarnings("unchecked")
		List<Config> myConfigList = (List<Config>) model.getModel().get("Configlist");
		assertSame(myConfigList, configList);
	}

}
