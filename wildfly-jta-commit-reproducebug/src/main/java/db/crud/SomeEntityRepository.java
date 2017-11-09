package db.crud;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import db.model.SomeEntity;

/**
 * Convenience tool to hammer the database.
 */
@RequestScoped
public class SomeEntityRepository {

    @PersistenceContext(unitName = "ContainerManagedXaPU")
    private EntityManager em;

    /**
     * @param entity
     *            entity to be persisted
     */
    public void perist(SomeEntity entity) {
        em.persist(entity);
    }

    /**
     * Fetch the entity by ID.
     *
     * @param primaryKey
     *            the entity primary key
     */
    public SomeEntity fetchById(Integer primaryKey) {
        return em.find(SomeEntity.class, primaryKey);
    }

    /**
     * Delete all of the {@link SomeEntity} entities in the DB
     */
    public void deleteAll() {
        for (Integer currentId : fetchAlIds()) {
            SomeEntity someEntity = em.find(SomeEntity.class, currentId);
            em.remove(someEntity);
        }
    }

    /**
     * Fetch all primary keys in the DB.
     *
     * @return The primary keys in the DB.
     */
    @SuppressWarnings("unchecked")
    public List<Integer> fetchAlIds() {
        return (List<Integer>) em.createQuery("SELECT E.id FROM SomeEntity E").getResultList();
    }

    /**
     *
     * @return The entity manager
     */
    public EntityManager getEm() {
        return em;
    }

}
