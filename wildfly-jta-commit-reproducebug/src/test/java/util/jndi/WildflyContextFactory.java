/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.jndi;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Provide access to the weblogic JNDI Context.
 */
public final class WildflyContextFactory {

    public static WildflyContextFactory SINGLETON = new WildflyContextFactory();

    private WildflyContextFactory() {
    }

    public InitialContext create() {
        Properties initialContextProperties = new Properties();
        initialContextProperties.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY,
                "org.jboss.naming.remote.client.InitialContextFactory");
        // Note this field is irrelevant once the java.naming.factory.url.pkgs optimization is in
        // The value that really matters for the connection is the jboss-ejb-clien.properties
        initialContextProperties.setProperty(InitialContext.PROVIDER_URL, "http-remoting://localhost:8080");
        initialContextProperties.setProperty("java.naming.factory.url.pkgs", "org.jboss.ejb.client.naming");
        initialContextProperties.setProperty(InitialContext.SECURITY_PRINCIPAL, "admin");
        initialContextProperties.setProperty(InitialContext.SECURITY_CREDENTIALS, "admin");
        try {
            return new InitialContext(initialContextProperties);
        } catch (NamingException ex) {
            throw new RuntimeException("could not create initial context for weblogic application server", ex);
        }
    }

}
