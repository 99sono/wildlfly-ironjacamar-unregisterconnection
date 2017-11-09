package util;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.naming.InitialContext;

/**
 * Reads messages out of queues.
 *
 * Reading out of queue is normally done for free when an MDB gets used. But on a system test that wants to wait for a
 * reply to a sent message, this is needed.
 */
@ApplicationScoped
public class ReadJMSMessageUtil {
    /**
     * Create a new SendJMSMessageUtil.
     */
    public ReadJMSMessageUtil() {
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

    @SuppressWarnings("unchecked")
    public <T extends Message> T readFromQueue(ConnectionFactory connectionFactory, Queue queue, long timeoutMs,
            Class<T> jmsMessageType) {

        Connection connection = null;
        Session session = null;
        try {

            // creating a queue connection
            connection = connectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            startJmsConnection(connection);
            MessageConsumer receiver = createReceiver(session, queue);

            // read the message out of the queue
            LOGGER.info("receiving...");
            Message message = receiver.receive(timeoutMs);
            if (message == null) {
                return null;
            }
            return (T) message;
        } catch (JMSException e) {
            throw new RuntimeException("Unexpected jms exception took place.", e);
        } finally {
            try {
                closeSession(session);
            } finally {
                closeConnection(connection);
            }
        }
    }

    /**
     * Check if a queue is empty of messages or not.
     */
    public boolean isEmptyQueue(ConnectionFactory connectionFactory, Queue queue) {
        Connection connection = null;
        Session session = null;
        QueueBrowser browser = null;
        try {
            // creating a queue connection
            connection = connectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            startJmsConnection(connection);
            browser = session.createBrowser(queue);
            return !browser.getEnumeration().hasMoreElements();
        } catch (JMSException e) {
            throw new RuntimeException("Unexpected jms exception took place.", e);
        } finally {
            try {
                try {
                    closeBrowser(browser);
                } finally {
                    closeSession(session);
                }
            } finally {
                closeConnection(connection);
            }
        }

    }

    private static void startJmsConnection(Connection connection) {
        try {
            connection.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start connection with hashCode: " + connection.hashCode(), e);
        }
    }

    private static MessageConsumer createReceiver(Session session, Queue queue) throws JMSException {
        return session.createConsumer(queue);
    }

    private void closeBrowser(QueueBrowser browser) {
        if (browser != null) {
            try {
                browser.close();
            } catch (JMSException e) {
                LOGGER.log(java.util.logging.Level.SEVERE, "exception while closing browser to jms.", e);
            }
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
