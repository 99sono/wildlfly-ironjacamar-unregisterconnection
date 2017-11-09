# IJ000311: Throwable from unregister connection: java.lang.IllegalStateException: IJ000152: Trying to return an unknown connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6.

## Summary
The issue is reported in jboss JIRA:
(still to be opened) 

A study was conducted to try to understand what was happening - or leading to - the occurence of the 
IJ000311: Throwable from unregister connection: java.lang.IllegalStateException: IJ000152: Trying to return an unknown connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6.

What we now understand  - but the final conclusions are to be taken by the Wildfly Red Hat team, is that there might be a problem in the integration of
Wilfly ARJUNA with IRON JAC AMAR, in the sense that ARJUNA is creating new "stacks of KeyConnectionAssociation" each time a call goes over an EJB.
This is appears to be fundamentally wrong. A new STACK of KeyConnectionAssociation should only be created when the go goes over a new JTA transaction boundary, but not when
the JTA transaction boundary is still the same.

In the case of the sample application we demonstrate that druing @Startup, we can put an EJB to run.
startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb

This EJB will open a first JTA transaction, creating the first stack of KeyConnectionAssociation.

Within this EJB - code invokes a different EJB (ExecutorFacade) that is not annotated @TrasnsactionRequiresNew  but rather supports.
This means that when we invoke business logic of the second EJB the JTA transaction context is kep the same.
Despite this, what happens is that IRON JAC AMAR is being to prepare a second stack of "KeyConnectionAssociation" in the org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl.


Now the issue is very trick to reproduce because JPA implementers like Eclipselink are trying to minimize as much as possible the time when they are keeping in hands
a managed connection. (e.g. if we do a Fetch entity Ids just a simple read operation - eclipselink would open and immedialy return the connecton).
In any case, in the case of the sample application when we are in the NESTED transaction (e.g. se method create50Entities_stack2) we for eclipselink to acquire JDBC connection 
by persisting 50 entities and then doing a FLUSH(). (without the flush eclipselink would only ask for the connection at the very end of the transcation - so we force it to happen in stack 2)


In the backgorund, by accessing a JDBC connection in stack2 creates a connection record in the KeyConnectionAssociation of stack 2.


Now the problem starts the moment we go out of our ExecuterFacde call.
Arjuna will JAC AMAR to pop the stack. 
The KeyConnectionAssociation of stack 2 along with the managed connection is lost.

At the same time, eclipselink has not yet given back/closed the conection, because it needs to wait until the moment the JTA transaction is finished.

Finaly, when we are comming out of the STACK 1 the main jta transaction we will have the exception.
Eclipselink will try to give back the connection, and the "ungergister connection" method of org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl.

```java
public void unregisterConnection(org.jboss.jca.core.api.connectionmanager.listener.ConnectionCacheListener cm,
                                    org.jboss.jca.core.api.connectionmanager.listener.ConnectionListener cl,
                                    Object connection)
   {
      if (debug)
      {
         CloseConnectionSynchronization cas = getCloseConnectionSynchronization(false);
         if (cas != null)
         {
            cas.remove(connection);
         }

         synchronized (connectionStackTraces)
         {
            connectionStackTraces.remove(connection);
         }
      }

      KeyConnectionAssociation key = peekMetaAwareObject();

      log.tracef("unregistering connection from connection manager: %s, connection: %s, key: %s",
                    cm, connection, key);

      if (key == null)
         return;

      ConcurrentMap<ConnectionCacheListener, CopyOnWriteArrayList<ConnectionRecord>> cmToConnectionsMap =
         key.getCMToConnectionsMap();

      CopyOnWriteArrayList<ConnectionRecord> conns = cmToConnectionsMap.get(cm);

      // Can happen if connections are "passed" between contexts
      if (conns == null)
         return; <------ NOTE: I Would consider this scenario logically equivalent to the throw new IllegalStateExeption. If an application is asking for a connection to be closed it should be in the stack
		 --- But in this case, we are still reproducing the issue but not seing any exception stack trace simply because the outer transaction T1 - did not dod any access on the JDBC layer.
		 ---- But the problem would have remained that one STACK was popped that was responsible for the connection the JPA layer wants to return to the pool.

      // Note iterator of CopyOnWriteArrayList does not support remove method
      // We use here remove on CopyOnWriteArrayList directly
      for (ConnectionRecord connectionRecord : conns)
      {
         if (connectionRecord.getConnection() == connection)
         {
            if (Tracer.isEnabled())
            {
               ConnectionListener l = (ConnectionListener)cl;
               Tracer.unregisterCCMConnection(l.getPool().getName(), l.getManagedConnectionPool(),
                                              l, connection, key.toString());
            }

            conns.remove(connectionRecord);
            return;
         }
      }

      if (Tracer.isEnabled())
      {
         ConnectionListener l = (ConnectionListener)cl;
         Tracer.unknownCCMConnection(l.getPool().getName(), l.getManagedConnectionPool(),
                                     l, connection, key.toString());
      }

      if (!ignoreConnections)
         throw new IllegalStateException(bundle.tryingToReturnUnknownConnection(connection.toString())); <------ Willd End Up here causing excpetion.
		 ---- Our sample application is tuned to be able to reproduce this logic here via a special magical call we do on transaciton1: reproduceBugService.fetchAlIds();
		 ----- But one should keep in mind that whether we see the stack trace or not via doing our magical step reproduceBugService.fetchAlIds(); the issue remains the same
		 ----- The moment when our connection was popped out of existence is not consistent to when the JTA transaction actually finishes. Eclipselink appears to be closing
		 ---- the connection at the right moment.
   }
```



## Console Log Example illustrating the problem
```
2017-11-09 10:20:00,918 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) 
--------------------------------------
-- STARTING TransactionImple < ac, BasicAction: 0:ffffac140c58:634ef22a:5a041dbd:15 status: ActionStatus.RUNNING >: observing the CDI event StartupEventToTryToReproduceIJ000311Issue  
--------------------------------------



2017-11-09 10:20:00,919 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) Doing a fetch ALL query. just to bind the connection manager to the KEY of stack level 1.
2017-11-09 10:20:01,033 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) Eclipselink connected (jndiName: java:/jdbc/SAMPLE_DS)- obtaining connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@1d5b3a43. Eclipselink Server session is: ServerSession(
	DatabaseAccessor(connected)
	PostgreSQLPlatform)   
2017-11-09 10:20:01,057 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) EclipseLink - Closing connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@1d5b3a43
2017-11-09 10:20:01,063 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) At this point the org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl will have  registered on stack1 the connection used by eclipselink, and unregistered as well.
 However, this temporary register/de-register is sufficient to add to the current key.getCMToConnectionsMap()  and entry for the JDBC connection manger.
  This is fundamental, because without this step the exception that we will later see would be hidden away - even though the logical problem would be there no exception would be logged.  By taking this magical step here, we have ensured that we will be able to reproduce the excpetion later. 
   To summarize what has happend so far.
 1. We have forced eclipselink to register and unregister a connection due to are fetch id query 
 2. The JTA transaction ID TransactionImple < ac, BasicAction: 0:ffffac140c58:634ef22a:5a041dbd:15 status: ActionStatus.RUNNING >  continues healthy and running without any problem 3. Meanwhile internally, our CachedConnectionManagerImpl, for this thread, has a single task of keys in its currentObjects field 
  4. The leading key on this stack does not hold any connectionrecords (eclipselink returned the connecton to the pool) but it does have a KEY for the JDBC connection manager 5. Point 4 is the magical step to allow us to see the stack trace but not the core of the issue - just the core of seing the stack trace 

2017-11-09 10:20:01,066 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) START: executoreSynchronousSameJta 
2017-11-09 10:20:01,066 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) Right now we are within the same JTA transaction. But on a NESTED EJB call.
  What technically has happened at this point is that IRON JAC AMAR now has a second stack (keyAssociation)
  in the currentObjects. We add in stacks each time we go through an EJB evne if we keep the JTA transaction
  and this is the BUG. But we will see more about this later.
 
2017-11-09 10:20:01,066 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) STARTING 3: observing the CDI event StartupEventToTryToReproduceIJ000311Issue - The current transaction id is: TransactionImple < ac, BasicAction: 0:ffffac140c58:634ef22a:5a041dbd:15 status: ActionStatus.RUNNING > 
2017-11-09 10:20:01,066 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) Going to create 50 entities on the database
2017-11-09 10:20:01,067 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) Eclipselink connected (jndiName: jdbc/SAMPLE_NON_JTA_DS)- obtaining connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@4be2573e. Eclipselink Server session is: ClientSession({})   
2017-11-09 10:20:01,073 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) EclipseLink - Closing connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@4be2573e
2017-11-09 10:20:01,078 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) 50 entities should have been created: 

2017-11-09 10:20:01,078 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) Now we want to flush - we want to force eclipselink to have to acquire the java:/jdbc/SAMPLE_DS and not be allowed to close the connection yet. 
2017-11-09 10:20:01,085 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) Eclipselink connected (jndiName: java:/jdbc/SAMPLE_DS)- obtaining connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6. Eclipselink Server session is: ClientSession({default=DatabaseAccessor(connected)})   
2017-11-09 10:20:01,101 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) The flush has been done. We expec that the log now shows us that java:/jdbc/SAMPLE_DS connection was acquired but not yet closed by eclipselink. 
2017-11-09 10:20:01,101 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) ENDING TransactionImple < ac, BasicAction: 0:ffffac140c58:634ef22a:5a041dbd:15 status: ActionStatus.RUNNING >: finishing to observer. The current transaction id is: {} 
2017-11-09 10:20:01,101 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) OK. Now we are about to leave our nested EJB call. What is happening now is very important. 
1. We will leave a nested EJB call but not the JTA transaction 2. Wildfly will go to the cachedConnectonManagerImpl and tell it to pop a stack (BUG) 3. The stack that goes away is actually holding the JDBC connection thta has been given to ECLIPSELINK and thta eclipselink will later try to close 4. When we leave this call we go back to STACK 1, where no connections are being managed
2017-11-09 10:20:01,102 INFO  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73)  FINISH: executoreSynchronousSameJta:

 
2017-11-09 10:20:01,102 WARN  [startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb] (ServerService Thread Pool -- 73) NOW we are out of our nested EJB call. We are bout to leave the main EJB transaction call. 
In short we are about to reproduce the stack trace. 
What will now happen is the following. 
1. Wildfly will tell eclipselink that the transaction is at an end and it should commit if it can 
2. Eclipselink does its job of committing the changes and takes the JTA connection it was given and tries to close it 
3. IRON JAC AMAR will log the stack trace we wanted to reproduce saying it has not idea what connection we are trying to return to the pool. 
4. The reason the CachedConnectionManagerImpl does not know about the connection is because it was managed under STACK 2 but was popped out of existence after the nested transaction 
 5. Now it is very important to keep in mind that even if we were not seing the stack trace  the error would be there (the moment we popped a key stack that had still connections) 6. Keep in mind that if we were to modify the source code  and disable our reproduceBugService.fetchAlIds() call we would see no stack trace but the same issue would be present.7. The difference that doing the reproduceBugService.fetchAlIds() does is whether or not CachedConnectionManagerImpl during unregister connection leaves via the if conns == null return null or if goes further into the if (!ignoreConnections)
 8. OK - Now that we understand what is happening behind the scenes we can let eclipselink try to close the JDBC connection at the end of the JTA transaction



 
2017-11-09 10:20:01,106 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) EclipseLink - Closing connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6
2017-11-09 10:20:01,107 INFO  [org.jboss.jca.core.connectionmanager.listener.TxConnectionListener] (ServerService Thread Pool -- 73) IJ000311: Throwable from unregister connection: java.lang.IllegalStateException: IJ000152: Trying to return an unknown connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6
	at org.jboss.jca.core.connectionmanager.ccm.CachedConnectionManagerImpl.unregisterConnection(CachedConnectionManagerImpl.java:408)
	at org.jboss.jca.core.connectionmanager.listener.TxConnectionListener.connectionClosed(TxConnectionListener.java:645)
	at org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnection.returnHandle(BaseWrapperManagedConnection.java:596)
	at org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnection.closeHandle(BaseWrapperManagedConnection.java:541)
	at org.jboss.jca.adapters.jdbc.WrappedConnection.returnConnection(WrappedConnection.java:298)
	at org.jboss.jca.adapters.jdbc.WrappedConnection.close(WrappedConnection.java:256)
	at org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor.closeDatasourceConnection(DatabaseAccessor.java:522)
	at org.eclipse.persistence.internal.databaseaccess.DatasourceAccessor.closeConnection(DatasourceAccessor.java:537)
	at org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor.closeConnection(DatabaseAccessor.java:545)
	at org.eclipse.persistence.internal.databaseaccess.DatasourceAccessor.closeJTSConnection(DatasourceAccessor.java:188)
	at org.eclipse.persistence.sessions.server.ClientSession.releaseJTSConnection(ClientSession.java:171)
	at org.eclipse.persistence.transaction.AbstractSynchronizationListener.beforeCompletion(AbstractSynchronizationListener.java:175)
	at org.eclipse.persistence.transaction.JTASynchronizationListener.beforeCompletion(JTASynchronizationListener.java:68)
	at org.jboss.as.txn.service.internal.tsr.JCAOrderedLastSynchronizationList.beforeCompletion(JCAOrderedLastSynchronizationList.java:116)
	at com.arjuna.ats.internal.jta.resources.arjunacore.SynchronizationImple.beforeCompletion(SynchronizationImple.java:76)
	at com.arjuna.ats.arjuna.coordinator.TwoPhaseCoordinator.beforeCompletion(TwoPhaseCoordinator.java:368)
	at com.arjuna.ats.arjuna.coordinator.TwoPhaseCoordinator.end(TwoPhaseCoordinator.java:91)
	at com.arjuna.ats.arjuna.AtomicAction.commit(AtomicAction.java:162)
	at com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple.commitAndDisassociate(TransactionImple.java:1200)
	at com.arjuna.ats.internal.jta.transaction.arjunacore.BaseTransaction.commit(BaseTransaction.java:126)
	at com.arjuna.ats.jbossatx.BaseTransactionManagerDelegate.commit(BaseTransactionManagerDelegate.java:89)
	at org.jboss.as.ejb3.tx.CMTTxInterceptor.endTransaction(CMTTxInterceptor.java:91)
	at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInOurTx(CMTTxInterceptor.java:279)
	at org.jboss.as.ejb3.tx.CMTTxInterceptor.requiresNew(CMTTxInterceptor.java:349)
	at org.jboss.as.ejb3.tx.CMTTxInterceptor.processInvocation(CMTTxInterceptor.java:241)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.invocationmetrics.WaitTimeInterceptor.processInvocation(WaitTimeInterceptor.java:47)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.security.SecurityContextInterceptor.processInvocation(SecurityContextInterceptor.java:100)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.deployment.processors.StartupAwaitInterceptor.processInvocation(StartupAwaitInterceptor.java:22)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
	at org.jboss.as.ejb3.concurrency.ContainerManagedConcurrencyInterceptor.processInvocation(ContainerManagedConcurrencyInterceptor.java:110)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.interceptors.ShutDownInterceptorFactory$1.processInvocation(ShutDownInterceptorFactory.java:64)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.interceptors.LoggingInterceptor.processInvocation(LoggingInterceptor.java:67)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.interceptors.AdditionalSetupInterceptor.processInvocation(AdditionalSetupInterceptor.java:54)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
	at org.wildfly.security.manager.WildFlySecurityManager.doChecked(WildFlySecurityManager.java:636)
	at org.jboss.invocation.AccessCheckingInterceptor.processInvocation(AccessCheckingInterceptor.java:61)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
	at org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
	at org.jboss.as.ee.component.ViewService$View.invoke(ViewService.java:198)
	at org.jboss.as.ee.component.ViewDescription$1.processInvocation(ViewDescription.java:185)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
	at org.jboss.as.ee.component.ProxyInvocationHandler.invoke(ProxyInvocationHandler.java:73)
	at startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb$$$view9.observeStartupEventToTryToReproduceIJ000311Issue(Unknown Source)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.jboss.weld.util.reflection.Reflections.invokeAndUnwrap(Reflections.java:433)
	at org.jboss.weld.bean.proxy.EnterpriseBeanProxyMethodHandler.invoke(EnterpriseBeanProxyMethodHandler.java:128)
	at org.jboss.weld.bean.proxy.EnterpriseTargetBeanInstance.invoke(EnterpriseTargetBeanInstance.java:56)
	at org.jboss.weld.bean.proxy.ProxyMethodHandler.invoke(ProxyMethodHandler.java:100)
	at startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb$Proxy$_$$_Weld$EnterpriseProxy$.observeStartupEventToTryToReproduceIJ000311Issue(Unknown Source)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.jboss.weld.injection.StaticMethodInjectionPoint.invoke(StaticMethodInjectionPoint.java:88)
	at org.jboss.weld.injection.StaticMethodInjectionPoint.invoke(StaticMethodInjectionPoint.java:78)
	at org.jboss.weld.injection.MethodInvocationStrategy$SimpleMethodInvocationStrategy.invoke(MethodInvocationStrategy.java:129)
	at org.jboss.weld.event.ObserverMethodImpl.sendEvent(ObserverMethodImpl.java:313)
	at org.jboss.weld.event.ObserverMethodImpl.sendEvent(ObserverMethodImpl.java:291)
	at org.jboss.weld.event.ObserverMethodImpl.notify(ObserverMethodImpl.java:269)
	at org.jboss.weld.event.ObserverNotifier.notifySyncObservers(ObserverNotifier.java:302)
	at org.jboss.weld.event.ObserverNotifier.notify(ObserverNotifier.java:291)
	at org.jboss.weld.event.EventImpl.fire(EventImpl.java:89)
	at startupconnectionproblem.event.FireStartupEventsServiceImpl.fireStartupCDIEvents(FireStartupEventsServiceImpl.java:41)
	at startupconnectionproblem.event.FireStartupEventsServiceImpl$Proxy$_$$_WeldClientProxy.fireStartupCDIEvents(Unknown Source)
	at startupconnectionproblem.event.StartupSingletonLocalEjb.postConstrcut(StartupSingletonLocalEjb.java:38)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.jboss.as.ee.component.ManagedReferenceLifecycleMethodInterceptor.processInvocation(ManagedReferenceLifecycleMethodInterceptor.java:96)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.doLifecycleInterception(Jsr299BindingsInterceptor.java:114)
	at org.jboss.as.weld.ejb.Jsr299BindingsInterceptor.processInvocation(Jsr299BindingsInterceptor.java:103)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.InterceptorContext$Invocation.proceed(InterceptorContext.java:437)
	at org.jboss.weld.ejb.AbstractEJBRequestScopeActivationInterceptor.aroundInvoke(AbstractEJBRequestScopeActivationInterceptor.java:73)
	at org.jboss.as.weld.ejb.EjbRequestScopeActivationInterceptor.processInvocation(EjbRequestScopeActivationInterceptor.java:83)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.weld.injection.WeldInjectionInterceptor.processInvocation(WeldInjectionInterceptor.java:53)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ee.component.AroundConstructInterceptorFactory$1.processInvocation(AroundConstructInterceptorFactory.java:28)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.weld.injection.WeldInterceptorInjectionInterceptor.processInvocation(WeldInterceptorInjectionInterceptor.java:56)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.weld.ejb.Jsr299BindingsCreateInterceptor.processInvocation(Jsr299BindingsCreateInterceptor.java:100)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ee.component.NamespaceContextInterceptor.processInvocation(NamespaceContextInterceptor.java:50)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.tx.CMTTxInterceptor.invokeInNoTx(CMTTxInterceptor.java:263)
	at org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor.notSupported(LifecycleCMTTxInterceptor.java:87)
	at org.jboss.as.ejb3.tx.LifecycleCMTTxInterceptor.processInvocation(LifecycleCMTTxInterceptor.java:64)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.weld.injection.WeldInjectionContextInterceptor.processInvocation(WeldInjectionContextInterceptor.java:43)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.interceptors.CurrentInvocationContextInterceptor.processInvocation(CurrentInvocationContextInterceptor.java:41)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ee.concurrent.ConcurrentContextInterceptor.processInvocation(ConcurrentContextInterceptor.java:45)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.ContextClassLoaderInterceptor.processInvocation(ContextClassLoaderInterceptor.java:64)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.as.ejb3.component.singleton.StartupCountDownInterceptor.processInvocation(StartupCountDownInterceptor.java:25)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.InterceptorContext.run(InterceptorContext.java:356)
	at org.jboss.invocation.PrivilegedWithCombinerInterceptor.processInvocation(PrivilegedWithCombinerInterceptor.java:80)
	at org.jboss.invocation.InterceptorContext.proceed(InterceptorContext.java:340)
	at org.jboss.invocation.ChainedInterceptor.processInvocation(ChainedInterceptor.java:61)
	at org.jboss.as.ee.component.BasicComponent.constructComponentInstance(BasicComponent.java:161)
	at org.jboss.as.ee.component.BasicComponent.constructComponentInstance(BasicComponent.java:134)
	at org.jboss.as.ee.component.BasicComponent.createInstance(BasicComponent.java:88)
	at org.jboss.as.ejb3.component.singleton.SingletonComponent.getComponentInstance(SingletonComponent.java:124)
	at org.jboss.as.ejb3.component.singleton.SingletonComponent.start(SingletonComponent.java:138)
	at org.jboss.as.ee.component.ComponentStartService$1.run(ComponentStartService.java:54)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
	at org.jboss.threads.JBossThread.run(JBossThread.java:320)

2017-11-09 10:20:01,114 INFO  [startupconnectionproblem.event.FireStartupEventsServiceImpl] (ServerService Thread Pool -- 73) FIINSH: StartupEventToTryToReproduceIJ000311Issue 
2017-11-09 10:20:01,114 INFO  [startupconnectionproblem.event.StartupSingletonLocalEjb] (ServerService Thread Pool -- 73) ENDING 1: Post construct of startup bean singleton ending
2017-11-09 10:20:01,240 INFO  [javax.enterprise.resource.webcontainer.jsf.config] (ServerService Thread Pool -- 73) Initializing Mojarra 2.2.13.SP1 20160303-1204 for context '/app'
2017-11-09 10:20:02,098 INFO  [org.primefaces.webapp.PostConstructApplicationEventListener] (ServerService Thread Pool -- 73) Running on PrimeFaces 6.0
```


## Reproduce ISSUE (Deploy sample application)

### Required modules:

To deploy the application some modules need to be present:
1. module: company.org.eclipse.persistence:main
This is the eclipselink module.

```xml
<module xmlns="urn:jboss:module:1.3" name="company.org.eclipse.persistence">
    <properties>
        <property name="jboss.api" value="private"/>
    </properties>

    <resources>
        <!-- resource-root path="jipijapa-eclipselink-10.1.0.Final.jar"/ -->
		<!-- This version of the module should address bug: WFLY-8954 -->
		<resource-root path="jipijapa-eclipselink-11.0.0.Final-SNAPSHOT.jar"/>
		
          <resource-root path="eclipselink-2.6.6.0.0.2.jar">		  
           <filter>
              <exclude path="javax/**" />
           </filter>
        </resource-root>		
    </resources>

    <dependencies>
        <module name="asm.asm"/>
        <module name="javax.api"/>
        <module name="javax.annotation.api"/>
        <module name="javax.enterprise.api"/>
        <module name="javax.persistence.api"/>
        <module name="javax.transaction.api"/>
        <module name="javax.validation.api"/>
        <module name="javax.xml.bind.api"/>
        <module name="org.antlr"/>
        <module name="org.dom4j"/>
        <module name="org.javassist"/>
        <module name="org.jboss.as.jpa.spi"/>
        <module name="org.jboss.logging"/>
        <module name="org.jboss.vfs"/>
		
		<!-- Add dependency on rest api -->
		<module name="javax.ws.rs.api" />
    </dependencies>
</module>
```

The eclipselink-2.6.6.0.0.2.jar does not exist anywhere excpet on my local machine.
It is a modified eclipselink where the DatasourceAcessor is logging information about CLOSING / OPENING connections.
It allows my log file to get information such as:

```
2017-11-09 10:20:01,106 FINE  [org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor] (ServerService Thread Pool -- 73) EclipseLink - Closing connection: org.jboss.jca.adapters.jdbc.jdk7.WrappedConnectionJDK7@2021ff6
```

This module will be referred in the persistence.xml under the property:
jboss.as.jpa.providerModule


### Module for JDBC Driver (e.g. Postgres)
The standalone.xml configuration provided, see 
C:\dev\branches\wildlfly-ironjacamar-unregisterconnection\wildlfly-ironjacamar-unregisterconnection\wildfly-jta-commit-reproducebug\src\test\resourceszstandalone-orcl_postgres_database_reproduces_issue_wildfly.xml

The dpeloyer will want to play with the configuraiton under:

```xml
<drivers>
                    <driver name="h2" module="com.h2database.h2">
                        <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
                    </driver>
                    <driver name="hsql" module="company.org.hsql">
                        <datasource-class>org.hsqldb.jdbc.JDBCDataSource</datasource-class>
                    </driver>
                    <driver name="oracleXA" module="company.com.oracle.ojdbc">
                        <xa-datasource-class>oracle.jdbc.xa.client.OracleXADataSource</xa-datasource-class>
                    </driver>
                    <driver name="oracle" module="company.com.oracle.ojdbc">
                        <xa-datasource-class>oracle.jdbc.pool.OracleDataSource</xa-datasource-class>
                    </driver>
                    <driver name="sqlserverXA" module="company.com.microsoft.jdbc">
                        <xa-datasource-class>com.microsoft.sqlserver.jdbc.SQLServerXADataSource</xa-datasource-class>
                    </driver>
                    <driver name="sqlserver" module="company.com.microsoft.jdbc">
                        <xa-datasource-class>com.microsoft.sqlserver.jdbc.SQLServerDriver</xa-datasource-class>
                    </driver>
                    <driver name="postgres" module="company.org.postgres.jdbc">
                        <xa-datasource-class>org.postgresql.Driver</xa-datasource-class>
                    </driver>
                </drivers>
```

And keep only what they need. 
The driver used in this exercise was postgres, but it is irrelevant.



### Deploy the create db war
Start by starting up wildfly with the standalone-orcl-config.xml.
Once the server is running, deploy the create DB war file.
This should create a table 
SELECT * FROM SOMEENTITY;

You can now undeploy the create db war, it is no longer needed once the DB schema is set in place.

### Deploy the reproduce bug war

When deploying the reproduce bug war, during startup the 
startupconnectionproblem.observer.EventObserverNestedJtaTransactionEjb




## Tunning eclipselink source code to have logging information about when CONNECTIONS are created:
It may be convenient to tune eclipselink to log information about acquisition / release of connections.
I have locally compiled a version of eclipselink 2.6 and modified the two source classes bellow to have easy tracking
of when eclipselink asks and gives back a connection to wildfly.

This why in the log snippet above we have staments about when eclipselink opend/closes a connection and for which data source the connection belongs.


### org.eclipse.persistence.internal.databaseaccess.DatasourceAccessor

The folloing changes went int

```java
/**
     * Connect to the database. Exceptions are caught and re-thrown as EclipseLink exceptions.
     */
    protected void connectInternal(Login login, AbstractSession session) throws DatabaseException {
        try {
            this.datasourceConnection = login.connectToDatasource(this, session);
            String jndiName = getJndiName(login);
            String connectInformation = String.format(
                    "Eclipselink connected (jndiName: %1$s)- obtaining connection: %2$s. Eclipselink Server session is: %3$s   ",
                    jndiName, this.datasourceConnection, session);
            LOGGER.fine(connectInformation);
            this.isConnected = true;
            if (this.customizer != null) {
                customizer.customize();
            }
        } catch (DatabaseException ex) {
            // Set the accessor to ensure the retry code has an opportunity to retry.
            ex.setAccessor(this);
            throw ex;
        }
    }
	
	
	/**
     * Try to figure out the JNDI name associated to a connection.
     *
     * @param login
     *            The database login
     * @return The JNDI name for the database login, if we can find it. Otherwise the empty tring
     */
    protected String getJndiName(Login login) {
        // (a) We only now how to work with DatabaseLogins for this scope
        boolean isDatabaseLoging = login instanceof DatabaseLogin;
        if (!isDatabaseLoging) {
            return "";
        }
        DatabaseLogin databaseLogin = (DatabaseLogin) login;
        // (b) The connector now would have to be a jndi connector
        if (!(databaseLogin.getConnector() instanceof JNDIConnector)) {
            return "";
        }

        // (c) Try to get the jndi name directly from the eclipse jndi connector
        JNDIConnector jndiConnector = (JNDIConnector) databaseLogin.getConnector();
        String name = jndiConnector.getName();
        boolean nameNotDefined = name == null || "".equals(name);
        if (!nameNotDefined) {
            return name;
        }

        // (d) if this does not work try to hack with reflection reading the JNDI name
        // from WildflyDataSource
        DataSource dataSource = jndiConnector.getDataSource();
        if (dataSource != null) {
            return readPrivateField(dataSource, "jndiName");
        }
        return "";
    }

    /**
     * Read the private field of a class. In particular, we are thinking about the wildfly data source.
     *
     * @param objectToRead
     *
     * @param strFieldName
     * @return
     */
    protected String readPrivateField(Object objectToRead, String strFieldName) {
        try {
            Field f = objectToRead.getClass().getDeclaredField(strFieldName); // NoSuchFieldException
            f.setAccessible(true);
            return (String) f.get(objectToRead); // IllegalAccessException
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    String.format("Field %1$s could not be read. Returning empty string", strFieldName));
            return "";
        }

    }
```


### org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor
```java
 /**
     * Close the connection.
     */
    @Override
    public void closeDatasourceConnection() throws DatabaseException {
        try {
            Connection connection = getConnection();
            String logMessage = String.format("EclipseLink - Closing connection: %1$s", connection);
            LOGGER.fine(logMessage);
            connection.close(); // FIXME: log the closing of the connection
        } catch (SQLException exception) {
            throw DatabaseException.sqlException(exception, this, null, false);
        }
    }

```









