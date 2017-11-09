/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package startupconnectionproblem.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We do not want a JTA transaction behind this fire CDI event.
 */
@ApplicationScoped
public class FireStartupEventsServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireStartupEventsServiceImpl.class);

    @Inject
    Event<StartupEventToTryToReproduceIJ000311Issue> startupEventToTryToReproduceIJ000311Issue;

    @Inject
    Event<InitializeEmEvent> initializeEmEvent;

    /**
     * This public api shall be opening an implicit new jta transaction
     */
    public void fireStartupCDIEvents() {

        // Fire entity manager event
        LOGGER.info("START: InitializeEmEvent ");
        initializeEmEvent.fire(new InitializeEmEvent());
        LOGGER.info("FINISH: InitializeEmEvent  ");

        // fire the event
        LOGGER.info("START: StartupEventToTryToReproduceIJ000311Issue ");
        startupEventToTryToReproduceIJ000311Issue.fire(new StartupEventToTryToReproduceIJ000311Issue());
        LOGGER.info("FIINSH: StartupEventToTryToReproduceIJ000311Issue ");

    }

}
