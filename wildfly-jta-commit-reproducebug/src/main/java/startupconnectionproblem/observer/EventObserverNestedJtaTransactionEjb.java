/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem.observer;

import java.util.Date;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blogic.ReproduceBugService;
import facade.ExecutorFacadeLocal;
import startupconnectionproblem.HelperConstants;
import startupconnectionproblem.event.StartupEventToTryToReproduceIJ000311Issue;
import util.SendJMSMessageUtil;
import util.TransactionUtil;

/**
 * We do not want a JTA transaction behind this fire CDI event.
 */
@Singleton
@LocalBean
public class EventObserverNestedJtaTransactionEjb {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventObserverNestedJtaTransactionEjb.class);

    private static final String SAMPLE_QUEUE_1_JNDI_NAME = "java:/queue/SampleQueue1";
    private static final String JMS_XA_QUEUE_CONNECTION_FACTORY_JNDI_NAME = "java:/JmsXA";

    public static final String HEADER_FORMAT = "%n--------------------------------------%n" //
            + "-- %1$s %n" //
            + "--------------------------------------%n%n%n";

    @Inject
    ReproduceBugService reproduceBugService;

    @Inject
    ExecutorFacadeLocal executorFacadeLocal;

    @Inject
    private SendJMSMessageUtil sendJMSMessageUtilBean;

    @Resource(mappedName = JMS_XA_QUEUE_CONNECTION_FACTORY_JNDI_NAME)
    private ConnectionFactory connectionFactory;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void observeStartupEventToTryToReproduceIJ000311Issue(
            @Observes StartupEventToTryToReproduceIJ000311Issue startupEventToTryToReproduceIJ000311Issue) {
        // No need to use MDB for this
        // LOGGER.info("Sending message to queue");
        // cachedConnectionManagerImpl_stack1_sendMessageToQueue();

        runNestedTransactionImpl();
    }

    /**
     * API that was invoked by our MDB on the first experimentations but that is not really needed to demonstrate the
     * problem.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void runNestedTransaction() {
        runNestedTransactionImpl();
    }

    protected void runNestedTransactionImpl() {
        // (a) Fetch the current ontoing transaction id
        final String transactionId = getTransactionId();
        String headerString = getHeaderString(String.format(
                "STARTING %1$s: observing the CDI event StartupEventToTryToReproduceIJ000311Issue ", transactionId));
        LOGGER.info(headerString);

        // (b) Do a fetch all ids query.
        // NOTE: This forces eclipselink to ask for a connection from the WildflyDatasource but immediately returns it
        // since no Entities are accessed. There is no need to keep the connection in hand for any longer than the
        // request
        // NOTE: But what happens with this statment that is fundamental is that on ok the
        // org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl the
        // key.getCMToConnectionsMap() is given for a short a conneciton records that is quickly removed out
        LOGGER.info("Doing a fetch ALL query. just to bind the connection manager to the KEY of stack level 1.");
        reproduceBugService.fetchAlIds();
        LOGGER.info("At this point the org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl will have " //
                + " registered on stack1 the connection used by eclipselink, and unregistered as well.\n" //
                + " However, this temporary register/de-register is sufficient to add to the current key.getCMToConnectionsMap() " //
                + " and entry for the JDBC connection manger.\n " //
                + " This is fundamental, because without this step the exception that we will later see" //
                + " would be hidden away - even though the logical problem would be there no exception would be logged. " //
                + " By taking this magical step here, we have ensured that we will be able to reproduce the excpetion later. \n " //
                + "  To summarize what has happend so far.\n" //
                + " 1. We have forced eclipselink to register and unregister a connection due to are fetch id query \n" //
                + " 2. The JTA transaction ID {}  continues healthy and running without any problem" //
                + " 3. Meanwhile internally, our CachedConnectionManagerImpl, for this thread, has a single task of keys in its currentObjects field \n " //
                + " 4. The leading key on this stack does not hold any connectionrecords (eclipselink returned the connecton to the pool) but it does have a KEY for the JDBC connection manager"
                + " 5. Point 4 is the magical step to allow us to see the stack trace but not the core of the issue - just the core of seing the stack trace \n" //
                , transactionId);

        // (b) Now we want to make sure that some sort of connection is accessed in this same - but outer - jta
        // transaction
        // we will send a message to a queue to have the ActiveMq connection being registered in stack 1
        // cachedConnectionManagerImpl_stack1_sendMessageToQueue();

        // (c) Now open a nested JTA transaction - but that is the same JTA transaction
        // and in this jta transaction eclipselink with do a flush query to ensure that the eclipselnk
        // is forced to acquire a connection from the container withing STACK 2
        this.executorFacadeLocal.executoreSynchronousSameJta(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("START: executoreSynchronousSameJta ");
                LOGGER.info("Right now we are within the same JTA transaction. But on a NESTED EJB call.\n "
                        + " What technically has happened at this point is that IRON JAC AMAR now has a second stack (keyAssociation)\n "
                        + " in the currentObjects. We add in stacks each time we go through an EJB evne if we keep the JTA transaction\n "
                        + " and this is the BUG. But we will see more about this later.\n ");
                create50Entities_stack2();

                LOGGER.info(
                        "OK. Now we are about to leave our nested EJB call. What is happening now is very important. \n"
                                + "1. We will leave a nested EJB call but not the JTA transaction "
                                + "2. Wildfly will go to the cachedConnectonManagerImpl and tell it to pop a stack (BUG) "
                                + "3. The stack that goes away is actually holding the JDBC connection thta has been given to ECLIPSELINK and thta eclipselink will later try to close "
                                + "4. When we leave this call we go back to STACK 1, where no connections are being managed");
                LOGGER.info(" FINISH: executoreSynchronousSameJta:\n\n ");
            }
        });

        // (d) Now we log some explanation data
        LOGGER.warn("NOW we are out of our nested EJB call. We are bout to leave the main EJB transaction call. \n"
                + "In short we are about to reproduce the stack trace. \n" + "What will now happen is the following. \n"
                + "1. Wildfly will tell eclipselink that the transaction is at an end and it should commit if it can \n"
                + "2. Eclipselink does its job of committing the changes and takes the JTA connection it was given and tries to close it \n"
                + "3. IRON JAC AMAR will log the stack trace we wanted to reproduce saying it has not idea what connection we are trying to return to the pool. \n"
                + "4. The reason the CachedConnectionManagerImpl does not know about the connection is because it was managed under STACK 2 but was popped out of existence after the nested transaction \n "
                + "5. Now it is very important to keep in mind that even if we were not seing the stack trace  the error would be there (the moment we popped a key stack that had still connections) "
                + "6. Keep in mind that if we were to modify the source code  and disable our reproduceBugService.fetchAlIds() call"
                + " we would see no stack trace but the same issue would be present."
                + "7. The difference that doing the reproduceBugService.fetchAlIds() does is whether or not CachedConnectionManagerImpl during unregister connection"
                + " leaves via the if conns == null return null or if goes further into the if (!ignoreConnections)\n "
                + "8. OK - Now that we understand what is happening behind the scenes we can let eclipselink try to close the JDBC connection at the end of the JTA transaction\n\n\n\n "
                + "");
    }

    protected void cachedConnectionManagerImpl_stack1_sendMessageToQueue() {
        final String transactionId = getTransactionId();
        LOGGER.info(
                "STARTING: {} Going to put a message into QUEUE: {} . The point is to force a Connection to exist in 'KeyConnectionAssociation key' of STACK 1 for current jta context.  ",
                transactionId, SAMPLE_QUEUE_1_JNDI_NAME);
        String message = String
                .format("Pump a dummy message to a queue to force a connection to be added to the current transaction stack key on CachedConnectionManagerImpl"
                        + " Current Date is: %1$s ", new Date().toString());
        LOGGER.info(message);
        Queue queue = sendJMSMessageUtilBean.lookupQueueDynamically(SAMPLE_QUEUE_1_JNDI_NAME);
        sendJMSMessageUtilBean.sendToQueueDoWorkMessage(connectionFactory, queue, message);
        LOGGER.info(
                "ENDING: {} message sento to QUEUE: {} . The point is to force a Connection to exist in 'KeyConnectionAssociation key' of STACK 1 for current jta context.  ",
                transactionId, SAMPLE_QUEUE_1_JNDI_NAME);
    }

    protected void create50Entities_stack2() {
        // (a) Fetch the current ontoing transaction id
        String transactionId = TransactionUtil.SINGLETON.getOngoingJtaTransactionId();
        LOGGER.info(
                "STARTING {}: observing the CDI event StartupEventToTryToReproduceIJ000311Issue - The current transaction id is: {} ",
                HelperConstants.ISSUE_ELEMENT_DEPTH_THREE, transactionId);

        // (b) Create some entities in the database
        LOGGER.info("Going to create 50 entities on the database");
        reproduceBugService.createSomeEntityEntities(50);
        LOGGER.info("50 entities should have been created: \n");

        // (c) Now we flush to force the acquisition of the java:/jdbc/SAMPLE_DS
        LOGGER.info(
                "Now we want to flush - we want to force eclipselink to have to acquire the java:/jdbc/SAMPLE_DS and not be allowed to close the connection yet. ");
        reproduceBugService.flush();
        LOGGER.info(
                "The flush has been done. We expec that the log now shows us that java:/jdbc/SAMPLE_DS connection was acquired but not yet closed by eclipselink. ");

        // (d) log the end of the observe logic
        LOGGER.info("ENDING {}: finishing to observer. The current transaction id is: {} ", transactionId);
    }

    /**
     * @return The transaction id
     */
    protected String getTransactionId() {
        return TransactionUtil.SINGLETON.getOngoingJtaTransactionId();
    }

    /**
     * Build a big fat header string
     *
     * @param headerContent
     *            the title of the header
     * @return a fat header string
     */
    protected String getHeaderString(String headerContent) {
        return String.format(HEADER_FORMAT, headerContent);
    }

}
