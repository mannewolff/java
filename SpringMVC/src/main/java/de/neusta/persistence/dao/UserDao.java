package de.neusta.persistence.dao;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.User;

@Repository
public class UserDao extends GenericDao<User> {


	public User getUserPerName(String name)  {
		return (User) em.createQuery("from User u where name = '" + name + "'").getSingleResult();
	}

	public User getUserPerLogin(String login) {
		return (User) em.createQuery("from User u where login = '" + login + "'").getSingleResult();
	}

	@Override
	public String getDataBaseName() {
		return "User";
	}
	
}
