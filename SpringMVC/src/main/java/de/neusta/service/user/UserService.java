package de.neusta.service.user;

import java.util.List;

import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

public interface UserService {

	List<User> getUserList();
	
	User getUser(final Long userId);
	
	void deleteUser(final Long userId);
	
	void deleteUser(final User user);
	
	void saveUser(final User user);

	List<Address> getUserAddresses(final User user);
	
	void addAddress(final User user, final Address address);

	void mergeUser(User user);
}