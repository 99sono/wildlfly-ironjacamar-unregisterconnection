/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem;

/**
 * Helper interface
 */
public interface HelperConstants {

    /**
     * Jndi name to lookup the container transaction manager (e.g.
     * com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate).
     *
     * <P>
     * The outcome of this lookup is expected to be an object of {@link javax.transaction.TransactionManager}
     *
     * @see javax.transaction.TransactionManager
     */
    String JBOSS_TRANSACTION_MANAGER = "java:jboss/TransactionManager";

    /**
     * Jndi name to lookup the container transaction synchronization registry (e.g.
     * org.jboss.as.txn.service.internal.tsr.TransactionSynchronizationRegistryWrapper).
     *
     * <P>
     * Motivation: <br>
     * Eclipselink wishes to bind to specific life cycles of a JTA transaction, e.g. it cares about the completion of a
     * transaction to synchronize changes on a unit of work with the server session cache. There are two ways by which
     * eclipselink can do this. One is by registering using the
     * {@link javax.transaction.Transaction#registerSynchronization(javax.transaction.Synchronization)}. This is the
     * traditional approach followed by eclipselink. It has the disadvantage that it cannot guarantee that its logic
     * will take place before, for example, the CDI Synchroinizaton components. <br>
     * Another approach, is to register itself via
     * {@link javax.transaction.TransactionSynchronizationRegistry#registerInterposedSynchronization(javax.transaction.Synchronization)}.
     * This latter API would allow to prioritize the logic of the eclipselink synchronization on the container.
     *
     *
     *
     *
     * <P>
     * The outcome of this lookup is expected to be an object of
     * {@link javax.transaction.TransactionSynchronizationRegistry}
     *
     * @see javax.transaction.TransactionSynchronizationRegistry
     */
    String JTA_TRANSACTION_SYNCHRONIZATION_REGISTRY = "java:comp/TransactionSynchronizationRegistry";

    int ISSUE_ELEMENT_DEPTH_ONE = 1;
    int ISSUE_ELEMENT_DEPTH_TWO = 2;
    int ISSUE_ELEMENT_DEPTH_THREE = 3;
    int ISSUE_ELEMENT_DEPTH_FOUR = 4;
    int ISSUE_ELEMENT_DEPTH_FIVE = 5;

}
