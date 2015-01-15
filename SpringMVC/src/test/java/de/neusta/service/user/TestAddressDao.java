package de.neusta.service.user;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import de.neusta.persistence.dao.AddressDao;
import de.neusta.persistence.entity.Address;

@TransactionConfiguration
@ContextConfiguration({ "file:src/test/resources/applicationcontext.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class TestAddressDao {
	
	@Resource
	private AddressDao addressdao;
	
	private long initializeTwoAddresses() {
		
		//GenericDao<Address> adressDao = new GenericDao<Address>();
		Address address;
		address = new Address();
		address.setZipcode("28176");
		address.setCity("Bremen");
		address.setStreet("Woltmershauser Str. 22");
		address.setPhone("0421 555123");
		address.setMobile("0151 3234555");
		address.setEmailhome("anonymous@email.com");
		address.setEmailbusiness("business@email.com");
		addressdao.save(address);
		final long saveId = address.getId();

		address = new Address();
		address.setZipcode("34567");
		address.setCity("Hannover");
		address.setStreet("Limmer Str. 12");
		address.setPhone("0421 555444");
		address.setMobile("0151 5443555");
		address.setEmailhome("anonymous@email.com");
		address.setEmailbusiness("business@email.com");
		addressdao.save(address);
		
		return saveId;
	}
	
	@Test
	public void testAdressesSaveExists() throws Exception {

		initializeTwoAddresses();
		final List<Address> allAdresses = addressdao.findAll(Address.class);
		Assert.assertEquals("There should be two addresses", 2, allAdresses.size());
		Address address = addressdao.findAll(Address.class).get(1);
		Assert.assertEquals("Adress city should be Hannover", "Hannover", address.getCity());
		Assert.assertEquals("Adress zip code should be 34567", "34567", address.getZipcode());
		Assert.assertEquals("Adress street should be Limmer Str. 12", "Limmer Str. 12", address.getStreet());
		Assert.assertEquals("Adress phone should be 0421 555444", "0421 555444", address.getPhone());
		Assert.assertEquals("Adress mobile should be 0151 5443555", "0151 5443555", address.getMobile());
		Assert.assertEquals("Adress email home should be anonymous@email.com", "anonymous@email.com", address.getEmailhome());
		Assert.assertEquals("Adress email business should be business@email.com", "business@email.com", address.getEmailbusiness());
		
		Address add = new Address();
		add.setId(10l);
		Assert.assertEquals(Long.valueOf(10), add.getId());
		
	}

}
