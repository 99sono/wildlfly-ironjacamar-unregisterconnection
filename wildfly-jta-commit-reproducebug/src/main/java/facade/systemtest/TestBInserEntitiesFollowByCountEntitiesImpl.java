/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade.systemtest;

import static util.JndiNamesUtil.getJndiQueueNameForReproduceBugQueue;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import constants.ApplicationConstants;
import facade.SendJmsMessageFacadeLocal;
import inteceptor.FacadeInterceptor;
import messages.WorkDoneObjectMessage;
import util.JndiNamesUtil;

/**
 * A basic system test we use simply to verify we are able to send message to queue.
 */
@LocalBean
@Stateless
@Interceptors(FacadeInterceptor.class)
public class TestBInserEntitiesFollowByCountEntitiesImpl extends AbstractSystemTestFacadeImpl
        implements SystemTestFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestBInserEntitiesFollowByCountEntitiesImpl.class);

    @Inject
    SendJmsMessageFacadeLocal sendJmsMessageFacadeLocal;

    /**
     * Simply log that a message has been sent to a queue.
     */
    @Override
    protected void testLogic() {
        for (int i = 0; i < 2000; i++) {
            runLogIterationNumber(i + 1);
        }

    }

    /**
     * Runs an iteration of create entities and check the number of entities in the DB.
     *
     * @param iterationNumber
     *            The current iteration number 1-N
     */
    protected void runLogIterationNumber(int iterationNumber) {
        // send message to server
        logHeaderOnServerLog("Going to send a JMS to back-end system so entities get created. Current Iteration = "
                + iterationNumber);
        final int queueNumber = 1;
        final String jndiQueueName = getJndiQueueNameForReproduceBugQueue(queueNumber);
        sendJmsMessageFacadeLocal.sendDoWorkJmsMessageNewJtaTransaction("jms Message Content Irrelevant",
                jndiQueueName);

        // wait for the server to reply back to us
        int expectedNumberOfEntitiesAfterProcessingMessage = ApplicationConstants.NUMBER_OF_SOME_ENTITY_ENTITIES_TO_CREATE
                * iterationNumber;
        logHeaderOnServerLog(
                "Going to wait for reply from back-end system confirming job done. We expect that when we get the reply the DB shows numberOfEntities = "
                        + expectedNumberOfEntitiesAfterProcessingMessage);
        WorkDoneObjectMessage objectMessagePayload = getReadJmsMessageFacadeLocal()
                .readWorkDoneJmsMessageNewJtaTransaction(JndiNamesUtil.getJndiQueueNameForWorkDoneMessage(),
                        ApplicationConstants.DEFAULT_WAIT_TIME_FOR_BACK_END_BUSINESS_LOGIC_TO_COMMIT_MS);
        assertReplyNotEmpty(objectMessagePayload);
        assertNumberOfEntitiesOnDbEqualsExpectations(expectedNumberOfEntitiesAfterProcessingMessage);
    }

}
