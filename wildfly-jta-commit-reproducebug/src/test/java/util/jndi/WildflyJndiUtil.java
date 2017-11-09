/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author b7godin
 */
public final class WildflyJndiUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(WildflyJndiUtil.class);

    public static final WildflyJndiUtil SINGLETON = new WildflyJndiUtil();

    InitialContext ictx = WildflyContextFactory.SINGLETON.create();

    private WildflyJndiUtil() {
    }

    public <T> T resolveBean(String war, String beanName, Class<T> remoteInterface) {
        // (a) build up the efficient jndi name of the jbos ejb client library
        final String appName = "";
        final String moduleName = war;
        final String distinctName = "";
        final String viewClassName = remoteInterface.getName();
        String jndiName = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!"
                + viewClassName;

        // (b) execute the lookup
        LOGGER.info("Going load EJB proxy for: {} ", jndiName);
        T bean = null;
        try {
            // (c) create an initial context to the remote JNDi server on the
            // app server
            InitialContext initialContext = ictx;
            bean = (T) remoteInterface.cast(initialContext.lookup(jndiName));
        } catch (NamingException ne) {
            throw new RuntimeException("Failed to resolved bean with JNDI name: " + jndiName, ne);
        }
        return bean;

    }

}
