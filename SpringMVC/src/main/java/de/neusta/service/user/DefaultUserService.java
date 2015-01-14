package de.neusta.service.user;

import org.apache.log4j.Logger;

public class DefaultUserService implements UserService {

	static Logger log = Logger.getLogger(DefaultUserService.class);

	@Override
	public String getName() {
		return "hello world autowired";
	}

}
