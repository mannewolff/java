package de.neusta.service.user;

import java.util.List;

import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

public interface UserService {

	void createUser(final User user);
	
	List<User> getUserList();
	
	User getUser(final Long userId);
	
	void deleteUser(final Long userId);
	
	void deleteUser(final User user);
	
	void updateUser(final User user);
	
	void createOrUpdateUser(final User user);

	List<Address> getUserAddresses(final User user);
	
	void addAdress(Address address);
}