package org.jboss.rhiot.beacon.bluez;

/**
 * The generic BLE AD structure used in advertising packets. The values for the type property are defined in:
 * https://www.bluetooth.com/specifications/assigned-numbers/generic-access-profile
 */
public class AdStructure {
    int length;
    int type;
    byte data[];

    public AdStructure(int type, byte[] data) {
        this.length = data.length;
        this.type = type;
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String dataAsHexString() {
        StringBuilder hex = new StringBuilder();
        for(byte b : data) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    public boolean startsWith(byte[] prefix) {
        boolean matches = true;
        for (int n = 0; n < prefix.length; n++) {
            matches &= (data[n] == prefix[n]);
            if(!matches)
                break;
        }
        return matches;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder(String.format("ADS(type=%2X:%d): %s", type, length, dataAsHexString()));
        return tmp.toString();
    }
}
