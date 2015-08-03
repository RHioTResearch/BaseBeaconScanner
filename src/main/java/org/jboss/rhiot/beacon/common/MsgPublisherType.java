package org.jboss.rhiot.beacon.common;

/**
 * Enum for the types of MsgPublisher/MsgPublisherFactory implementations
 */
public enum MsgPublisherType {
    PAHO_MQTT("org.jboss.rhiot.beacon.publishers.paho.MsgPublisherFactory"),
    AMQP_PROTON(null),
    AMQP_CMS(null),
    AMQP_QPID("org.jboss.rhiot.beacon.publishers.qpid.MsgPublisherFactory");

    public String getFactoryClass() {
        return factoryClass;
    }

    private String factoryClass;
    /**
     * Map the enum to the class implementing the MsgPublisherFactory
     * @param factoryClass
     */
    private MsgPublisherType(String factoryClass) {
        this.factoryClass = factoryClass;
    }
}
