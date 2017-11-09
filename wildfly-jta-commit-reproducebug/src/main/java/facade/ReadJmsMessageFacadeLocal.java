/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.ejb.Local;

import messages.WorkDoneObjectMessage;

/**
 * A local EJB api that helps us read JMS messages put into reply queues. Normally MdB messages are simply read by the
 * container and put to run on the context of an Mdb. But a system test that awaits the reply to a JMS messages will
 * want to read out of a queue.
 *
 */
@Local
public interface ReadJmsMessageFacadeLocal {

    /**
     * Read a JMS message out of a queue. Opens a short lived JTA transaction to do the queue Read.
     *
     * @return null, if no message is available on the queue to be read, otherwise the message read out of the queue.
     */
    WorkDoneObjectMessage readWorkDoneJmsMessageNewJtaTransaction(String jndiQueueName, long timeoutMs);

    /**
     * True if a queue is empty at the current point in time.
     *
     * @param jndiQueueName
     *            the queue we want to check.
     */
    boolean isEmptyQueueNewJtaTransaction(String jndiQueueName);

    /**
     * Empties a queue out of messages by checking if the queue has any content, and when so running individual small
     * transactions to flush those messages out of queue.
     *
     * @param jndiQueueName
     *            The queue whose JMS messages we want to see discarded (typical test setup api).
     */
    void emptyQueue(String jndiQueueName);

}
