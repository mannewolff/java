package de.neusta.facades;

import java.util.List;

import de.neusta.persistence.entity.User;

public interface UserFacade {
	
	/**
	 * Calculates even the object is new and has to be saved or is just modified
	 * and has to be merged. The actual calculation is done by the service.
	 */
	void saveOrUpdate(final User user);
	
	/**
	 * Creates a new user either from scratch if id == null or from database.
	 */
	User createUser(final Long id);
	
	/**
	 * Gets a list of all users stored in the database.
	 * @return
	 */
	List<User> getAllUser();
	

}
