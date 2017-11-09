/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem.event;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import startupconnectionproblem.HelperConstants;

/**
 * A singleton EJB that the container must create during startup.
 */
@Singleton
@Startup
@LocalBean
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StartupSingletonLocalEjb {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupSingletonLocalEjb.class);
    @Inject
    FireStartupEventsServiceImpl fireStartupEventsServiceImpl;

    @PostConstruct
    public void postConstrcut() {
        LOGGER.info("STARTING {}:Post construct of startup bean singleton starting",
                HelperConstants.ISSUE_ELEMENT_DEPTH_ONE);
        fireStartupEventsServiceImpl.fireStartupCDIEvents();
        LOGGER.info("ENDING {}: Post construct of startup bean singleton ending",
                HelperConstants.ISSUE_ELEMENT_DEPTH_ONE);
    }

}
