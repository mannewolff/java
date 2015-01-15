package de.neusta.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

@Repository
public class UserDao extends GenericDao<User> {

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
	
	public List<Address> getAddresses(User user) {
		
		Set<Address> addressset = user.getAddresses();
		List<Address> addresslist = new ArrayList<Address>();
		for (Address address : addressset) {
			addresslist.add(address);
		}
		return addresslist;
	}

	public void merge(User user) {
		em.merge(user);
	}

}
