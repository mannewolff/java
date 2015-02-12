package de.neusta.persistence.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Config implements DataBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected Long id;

	protected String name;

	@OneToMany(fetch = FetchType.EAGER)
	protected Set<ConfigItem> configuration = new HashSet<ConfigItem>();
	
	public Set<ConfigItem> getConfigItems() {
		return this.configuration;
	}
	

	public void setAddresses(Set<ConfigItem> configuration) {
		this.configuration = configuration;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
