package org.jboss.rhiot.beacon.publishers.qpid;

import org.jboss.logging.Logger;
import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.Beacon;
import org.jboss.rhiot.beacon.common.MsgType;
import org.jboss.rhiot.beacon.common.MsgPublisher;

import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Created by starksm on 7/12/15.
 */
public class QpidPublisher implements MsgPublisher {
    private static Logger log = Logger.getLogger(QpidPublisher.class);

    private String brokerURL;
    private String username;
    private String password;
    private String clientID;
    private String defaultDestinationName;
    private boolean useTopics;
    private Connection connection;
    private Session session;
    private MessageProducer defaultProducer;
    private Destination defaultDestination;

    public QpidPublisher(String brokerUrl, String userName, String password, String clientID) {
        this.brokerURL = brokerUrl;
        this.username = userName;
        this.password = password;
        this.clientID = clientID;
    }

    @Override
    public void setDestinationName(String name) {
        this.defaultDestinationName = name;
    }

    @Override
    public String getDestinationName() {
        return defaultDestinationName;
    }

    @Override
    public int getReconnectInterval() {
        return 0;
    }

    @Override
    public void setReconnectInterval(int reconnectInterval) {

    }

    @Override
    public boolean isReconnectOnFailure() {
        return false;
    }

    @Override
    public void setReconnectOnFailure(boolean reconnectOnFailure) {

    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public void setConnected(boolean connected) {

    }

    @Override
    public void setUseTopics(boolean flag) {
        this.useTopics = flag;
    }

    @Override
    public boolean isUseTopics() {
        return useTopics;
    }

    @Override
    public boolean isUseTransactions() {
        return false;
    }

    @Override
    public void setUseTransactions(boolean useTransactions) {

    }

    @Override
    public void start(boolean asyncMode) throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        String uri = brokerURL; // String.format("failover:(%s)?failover.reconnectDelay=1000", brokerURL);
        env.setProperty("connectionfactory.myFactoryLookup", uri);

        Context context = new InitialContext(env);
        // Create a Connection
        ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
        connection = factory.createConnection(username, password);
        connection.setExceptionListener(Throwable::printStackTrace);
        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        defaultDestination = getDestination(defaultDestinationName);
        defaultProducer = session.createProducer(defaultDestination);
    }

    @Override
    public void stop() throws Exception {
        if(session != null)
            session.close();
        connection.stop();
        connection.close();
    }

    @Override
    public void publish(String destinationName, BeaconInfo beaconInfo) {
        try {
            BytesMessage message = session.createBytesMessage();
            message.setStringProperty("uuid", beaconInfo.getUuid());
            message.setStringProperty("scannerID", beaconInfo.getScannerID());
            message.setIntProperty("major", beaconInfo.getMajor());
            message.setIntProperty("minor", beaconInfo.getMinor());
            message.setIntProperty("manufacturer", beaconInfo.getManufacturer());
            message.setIntProperty("code", beaconInfo.getCode());
            message.setIntProperty("power", beaconInfo.getPower());
            message.setIntProperty("calibratedPower", beaconInfo.getCalibrated_power());
            message.setIntProperty("rssi", beaconInfo.getRssi());
            message.setLongProperty("time", beaconInfo.getTime());
            message.setIntProperty("messageType", MsgType.SCANNER_READ.ordinal());
            message.setIntProperty("scannerSeqNo", beaconInfo.getScannerSequenceNo());
            sendMessage(destinationName, message);
        } catch (JMSException e) {
            log.error("publish", e);
        }
    }

    @Override
    public void publish(String destinationName, Beacon beacon) {

    }

    @Override
    public void publishStatus(BeaconInfo beaconInfo) {

    }

    @Override
    public void publishStatus(Beacon beaconInfo) {

    }

    @Override
    public void publishProperties(String destinationName, Properties properties) {
        try {
            BytesMessage message = session.createBytesMessage();
            for(String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                message.setStringProperty(key, value);
            }
            sendMessage(destinationName, message);
        } catch (JMSException e) {
            log.error("publish", e);
        }
    }

    /**
     * Only for debugging
     * @return
     */
    public Message recvMessage() throws JMSException {
        MessageConsumer consumer = session.createConsumer(defaultDestination);
        Message msg = consumer.receive(5000);
        consumer.close();
        return msg;
    }

    private Destination getDestination(String name) throws JMSException {
        Destination destination;
        if(useTopics)
            destination = session.createTopic(name);
        else
            destination = session.createQueue(name);
        return destination;
    }
    private void sendMessage(String destinationName, Message message) {
        Destination destination = defaultDestination;
        MessageProducer producer = defaultProducer;
        try {
            if (destinationName != null && !destinationName.isEmpty()) {
                destination = getDestination(destinationName);
                producer = session.createProducer(destination);
            }
            producer.send(message);
            if(producer != defaultProducer)
                producer.close();
        } catch (JMSException e) {
            log.error("Failed to publishProperties", e);
        }
    }
}
