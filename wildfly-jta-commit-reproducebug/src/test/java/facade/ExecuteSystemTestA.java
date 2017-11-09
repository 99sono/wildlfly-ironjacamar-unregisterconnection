/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.management.remote.JMXConnector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import facade.systemtest.SystemTestFacade;
import facade.systemtest.TestASendMessageToQueueSystemTestImpl;
import util.jndi.WildflyJmxAccessorFactory;
import util.jndi.WildflyJndiUtil;

/**
 * A test class.
 */
public class ExecuteSystemTestA {

    /**
     * The EJB reference is not supposed to in relatedion to the deploy context (app) but relative to the real pure war
     * name.
     */
    private static final String WAR_NAME = "wildfly-jta-commit-reproducebug";

    /**
     * Remote EJB containing the business logic for the test.
     */
    SystemTestFacade testee;

    /**
     * Startup collaborators.
     */
    @Before
    public void before() {
        testee = WildflyJndiUtil.SINGLETON.resolveBean(WAR_NAME,
                TestASendMessageToQueueSystemTestImpl.class.getSimpleName(), SystemTestFacade.class);

    }

    /**
     * Invoke the container run test.
     */
    @Test
    public void executeTest() {
        testee.executeTest();
    }

    @Test
    public void checkJmxConnectionConfiguration() {
        JMXConnector result = WildflyJmxAccessorFactory.SINGLETON.createAJmxConntection();
        Assert.assertNotNull(result);
    }

}
