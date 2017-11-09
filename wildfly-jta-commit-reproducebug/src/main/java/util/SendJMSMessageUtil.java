package util;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;

import messages.DoWorkObjectMessage;
import messages.WorkDoneObjectMessage;

/**
 * Sends messages to queues.
 */
@ApplicationScoped
public class SendJMSMessageUtil {
    /**
     * Create a new SendJMSMessageUtil.
     */
    public SendJMSMessageUtil() {
        super();
    }

    private static final Logger LOGGER = Logger.getLogger(JndiNamesUtil.class.getCanonicalName());

    /**
     * Gien a jndi queue name lookup the queue resource in the container.
     *
     * @param jndiQueueName
     *            the jndi queue name
     * @return the respective queue resource, when found.
     */
    public Queue lookupQueueDynamically(String jndiQueueName) {
        try {
            InitialContext jndiContext = new InitialContext();
            return (Queue) jndiContext.lookup(jndiQueueName);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to look up queue %1$s ", jndiQueueName), e);
        }
    }

    /**
     * Api to send data to a message where typically, there should be an Message driven bean plugged in.
     */
    public void sendToQueueDoWorkMessage(ConnectionFactory connectionFactory, Queue targetQueue, String message) {
        DoWorkObjectMessage payload = new DoWorkObjectMessage(message);
        sendToQueue(connectionFactory, targetQueue, payload);
    }

    /**
     * APi to send data to a message queue where typically a system test will be plugging waiting to the server reply to
     * check some conditions (e.g. take a peek a the DB state).
     */
    public void sendToQueueWorkDoneMessage(ConnectionFactory connectionFactory, Queue targetQueue) {
        WorkDoneObjectMessage payload = new WorkDoneObjectMessage();
        sendToQueue(connectionFactory, targetQueue, payload);
    }

    /**
     * Generic send message to queue API - has no idea of the nature of the object message payload.
     *
     * <P>
     * In order to have the JMS message sent immediately, ensure the invoker of the API uses
     * {@link TransactionAttributeType#REQUIRES_NEW}, otherwise, the message is only sent out when the parent JTA
     * transaction (e.g. that of the mdb) is commited by the container.
     */
    private void sendToQueue(ConnectionFactory connectionFactory, Queue targetQueue, Serializable payload) {
        Connection connection = null;
        Session session = null;

        try {
            // (a) prepare resources to pump a message out
            // note: the session we create is create with ack mode "transacted" which means regardless of the connection
            // close and session close the data we send is only actually sent when the Continer commits all JTA
            // resources
            connection = connectionFactory.createConnection();
            session = connection.createSession(javax.jms.Session.SESSION_TRANSACTED);
            MessageProducer messageProducer = ((Session) session).createProducer(targetQueue);

            // (b) create our object message with information about the date when the message got created
            ObjectMessage objectMessage = session.createObjectMessage(payload);

            // (c) let the message get sent
            messageProducer.send(objectMessage);

        } catch (JMSException e) {
            throw new RuntimeException("unkown error in jms", e);
        } finally {
            closeSession(session);
            closeConnection(connection);
        }
    }

    private void closeSession(Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "exception while closing session to jms.", e);
            }
        }
    }

    private void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "error closing connection", e);
            }
        }
    }

}
