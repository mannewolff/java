package de.neusta.facades;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import de.neusta.persistence.dao.UserDao;
import de.neusta.persistence.entity.User;
import de.neusta.service.UserService;

public class DefaultUserFacade implements UserFacade {

	static Logger log = Logger.getLogger(DefaultUserFacade.class);

	@Resource
	UserService userService;

	@Resource
	UserDao userDao;

	@Override
	public void saveOrUpdate(final User user) {
		
		if ((user.getId() == null) || (user.getId() == 0l)) {
			log.debug("Saving user " + user.getId() + " " + user.getPrename()
					+ " " + user.getName() + " " + user.getComment() + " "
					+ user.getLogin());
			userDao.save(user);
		} else {
			log.debug("Merging user " + user.getId() + " " + user.getPrename()
					+ " " + user.getName() + " " + user.getComment() + " "
					+ user.getLogin());
			User mergedUser = userDao.update(user);
			log.debug("Merged user " + mergedUser.getId() + " "
					+ mergedUser.getPrename() + " " + mergedUser.getName()
					+ " " + mergedUser.getComment() + " "
					+ mergedUser.getLogin());
		}
	}

	@Override
	public User createUser(Long id) {
		User user = null;
		
		if ((id == null) || (id == 0)) {
			user = userService.createuserObject();
		} else {
			user = userDao.getPerID(id, User.class);
		}
		
		return user;
	}

	@Override
	public List<User> getAllUser() {
		List<User> userlist = userDao.findAll(User.class, "", "order by name");
		return userlist;
	}

}
