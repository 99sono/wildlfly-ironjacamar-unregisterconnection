/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import org.junit.Before;

/**
 *
 * @author b7godin
 */
public class ModelCrudFacadeImplSystemT {

    private static final String WAR_NAME = "generic-logging-interceptor-bug";

    private static int NUMBER_OF_FACADE_CALLS_TO_DO = 500;

    /*
     * ModelCrudFacadeRemote remoteFacade; ModelCrudFacadeRemoteA remoteFacadeA; ModelCrudFacadeRemoteB remoteFacadeB;
     * ModelCrudFacadeRemoteC remoteFacadeC;
     */
    @Before
    public void before() {
        // java:global/generic-logging-interceptor-bug/ModelCrudFacadeImpl!facade.ModelCrudFacadeRemote
        // java:global/generic-logging-interceptor-bug/ModelCrudFacadeImpl!facade.ModelCrudFacadeRemote
        /*
         * remoteFacade = JndiUtil.SINGLETON.resolveBean(WAR_NAME, "ModelCrudFacadeImpl", ModelCrudFacadeRemote.class);
         * remoteFacadeA = JndiUtil.SINGLETON.resolveBean(WAR_NAME, "ModelCrudFacadeAImpl",
         * ModelCrudFacadeRemoteA.class); remoteFacadeB = JndiUtil.SINGLETON.resolveBean(WAR_NAME,
         * "ModelCrudFacadeBImpl", ModelCrudFacadeRemoteB.class); remoteFacadeC =
         * JndiUtil.SINGLETON.resolveBean(WAR_NAME, "ModelCrudFacadeCImpl", ModelCrudFacadeRemoteC.class);
         */

        //
        System.out.println("Deleting all entiteis before test");
        // remoteFacade.deleteAll();
    }

}
