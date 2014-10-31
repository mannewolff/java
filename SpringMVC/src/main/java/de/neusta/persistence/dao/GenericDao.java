package de.neusta.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import de.neusta.persistence.entity.DataBaseEntity;

public abstract class GenericDao<T extends DataBaseEntity> {
	
		@PersistenceContext
		protected EntityManager em;
		
		public abstract String getDataBaseName();

		@SuppressWarnings("unchecked")
		public List<T> findAll() {
			return em.createQuery("from " + getDataBaseName() + "").getResultList();
		}

		public void save(T dao) {
			em.persist(dao);
		}
		
		public void remove(T dao) {
			em.remove(dao);
		}

}
