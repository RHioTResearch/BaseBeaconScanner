package org.jboss.rhiot.beacon.ble;

/**
 * Constants for the commonly used assigned number found in the generic access profile.
 * https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile
 */
public class GAP_UUIDs {
    /** One bit boolean flags for the packet */
    public static final int Flags = 0x1;
    /** Incomplete List of 16-bit Service Class UUIDs */
    public static final int ServiceClassPartial16BitUUIDs = 0x2;
    /** Complete List of 16-bit Service Class UUIDs */
    public static final int ServiceClassComplete16BitUUIDs = 0x3;
    /** Incomplete List of 32-bit Service Class UUIDs */
    public static final int ServiceClassPartial32BitUUIDs = 0x4;
    /** Complete List of 32-bit Service Class UUIDs */
    public static final int ServiceClassComplete32BitUUIDs = 0x5;
    /** Incomplete List of 128-bit Service Class UUIDs */
    public static final int ServiceClassPartial128BitUUIDs = 0x6;
    /** Complete List of 128-bit Service Class UUIDs */
    public static final int ServiceClassComplete128BitUUIDs = 0x7;
    /** Shortened Local Name */
    public static final int ShortenedLocalName = 0x8;
    /** Complete Local Name */
    public static final int CompleteLocalName = 0x9;
    /** Tx Power Level */
    public static final int TxPowerLevel = 0xA;
    /** Class of device */
    public static final int ClassOfDevice = 0xD;

    /** Slave Connection Interval Range */
    public static final int SlaveConnectionIntervalRange = 0x12;

    /** Service Data */
    public static final int ServiceData = 0x16;
    /** Service Data 32-bit UUID */
    public static final int ServiceData32BitUUID = 0x20;
    /** URI data type as UTF-8 */
    public static final int URI = 0x24;
    /** two byte uint16 in 0.625 ms increments */
    public static final int AdvertisingInterval = 0x1A;

    /** Manufacturer Specific Data */
    public static final int ManufacturerSpecificData = 0xFF;

}
