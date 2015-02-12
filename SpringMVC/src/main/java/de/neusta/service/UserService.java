package de.neusta.service;

import de.neusta.persistence.entity.User;


public interface UserService {

	/**
	 * Creates a fresh instance of a User object.
	 * @return The User object.
	 */
	User createuserObject();
}