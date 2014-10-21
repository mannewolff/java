package de.neusta.service.person;

import org.apache.log4j.Logger;

public class DefaultPersonService implements PersonService {
	
	static Logger log =  Logger.getLogger(DefaultPersonService.class);

	@Override
	public String getName() {
		return "hello world autowired";
	}

}
