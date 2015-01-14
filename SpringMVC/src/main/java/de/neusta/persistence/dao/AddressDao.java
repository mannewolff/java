package de.neusta.persistence.dao;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.Address;
import de.neusta.persistence.entity.User;

@Repository
public class AddressDao extends GenericDao<Address> {

	@Override
	public String getDataBaseName() {
		return "Address";
	}

}
