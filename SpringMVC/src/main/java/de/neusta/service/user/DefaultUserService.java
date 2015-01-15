package de.neusta.service.user;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

public class DefaultUserService implements UserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Resource
	UserDao userdao;

	@Override
	public List<User> getUserList() {
		return userdao.findAll(User.class);
	}

	@Override
	public void deleteUser(Long userId) {
		User user = userdao.getPerID(userId, User.class);
		if (user != null) {
			userdao.remove(user);
		}
	}

	@Override
	public void deleteUser(User user) {
		if (user != null) {
			deleteUser(user.getId());
		}
	}

	@Override
	public void saveUser(User user) {
		if (user != null) {
			userdao.save(user);
		}
	}

	@Override
	public List<Address> getUserAddresses(final User user) {
		return null;
	}

	@Override
	public void addAdress(Address address) {
	}

	@Override
	public User getUser(Long userId) {
		return userdao.getPerID(userId.longValue(), User.class);
	}

}
