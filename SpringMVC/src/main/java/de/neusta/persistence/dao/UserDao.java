package de.neusta.persistence.dao;

import java.util.Set;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

@Repository
public class UserDao extends GenericDao<User> {

	@Override
	public String getDataBaseName() {
		return "User";
	}

	public User getUserPerLogin(final String login) {
		return (User) this.em.createQuery(
				"from User u where login = '" + login + "'").getSingleResult();
	}

	public User getUserPerName(final String name) {
		return (User) this.em.createQuery(
				"from User u where name = '" + name + "'").getSingleResult();
	}

	public void setAddress(User user, Address address) {
		Set<Address> addresse = user.getAddresses();
		addresse.add(address);
		save(user);
	}

}
