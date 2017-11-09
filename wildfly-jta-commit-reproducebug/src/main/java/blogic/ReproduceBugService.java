package blogic;

import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import db.crud.SomeEntityRepository;
import db.model.SomeEntity;

/**
 * Convenience tool to hammer the database.
 */
@ApplicationScoped
public class ReproduceBugService {

    @Inject
    SomeEntityRepository someEntityRepository;

    /**
     * This API is particularly relevant because it allows us to force eclipselink to acquire a JTA datasource
     * connection to run the query. If we just try to persist the entity, eclipselink can delay the acquisition of the
     * connection until the last moment when the transaction needs to start commiting. We want to control when
     * eclipselink acquires the connection. We to try to see Iron jac amar poping the "Stack" of keys of the inner
     * transaction.
     *
     * @return All primary keys in the database
     */
    public List<Integer> fetchAlIds() {
        return someEntityRepository.fetchAlIds();
    }

    /**
     * Persists the desired given number of entities. Each entity persisted is given dynamic string text compoesed of a
     * random int and an increasing loop number.
     *
     * @param numberOfEntitiesToCreate
     *            the number of entities to be persisted - this variable may or may influence the likelihood of
     *            reproducing the bug being verified.
     */
    public void createSomeEntityEntities(int numberOfEntitiesToCreate) {
        int randomInt = new Random().nextInt();
        for (int i = 0; i < Math.max(numberOfEntitiesToCreate, 0); i++) {
            String entityText = String.format("PersistLoop (RandomInt x loopIteration) = (%1$d,%2$d)", randomInt, i);
            SomeEntity someEntity = new SomeEntity();
            someEntity.setText(entityText);
            someEntityRepository.perist(someEntity);
        }
    }

    /**
     * Flushing is enough for us to initialize the first entity manager.
     */
    public void flush() {
        someEntityRepository.getEm().flush();
    }

}
