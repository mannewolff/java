package de.neusta.service.user;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import de.neusta.persistence.dao.UserDao;

public class DefaultUserService implements UserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Resource
	UserDao userdao;

	@Override
	public UserDao getUserDao() {
		return userdao;
	}

}
