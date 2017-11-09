/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem.observer;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blogic.ReproduceBugService;
import inteceptor.FacadeInterceptor;
import startupconnectionproblem.HelperConstants;
import startupconnectionproblem.event.InitializeEmEvent;
import util.TransactionUtil;

/**
 * We do not want a JTA transaction behind this fire CDI event.
 */
@Singleton
@LocalBean
@Interceptors({ FacadeInterceptor.class })
public class InitializeEntityManagerObserverEjb {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitializeEntityManagerObserverEjb.class);

    @Inject
    ReproduceBugService reproduceBugService;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void observeStartupEventToTryToReproduceIJ000311Issue(
            @Observes InitializeEmEvent startupEventToTryToReproduceIJ000311Issue) {
        String transactionId = TransactionUtil.SINGLETON.getOngoingJtaTransactionId();
        LOGGER.info("STARTING {}: observing the CDI event InitializeEmEvent - The current transaction id is: {} ",
                HelperConstants.ISSUE_ELEMENT_DEPTH_THREE, transactionId);
        reproduceBugService.flush();
        LOGGER.info("Entity manager flush finished, the first entity manager has been created at this point.");
    }

}
