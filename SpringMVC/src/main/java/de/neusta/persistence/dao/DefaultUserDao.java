package de.neusta.persistence.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import de.neusta.persistence.entity.User;

@Repository
public class DefaultUserDao implements UserDao {

	@PersistenceContext
	private EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public List<User> findAllUsers() {
		return em.createQuery("from User u").getResultList();
	}

	@Override
	public void save(User user) {
		em.persist(user);
	}
	
	@Override
	public User getUserPerName(String name)  {
		return (User) em.createQuery("from User u where name = '" + name + "'").getSingleResult();
	}

	@Override
	public void remove(User user) {
		em.remove(user);
	}
	
}
