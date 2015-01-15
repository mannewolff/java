package de.neusta.persistence.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Address implements DataBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long id;
	
	protected String zipcode;
	
	protected String city;
	
	protected String street;
	
	protected String phone;
	
	protected String mobile;
	
	protected String emailhome;

	protected String emailbusiness;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getEmailhome() {
		return emailhome;
	}

	public void setEmailhome(String emailhome) {
		this.emailhome = emailhome;
	}

	public String getEmailbusiness() {
		return emailbusiness;
	}

	public void setEmailbusiness(String emailbusiness) {
		this.emailbusiness = emailbusiness;
	}
}
