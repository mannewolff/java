package de.neusta.service;

import org.apache.log4j.Logger;

import de.neusta.persistence.entity.User;

public class DefaultUserService implements UserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Override
	public User createuserObject() {
		User user = new User();
		user.setId(null);
		user.setName("");
		user.setPrename("");
		user.setLogin("");
		user.setPassword("");
		user.setComment("");
		return user;
	}
}
