/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.jms.ConnectionFactory;
import javax.jms.ObjectMessage;
import javax.jms.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import inteceptor.FacadeInterceptor;
import messages.WorkDoneObjectMessage;
import util.ReadJMSMessageUtil;

/**
 * A Stateless EJB that helps us read messages out of queues.
 *
 * <P>
 * Pay special attention to the {@link TransactionAttribute} annotations behind every method. When a system test is
 * reading a reply to a request it sent it will typically want to open a transaction and commit it as soon as the JMS
 * message is in its hands.
 */
@Stateless
@Interceptors(FacadeInterceptor.class)
public class ReadJmsMessageFacadeImpl implements ReadJmsMessageFacadeLocal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadJmsMessageFacadeImpl.class);
    /**
     * We know there is a message in the queue and it is unlikely we will be stuck waiting for the read out of the
     * queue.
     */
    private static final long IRRELEVANT_TIME_OUT_MS = 600;

    @Resource(mappedName = "java:/jms/JcomJmsFactory")
    private ConnectionFactory connectionFactory;

    /**
     * Helps us Read messages from queues.
     */
    @Inject
    private ReadJMSMessageUtil readJMSMessageUtilBean;

    @Inject
    private ExecutorFacadeLocal executorFacadeLocal;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public WorkDoneObjectMessage readWorkDoneJmsMessageNewJtaTransaction(String jndiQueueName, long timeoutMs) {
        Queue queue = readJMSMessageUtilBean.lookupQueueDynamically(jndiQueueName);
        ObjectMessage objectMessage = readJMSMessageUtilBean.readFromQueue(connectionFactory, queue, timeoutMs,
                ObjectMessage.class);
        try {
            return objectMessage == null ? null : (WorkDoneObjectMessage) objectMessage.getObject();
        } catch (Exception e) {
            // note: we log the exception but let the transaction commit - we do not want to be poisoning the queue
            // we want to empty out the queue
            LOGGER.error("Unexpected error took place attempting to the obtain the payload out of the object message.",
                    e);
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public boolean isEmptyQueueNewJtaTransaction(String jndiQueueName) {
        Queue queue = readJMSMessageUtilBean.lookupQueueDynamically(jndiQueueName);
        return readJMSMessageUtilBean.isEmptyQueue(connectionFactory, queue);
    }

    /**
     * {@inheritDoc}
     *
     * JTA transactions are managed internally using our convenience executor facade.
     *
     * @param jndiQueueName
     *            a queue we want to empty out of messages.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void emptyQueue(String jndiQueueName) {
        while (!internalIsEmptyQueueNewJtaTransaction(jndiQueueName)) {
            internalConsumeOneMessageOutOfQueue(jndiQueueName);
        }
    }

    // helper logic ... open JTA transactions internally on-demand

    /**
     * @param jndiQueueName
     *            the queue we know to have a message and out of which we want to discard a message.
     */
    private void internalConsumeOneMessageOutOfQueue(final String jndiQueueName) {
        executorFacadeLocal.executoreSynchronousNewJta(new Runnable() {
            @Override
            public void run() {
                readWorkDoneJmsMessageNewJtaTransaction(jndiQueueName, IRRELEVANT_TIME_OUT_MS);
            }
        });
    }

    /**
     * Open a short lived transaction to check if a queue is open.
     *
     * @param jndiQueueName
     * @return
     */
    private boolean internalIsEmptyQueueNewJtaTransaction(final String jndiQueueName) {
        final List<Boolean> result = new ArrayList<>();
        executorFacadeLocal.executoreSynchronousNewJta(new Runnable() {
            @Override
            public void run() {
                result.add(isEmptyQueueNewJtaTransaction(jndiQueueName));
            }
        });
        return result.get(0);
    }

}
