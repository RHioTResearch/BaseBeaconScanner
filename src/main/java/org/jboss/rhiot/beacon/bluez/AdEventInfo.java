package org.jboss.rhiot.beacon.bluez;

import java.util.Date;
import java.util.List;

/**
 * Top level object for a general BLE advertising event. This gives the packet address, type, rssi, time and
 * the number of AdStructures found in the event msg.
 */
public class AdEventInfo {
    /** The type of the bdaddr; 0 = Public, 1 = Random, other = Reserved */
    int	bdaddr_type;
    /** The address of the advertising packet */
    byte bdaddr[];
    /** The count of the data[] elements */
    int count;
    /** The rssi of the advertising packet */
    int rssi;
    /** The time the advertising packet was received */
    long time;
    /** The advertising data structures in the packet */
    List<AdStructure> data;

    public int getBdaddrType() {
        return bdaddr_type;
    }

    public void setBdaddrType(int bdaddr_type) {
        this.bdaddr_type = bdaddr_type;
    }

    public byte[] getBDaddr() {
        return bdaddr;
    }
    public String getBDaddrAsString() {
        StringBuilder tmp = new StringBuilder();
        for(byte b : bdaddr) {
            tmp.append(String.format("%2X:", b));
        }
        tmp.setLength(tmp.length()-1);
        return tmp.toString();
    }

    public void setBDaddr(byte[] bdaddr) {
        this.bdaddr = bdaddr;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<AdStructure> getData() {
        return data;
    }

    public void setData(List<AdStructure> data) {
        this.data = data;
    }

    public AdStructure getADSOfType(int type) {
        AdStructure ads = null;
        for(AdStructure test : data) {
            if(test.getType() == type) {
                ads = test;
                break;
            }
        }
        return ads;
    }
    public String toString() {
        StringBuilder tmp = new StringBuilder(String.format("AdEventInfo(%s/%d): rssi=%d, time=%s\n", getBDaddrAsString(), bdaddr_type, rssi, new Date(time)));
        for(AdStructure ads : data) {
            tmp.append('\t');
            tmp.append(ads);
            tmp.append('\n');
        }
        return tmp.toString();
    }
}
