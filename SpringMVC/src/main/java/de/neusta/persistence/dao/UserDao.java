package de.neusta.persistence.dao;

import java.util.List;

import de.neusta.persistence.entity.User;

public interface UserDao {
	
	/**
	 * Returns a list of all users.
	 * @return
	 */
	List<User> findAllUsers();
	
	/**
	 * Creates and modifies users.
	 * @param user
	 */
	void save(User user);
	
	/**
	 * Searches a user per name.
	 * @param name
	 * @return
	 */
	User getUserPerName(String name);
	
	/**
	 * Removes a user from the database.
	 * @param user
	 */
	void remove(User user);
	
}
