/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade.systemtest;

import javax.ejb.Remote;

/**
 * Remote EJB interface - triggering different system tests.
 */
@Remote
public interface SystemTestFacade {

    /**
     * Execute the business logic of the test.
     */
    void executeTest();
}
