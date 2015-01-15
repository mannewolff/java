package de.neusta.service.user;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

public class DefaultUserService implements UserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Resource
	UserDao userdao;

	@Override
	public List<User> getUserList() {
		return userdao.findAll(User.class, "", "order by name");
	}

	@Override
	@Transactional
	public void deleteUser(Long userId) {
		User user = userdao.getPerID(userId, User.class);
		if (user != null) {
			userdao.remove(user);
		}
	}

	@Override
	@Transactional
	public void deleteUser(User user) {
		if (user != null) {
			deleteUser(user.getId());
		}
	}

	@Override
	@Transactional
	public void saveUser(User user) {
		if (user != null) {
			userdao.save(user);
		}
	}

	@Override
	public List<Address> getUserAddresses(final User user) {
		return userdao.getAddresses(user);
	}

	@Override
	@Transactional
	public void addAddress(final User user, final Address address) {
		userdao.setAddress(user, address);
	}

	@Override
	public User getUser(Long userId) {
		return userdao.getPerID(userId.longValue(), User.class);
	}

}
