/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade.systemtest;

import static java.lang.String.format;
import static util.JndiNamesUtil.getJndiQueueNameForWorkDoneMessage;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import constants.ApplicationConstants;
import db.crud.SomeEntityRepository;
import facade.ExecutorFacadeLocal;
import facade.ReadJmsMessageFacadeLocal;

/**
 * A base class for our system tests
 */
public abstract class AbstractSystemTestFacadeImpl implements SystemTestFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSystemTestFacadeImpl.class);

    // common collaborators
    @Inject
    ExecutorFacadeLocal executorFacadeLocal;

    @Inject
    ReadJmsMessageFacadeLocal readJmsMessageFacadeLocal;

    @Inject
    SomeEntityRepository someEntityRepository;

    // apis ...

    /**
     * {@inheritDoc}
     *
     * We do not want a JTA transaction in the TEST logic. The test logic if needed should be allowed to rung for
     * several minutes.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void executeTest() {
        before();
        try {
            testLogic();
        } finally {
            after();
        }

    }

    /**
     * Test setup logic - before the test start
     */
    protected void before() {
        logHeaderOnServerLog(format("START TEST - %1$s", this.getClass().getSimpleName()));
        LOGGER.info("Cleanup - Going to discard any Work Done messages that may currently exist in the {} queue ",
                getJndiQueueNameForWorkDoneMessage());
        discardAllWorkDoneMessages();

        LOGGER.info("Cleanup - To discard all db entities", getJndiQueueNameForWorkDoneMessage());
        discardAllDbEntities();

        LOGGER.info("Cleanup - making sure no entities are found on the DB.");
        assertNumberOfEntitiesOnDbEqualsExpectations(0);

    }

    /**
     * Test setup logic - after the test ends
     */
    protected void after() {
        LOGGER.info("Cleanup - Going to discard any Work Done messages that may currently exist in the {} queue ",
                getJndiQueueNameForWorkDoneMessage());
        discardAllWorkDoneMessages();
        logHeaderOnServerLog(format("END TEST - %1$s", this.getClass().getSimpleName()));
    }

    /**
     * The test logic.
     */
    protected abstract void testLogic();

    // helper logic
    protected void logHeaderOnServerLog(String msg) {
        String header = format("%n%1$s%n%2$s%n%1$s", ApplicationConstants.LONG_SLASH_LINE, msg);
        LOGGER.info(header);
    }

    /**
     * Delete all entities in db
     */
    protected void discardAllDbEntities() {
        getExecutorFacadeLocal().executoreSynchronousNewJta(new Runnable() {
            @Override
            public void run() {
                someEntityRepository.deleteAll();
            }
        });
        LOGGER.info("All db entities have been deleted");
    }

    /**
     * When we start a test - or when we end a test we want to make sure we clean up after ourselves and do not let
     * messages stay on the Reply queue.
     */
    protected void discardAllWorkDoneMessages() {
        readJmsMessageFacadeLocal.emptyQueue(getJndiQueueNameForWorkDoneMessage());
    }

    // assertions

    /**
     * If a message we got out of a queue comes out null, it meands after the wait timeout we got nothing. Break the
     * system test.
     *
     * @param replyMessage
     *            The message payload that is expected to not be null.
     */
    protected void assertReplyNotEmpty(Object replyMessage) {
        if (replyMessage == null) {
            throw new RuntimeException("reply message is null - timeout waiting for reply.");
        }
    }

    /**
     * Count the number of entities in the DB and make sure that they match our expectations
     *
     * @param expectedNumberOfEntities
     *            the expected number of entities to be found in the DB.
     */
    protected void assertNumberOfEntitiesOnDbEqualsExpectations(int expectedNumberOfEntities) {
        final List<Integer> result = getAllSomeEntityPrimaryKeys();

        boolean numberOfRecordsMatchesExpectations = result.size() == expectedNumberOfEntities;
        if (!numberOfRecordsMatchesExpectations) {
            String errMsg = format("Expected a total of %1$s db entities but found %2$s", expectedNumberOfEntities,
                    result.size());
            LOGGER.error(errMsg);
            throw new RuntimeException(errMsg);
        }
        LOGGER.info("Expectations were met - a total of {} entities were found in the db", expectedNumberOfEntities);
    }

    /**
     * Fetch all ids in the DB.
     */
    protected List<Integer> getAllSomeEntityPrimaryKeys() {
        final List<Integer> result = new ArrayList<>();
        executorFacadeLocal.executoreSynchronousNewJta(new Runnable() {
            @Override
            public void run() {
                result.addAll(someEntityRepository.fetchAlIds());
            }
        });
        return result;
    }

    // boiler plate code
    /**
     * Utility facade
     */
    public ExecutorFacadeLocal getExecutorFacadeLocal() {
        return executorFacadeLocal;
    }

    /**
     * @return provide utility ejb to read messages out of a queue in system tests.
     */
    public ReadJmsMessageFacadeLocal getReadJmsMessageFacadeLocal() {
        return readJmsMessageFacadeLocal;
    }

}
