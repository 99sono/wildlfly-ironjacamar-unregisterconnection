/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import startupconnectionproblem.HelperConstants;

/**
 * A utility class to give us some information about the ongoing jta transaction.
 */
public class TransactionUtil {

    public static final TransactionUtil SINGLETON = new TransactionUtil();

    private TransactionUtil() {

    }

    /**
     * @return The transaction id of an ongoing transaction
     */
    public String getOngoingJtaTransactionId() {
        Transaction transaction = getOngoingJtaTransaction();
        if (transaction != null) {
            return transaction.toString();
        } else {
            return "NO-Transaction is currently active";
        }
    }

    /**
     * @return The ongoing jta transaction
     */
    public Transaction getOngoingJtaTransaction() {
        try {
            TransactionManager transactionManager = acquireTransactionManager();
            return transactionManager.getTransaction();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error trying to get transaction", e);
        }
    }

    /**
     * Obtain the container transaction manager.
     *
     * @return The container transaction manager (e.g. com.arjuna.ats.jbossatx.jta.TransactionManagerDelegate)
     * @throws Exception
     *             Unexpected error takes place during jndi lookup
     */
    public TransactionManager acquireTransactionManager() throws Exception {
        try {
            return InitialContext.doLookup(HelperConstants.JBOSS_TRANSACTION_MANAGER);
        } catch (NamingException ex) {
            String errMsg = createErrMsgForFailedJndiLookup(ex, HelperConstants.JBOSS_TRANSACTION_MANAGER);
            throw new RuntimeException(errMsg, ex);
        }
    }

    private String createErrMsgForFailedJndiLookup(NamingException namingException, String jndiLookupName) {
        return String.format(
                "Unexpected error took place while attempting to lookup the container. %n"
                        + " Jndi-lookup-name: %1$s %n" + " Error was: %2$s. ",
                jndiLookupName, namingException.getMessage());

    }

}
