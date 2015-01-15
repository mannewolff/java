package de.neusta.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.DataBaseEntity;

@Repository
public class GenericDao<T extends DataBaseEntity> {

	@PersistenceContext
	protected EntityManager em;

	@SuppressWarnings("unchecked")
	public List<T> findAll(final Class<T> type, final String whereClause, final String orderByClause) {
		return this.em.createQuery("from " + type.getName() + " " + whereClause + " "  + orderByClause)
				.getResultList();
	}

	public T getPerID(final long id, final Class<T> type) {

		return this.em.find(type, new Long(id));
	}

	public void remove(final T dao) {
		this.em.remove(dao);
	}

	public void save(final T dao) {
		this.em.persist(dao);
	}

}
