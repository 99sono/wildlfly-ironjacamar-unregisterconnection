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
public class TestASendMessageToQueueSystemTestImpl extends AbstractSystemTestFacadeImpl implements SystemTestFacade {

    @Inject
    SendJmsMessageFacadeLocal sendJmsMessageFacadeLocal;

    /**
     * Simply log that a message has been sent to a queue.
     */
    @Override
    protected void testLogic() {
        // send message to server
        logHeaderOnServerLog("Going to send a JMS to back-end system so entities get created.");
        final int queueNumber = 1;
        final String jndiQueueName = getJndiQueueNameForReproduceBugQueue(queueNumber);
        sendJmsMessageFacadeLocal.sendDoWorkJmsMessageNewJtaTransaction("jms Message Content Irrelevant",
                jndiQueueName);

        // wait for the server to reply back to us
        logHeaderOnServerLog("Going to wait for reply from back-end system confirming job done");
        WorkDoneObjectMessage objectMessagePayload = getReadJmsMessageFacadeLocal()
                .readWorkDoneJmsMessageNewJtaTransaction(JndiNamesUtil.getJndiQueueNameForWorkDoneMessage(),
                        ApplicationConstants.DEFAULT_WAIT_TIME_FOR_BACK_END_BUSINESS_LOGIC_TO_COMMIT_MS);
        assertReplyNotEmpty(objectMessagePayload);
        assertNumberOfEntitiesOnDbEqualsExpectations(ApplicationConstants.NUMBER_OF_SOME_ENTITY_ENTITIES_TO_CREATE);

    }

}
