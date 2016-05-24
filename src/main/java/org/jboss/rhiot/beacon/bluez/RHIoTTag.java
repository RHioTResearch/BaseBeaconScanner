package org.jboss.rhiot.beacon.bluez;

import org.jboss.rhiot.beacon.ble.GAP_UUIDs;

import java.nio.ByteBuffer;

/**
 * The RHIoT Tag object interface that extracts the tag's sensor information from the BLE AdEventInfo.
 */
public class RHIoTTag {
    public static byte[] SERVICE_DATA_PREFIX = {(byte)0xAA, (byte)0xFE, 0x20};

    /**
     * A factory method that builds a RHIoTTag from BLE advertising events that contain the expected ServiceData.
     *
     * @param info - the BLE advertising event info
     * @return the newly created RHIoTTag if the event info contains the correct ServiceData, null if it does not.
     */
    public static RHIoTTag create(AdEventInfo info) {
        RHIoTTag tag = null;
        AdStructure tagData = info.getADSOfType(GAP_UUIDs.ServiceData);
        if(tagData != null) {
            if (tagData.startsWith(SERVICE_DATA_PREFIX)) {
                tag = new RHIoTTag(tagData.getData());
                tag.setAddress(info.bdaddr);
            }
        }
        return tag;
    }


    private short vBatt;
    private double tempC;
    private int advCnt;
    private int secCnt;
    private byte keys;
    private short lux;
    /** The BLE address fo the sending tag */
    private byte[] address;

    /**
     * Create an object from the data from the AdStructure with the tags ServiceData value. The data array
     * represents the following encoded structure information:
     uint8_t   frameType;      // TLM
     uint8_t   version;        // 0x00 for now
     uint8_t   vBatt[2];       // Battery Voltage, 1mV/bit, Big Endian
     uint8_t   temp[2];        // Temperature. Signed 8.8 fixed point
     uint8_t   advCnt[4];      // Adv count since power-up/reboot
     uint8_t   secCnt[4];      // Time since power-up/reboot in 0.1 second resolution
     // Non-standard TLM data
     uint8_t   keys;           //  Bit 0: left key (user button), Bit 1: right key (power button), Bit 2: reed relay
     uint8_t   lux[2];         // raw optical sensor data, BE

     * @param data - the data from the AdStructure of type ServiceData(0x16).
     */
    public RHIoTTag(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Skip the service 0xAAFE id 2 bytes
        buffer.get();
        buffer.get();
        // Skip frameType
        buffer.get();
        // Skip version
        buffer.get();
        // battery
        vBatt = buffer.getShort();
        byte[] temp = {buffer.get(), buffer.get()};
        tempC = temp[0] + temp[1] / 256.0;
        advCnt = buffer.getInt();
        secCnt = buffer.getInt();
        keys = buffer.get();
        lux = buffer.getShort();
    }

    public String keysString() {
        String keysStr = "";
        if ((keys & 0x1) != 0)
            keysStr += "Left|";
        if ((keys & 0x2) != 0)
            keysStr += "Right|";
        if ((keys & 0x4) != 0)
            keysStr += "Reed";
        return keysStr;
    }

    /**
     * @return the battery voltage in mV
     */
    public short getvBatt() {
        return vBatt;
    }

    /**
     * @return the sensor temperature in degrees C
     */
    public double getTempC() {
        return tempC;
    }

    /**
     * @return The number of advertising packets sent out since startup
     */
    public int getAdvCnt() {
        return advCnt;
    }

    /**
     * @return The time since power up in tenths of a second
     */
    public int getSecCnt() {
        return secCnt;
    }

    public String getTimeUpString() {
        int days = secCnt / (36000 * 24);
        int remainder = secCnt - days * 36000 * 24;
        int hours = remainder / 36000;
        remainder = remainder - hours * 36000;
        int minutes = remainder / 600;
        remainder = remainder - minutes * 600;
        int secs = remainder / 10;
        return String.format("%d days, %d hours, %d mins, %d secs", days, hours, minutes, secs);
    }

    /**
     * Bit 0: left key (user button), Bit 1: right key (power button), Bit 2: reed relay
     * @return the key state bit map.
     */
    public byte getKeys() {
        return keys;
    }

    /**
     *
     * @return
     */
    public short getLux() {
        return lux;
    }

    /**
     * @return The BLE address of the sending tag if set
     */
    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public String toFullString() {
        StringBuilder tmp = new StringBuilder("RHIoTTag(");
        if(address != null) {
            for (int n = 0; n < address.length; n ++) {
                tmp.append(String.format("%02X", address[n]));
                tmp.append(':');
            }
            tmp.setLength(tmp.length()-1);
        }
        tmp.append(')');
        tmp.append(String.format(": battery: %dmV", vBatt));
        tmp.append(String.format(", temp: %.2fC", tempC));
        tmp.append(String.format(", keys: %s", keysString()));
        tmp.append(String.format(", lux: %d raw", lux));
        tmp.append(String.format(", advertCnt: %d", advCnt));
        tmp.append(String.format(", timeUp: %s", getTimeUpString()));

        return tmp.toString();
    }
}
