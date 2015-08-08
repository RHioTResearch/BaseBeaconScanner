package amq;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.rhiot.beacon.publishers.qpid.QpidPublisher;
import org.junit.Test;

/**
 * Created by starksm on 8/7/15.
 */
public class TestJMSClient {
    private static final String USER = "demo-user";
    private static final String PASSWORD = "2015-summit-user";

    static void sendmsg(Session session, Destination dest) throws JMSException {
        MessageProducer producer = session.createProducer(dest);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        Message msg = session.createTextMessage();
        msg.setStringProperty("scannerID", "LoopbackTest");
        producer.send(msg);
        System.out.printf("Sent message\n");
        producer.close();
    }
    static void recvmsg(Session session, Destination dest) throws JMSException {
        MessageConsumer consumer = session.createConsumer(dest);
        Message msg = consumer.receive(5000);
        System.out.printf("Recv message: %s\n", msg);
        consumer.close();
    }
    @Test
    public void testSendRecv() throws Exception {
        Properties props = new Properties();
        props.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        //props.setProperty("connectionfactory.myFactoryLookup", "amqp://52.10.252.216:5672");
        props.setProperty("connectionfactory.myFactoryLookup", "amqp://192.168.1.107:5672");
        Context context = new InitialContext(props);

        // Create a Connection
        ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
        Connection connection = factory.createConnection(USER, PASSWORD);
        System.out.printf("ConnectionFactory created connection: %s\n", connection);
        connection.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException ex) {
                ex.printStackTrace();
            }
        });
        connection.start();

        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        System.out.printf("Created session: %s\n", session);
        Destination beaconEvents  = session.createQueue("beaconEvents");
        sendmsg(session, beaconEvents);
        recvmsg(session, beaconEvents);
        session.close();
        connection.close();
    }

    @Test
    public void testSendRecvFailoverURI() throws Exception {
        Properties props = new Properties();
        props.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        //props.setProperty("connectionfactory.myFactoryLookup", "amqp://52.10.252.216:5672");
        String uri = String.format("failover:(%s)?failover.reconnectDelay=1000", "amqp://192.168.1.107:5672");
        props.setProperty("connectionfactory.myFactoryLookup", uri);
        Context context = new InitialContext(props);

        // Create a Connection
        ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
        Connection connection = factory.createConnection(USER, PASSWORD);
        System.out.printf("ConnectionFactory created connection: %s\n", connection);
        connection.setExceptionListener(new ExceptionListener() {
            @Override
            public void onException(JMSException ex) {
                ex.printStackTrace();
            }
        });
        connection.start();

        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        System.out.printf("Created session: %s\n", session);
        Destination beaconEvents  = session.createQueue("beaconEvents");
        sendmsg(session, beaconEvents);
        recvmsg(session, beaconEvents);
        session.close();
        connection.close();
    }


    @Test
    public void testQpidPublisher() throws Exception {
        //String uri = String.format("failover:(%s)?failover.reconnectDelay=1000", "amqp://192.168.1.107:5672");
        String uri = "amqp://192.168.1.107:5672";

        QpidPublisher publisher = new QpidPublisher(uri, USER, PASSWORD, null);
        publisher.setDestinationName("beaconEvents");
        publisher.start(false);
        Properties props = new Properties();
        props.setProperty("scannerID", "testQpidPublisher");
        publisher.publishProperties(null, props);
        Message msg = publisher.recvMessage();
        System.out.printf("%s\n", msg);
        publisher.stop();
    }
}
