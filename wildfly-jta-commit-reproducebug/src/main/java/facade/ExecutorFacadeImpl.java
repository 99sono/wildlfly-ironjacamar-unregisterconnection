/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;

import inteceptor.FacadeInterceptor;

/**
 * Run a given command with different flavors of JTA transaction context underneath it. Avoids mushrooming facade API
 * classes
 */
@Stateless
@Interceptors(FacadeInterceptor.class)
public class ExecutorFacadeImpl implements ExecutorFacadeLocal {

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void executoreSynchronousNewJta(Runnable executeLogic) {
        executeLogic.run();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @Override
    public void executoreSynchronousSameJta(Runnable executeLogic) {
        executeLogic.run();

    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Override
    public void executoreSynchronousNoJta(Runnable executeLogic) {
        executeLogic.run();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @Asynchronous
    @Override
    public void executoreAsynchronousNoJta(Runnable executeLogic) {
        executeLogic.run();
    }

}
