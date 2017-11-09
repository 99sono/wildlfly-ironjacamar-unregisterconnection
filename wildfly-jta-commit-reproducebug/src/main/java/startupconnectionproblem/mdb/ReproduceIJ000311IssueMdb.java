/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blogic.ReproduceBugService;
import startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb;
import util.TransactionUtil;

/**
 * We do not want a JTA transaction behind this fire CDI event.
 */
@MessageDriven(name = "ReproduceIJ000311IssueMdb", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = ReproduceIJ000311IssueMdb.SAMPLE_QUEUE_1_JNDI_NAME),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class ReproduceIJ000311IssueMdb implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReproduceIJ000311IssueMdb.class);

    public static final String SAMPLE_QUEUE_1_JNDI_NAME = "java:/queue/SampleQueue1";
    public static final String JMS_XA_QUEUE_CONNECTION_FACTORY_JNDI_NAME = "java:/JmsXA";
    public static final String HEADER_FORMAT = "%n--------------------------------------%n" //
            + "-- %1$s %n" //
            + "--------------------------------------%n%n%n";

    @Inject
    EventObserverNestedJtaTransactionEjb eventObserverNestedJtaTransactionEjb;

    @Inject
    ReproduceBugService reproduceBugService;

    @Override
    public void onMessage(Message message) {
        LOGGER.info(getHeaderString("MDB has started running"));

        LOGGER.info("Doing a fetch ALL query. just to bind the connection manager to the KEY of stack level 1");
        reproduceBugService.fetchAlIds();

        eventObserverNestedJtaTransactionEjb.runNestedTransaction();
        LOGGER.info("MDB transaction will finish now.");
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
