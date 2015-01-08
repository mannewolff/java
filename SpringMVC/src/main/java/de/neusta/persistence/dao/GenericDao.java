package de.neusta.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.neusta.persistence.entity.DataBaseEntity;

public abstract class GenericDao<T extends DataBaseEntity> {

	@PersistenceContext
	protected EntityManager em;

	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		return this.em.createQuery("from " + getDataBaseName() + "")
				.getResultList();
	}

	public abstract String getDataBaseName();

	public void remove(final T dao) {
		this.em.remove(dao);
	}

	public void save(final T dao) {
		this.em.persist(dao);
	}

}
