/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.ejb.Local;

/**
 *
 * A generic EJB so that we can reduce in some about the number mushrooming EJBs by having a prettyg generic EJB that
 * opens a transaction when needed.
 */
@Local
public interface ExecutorFacadeLocal {

    /**
     * Execute the given logic under a new JTA transaction.
     */
    void executoreSynchronousNewJta(Runnable executeLogic);

    /**
     * Execute the given logic under the Same jta transaction. The transaction attribute is "supports" meaning - if an
     * ongoing jta transaction context exists it continues being used.
     */
    void executoreSynchronousSameJta(Runnable executeLogic);

    /**
     * Execute the given logic making sure no JTA transaction context is present
     */
    void executoreSynchronousNoJta(Runnable executeLogic);

    /**
     * Execute the given logic making sure no JTA transaction context is present
     */
    void executoreAsynchronousNoJta(Runnable executeLogic);
}
