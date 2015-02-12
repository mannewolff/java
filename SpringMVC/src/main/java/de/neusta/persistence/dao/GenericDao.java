package de.neusta.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional
	public void remove(final T dao) {
		this.em.remove(dao);
	}

	@Transactional
	public void save(final T dao) {
		this.em.persist(dao);
	}

	@SuppressWarnings("hiding")
	@Transactional
	public <T> T update (final T dao) {
		return this.em.merge(dao);
	}
}
