package de.neusta.facades;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import de.neusta.persistence.dao.ConfigDao;
import de.neusta.persistence.entity.Config;

public class DefaultConfigFacade implements ConfigFacade {

	static Logger log = Logger.getLogger(DefaultConfigFacade.class);

	@Resource
	ConfigDao configDao;

	@Override
	public List<Config> getAllConfig() {
		List<Config> configlist = configDao.findAll(Config.class, "", "order by name");
		return configlist;
	}

}
