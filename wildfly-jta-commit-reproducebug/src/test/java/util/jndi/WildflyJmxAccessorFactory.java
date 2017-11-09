/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.jndi;

import java.util.HashMap;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Provide access to the weblogic JNDI Context.
 */
public final class WildflyJmxAccessorFactory {

    public static WildflyJmxAccessorFactory SINGLETON = new WildflyJmxAccessorFactory();

    private WildflyJmxAccessorFactory() {
    }

    public JMXConnector createAJmxConntection() {
        String[] credentials = new String[] { "weblogic", "welcome1" };
        HashMap<String, Object> environment = new HashMap<String, Object>();
        environment.put(JMXConnector.CREDENTIALS, credentials);
        environment.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "org.jboss.remotingjmx");
        String urlString = System.getProperty("jmx.service.url", "service:jmx:remote+http://localhost:7001");
        JMXServiceURL url;
        try {
            url = new JMXServiceURL(urlString);
            // 2. connect to the JMX server
            return JMXConnectorFactory.connect(url, environment);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
