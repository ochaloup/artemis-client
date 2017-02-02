package org.jboss.qa;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class Main {
    private static final long timeoutMs = TimeUnit.SECONDS.toMillis(15);
    private static final String host = "localhost";
    private static final int port = 8080;
    private static final String username = "user";
    private static final String password = "user";
    private static final String queue = "DLQ";
    private static final String message = "hello there";
    

    public static void main( String[] args ) {
        System.out.println(String.format("Getting artemis connection at %s:%s with credentials %s/%s",
            host, port, username, password));

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(TransportConstants.HOST_PROP_NAME, host);
        props.put(TransportConstants.PORT_PROP_NAME, port);
        // this is needed for WildFly as http protocol upgrade has to be enabled
        props.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        props.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        TransportConfiguration config = new TransportConfiguration(NettyConnectorFactory.class.getCanonicalName(), props);

        ActiveMQConnectionFactory cf = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);
        cf.setCallTimeout(timeoutMs);
        
        try (Connection connection = cf.createConnection(username, password)) {

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue testQueue = session.createQueue(queue);

            // for sending message I do not need to start connection, for receiving we need it
            MessageProducer producer = session.createProducer(testQueue);
            TextMessage msg = session.createTextMessage(message);
            producer.send(msg);

            session.close();
        } catch (Exception e) {
            throw new IllegalStateException("Something wrong happens during sending message to queue " + queue, e);
        }

        System.out.println(String.format("Message '%s' has been sent to queue %s", message, queue));
    }
}
