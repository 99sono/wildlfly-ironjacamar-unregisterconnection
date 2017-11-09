/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.ejb.Local;

import messages.DoWorkObjectMessage;
import messages.WorkDoneObjectMessage;

/**
 * A local EJB api that helps us pump messages into queues for different purposes (e.g. stimulating Mdbs or reporting
 * back to a test case)
 *
 */
@Local
public interface SendJmsMessageFacadeLocal {

    // Do Work Messages .... stimulate deployed Mdbs
    /**
     * send JMS message that will produce an object message with payload {@link DoWorkObjectMessage}. This is a typical
     * message we want to send to an MdB to have the mdb do something appropriate to our bug detection back-end logic.
     *
     * <P>
     * The message is sent as part of a NEW JTA transaction. Typically this is what we want to do, since our system test
     * stimulating an MDB is not running behind a JTA transaction.
     */
    void sendDoWorkJmsMessageNewJtaTransaction(String message, String jndiQueueName);

    /**
     * same {@link SendJmsMessageFacadeLocal#sendDoWorkJmsMessageNewJtaTransaction(String, String)} but asynchronously
     * in this case.
     *
     * <P>
     * The message is sent as part of a NEW JTA transaction. Typically this is what we want to do, since our system test
     * stimulating an MDB is not running behind a JTA transaction.
     */
    void sendDoWorkJmsMessageAsynchronousNewJtaTransaction(String message, String jndiQueueName);

    // Work Done Messages ... reply back to the system test that the work of the Mdb is done.
    /**
     * send JMS message that will produce an object message with payload {@link WorkDoneObjectMessage}. This is a
     * typical message we want to send to a queue on which a System test will be waiting for a reply.
     *
     * <P>
     * The message is sent as part of an ongoing JTA transaction. Typically this is what we want to do, back-end
     * business logic only wants to inform the system test that the processing is actually done when its JTA transaction
     * is committed at the end of the back-end logic. We want the DB changes and the JMS changes to be committed
     * together.
     */
    void sendWorkDoneJmsMessageRequiresTransaction(String jndiQueueName);
}
