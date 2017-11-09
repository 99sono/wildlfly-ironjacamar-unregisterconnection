/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import inteceptor.FacadeInterceptor;
import util.SendJMSMessageUtil;

/**
 * A Stateless EJB that helps us send messages into queues.
 *
 * <P>
 * Pay special attention to the {@link TransactionAttribute} annotations behind every mthod. When we are stimulating the
 * MdB business logic we will want to open short lived JTA transactions. When we want the MdB business logic to report
 * back to us that it is done working we want the Jms message to be committed as part of the ongoing parent JTA
 * transaction.
 */
@Stateless
@Interceptors(FacadeInterceptor.class)
public class SendJmsMessageFacadeImpl implements SendJmsMessageFacadeLocal {

    @Resource(mappedName = "java:/jms/JcomJmsFactory")
    private ConnectionFactory connectionFactory;

    /*
     * Thrown away we lookup the queue to send dynamically
     *
     * @Resource(lookup = "queue/Cran30AvSendQueue") private Queue queue;
     */

    /**
     * Helps us pump messages into queues.
     */
    @Inject
    private SendJMSMessageUtil sendJMSMessageUtilBean;

    // system tests sending message into the Mdb Queues

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void sendDoWorkJmsMessageNewJtaTransaction(String message, String jndiQueueName) {
        Queue queue = sendJMSMessageUtilBean.lookupQueueDynamically(jndiQueueName);
        sendJMSMessageUtilBean.sendToQueueDoWorkMessage(connectionFactory, queue, message);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Asynchronous
    @Override
    public void sendDoWorkJmsMessageAsynchronousNewJtaTransaction(String message, String jndiQueueName) {
        Queue queue = sendJMSMessageUtilBean.lookupQueueDynamically(jndiQueueName);
        sendJMSMessageUtilBean.sendToQueueDoWorkMessage(connectionFactory, queue, message);
    }

    // mdbs reporting back to the system test that their work is done ... the message should be committed with the
    // business transaction
    /**
     * Pu
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    @Override
    public void sendWorkDoneJmsMessageRequiresTransaction(String jndiQueueName) {
        Queue queue = sendJMSMessageUtilBean.lookupQueueDynamically(jndiQueueName);
        sendJMSMessageUtilBean.sendToQueueWorkDoneMessage(connectionFactory, queue);
    }

}
