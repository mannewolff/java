package de.neusta.facades;

import java.util.List;

import de.neusta.persistence.entity.Config;

public interface ConfigFacade {
	
	/**
	 * Gets a list of all configurations stored in the database.
	 * @return
	 */
	List<Config> getAllConfig();
	

}
