package org.jboss.rhiot.beacon.lcd;

import java.util.Date;
import java.util.Properties;

import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.IBeaconMapper;
import org.jboss.rhiot.beacon.common.Beacon;
import org.jboss.rhiot.beacon.common.StatusInformation;
import org.jboss.rhiot.beacon.scannerjni.ScannerView;

/**
 * Base lcd view class that handles the formatting of objects to text
 */
public abstract class AbstractLcdView extends AbstractLcdDisplay implements ScannerView {
    private IBeaconMapper beaconMapper;
    private boolean displayBeaconsMode = true;

    /**
     * Singleton accessor
     */
    public static AbstractLcdView getLcdDisplayInstance(LcdDisplayType type) {
        AbstractLcdView display = null;
        switch(type) {
            case HD44780U:
                display = new WiringPiLcdDisplay();
                break;
            case PCD8544:
                display = new MiniLcdPCD8544();
                break;
        }
        return display;
    }

    public IBeaconMapper getBeaconMapper() {
        return beaconMapper;
    }
    public void setBeaconMapper(IBeaconMapper beaconMapper) {
        this.beaconMapper = beaconMapper;
    }

    @Override
    public boolean isDisplayBeaconsMode() {
        return displayBeaconsMode;
    }
    public void toggleDisplayBeaconsMode() {
        displayBeaconsMode = !displayBeaconsMode;
    }

    /**
     * display format:
     * 0: scannerID
     * 1: Beacon(%d)
     * 2:  rssi
     * 3:  timestamp
     * 4:  Hello user...
     */
    public void displayBeacon(Beacon beacon) {
        clear();
        int col = 1;
        int row = 0;
        displayText(beacon.getScannerID(), 0, row ++);
        int minorID = beacon.getMinor();
        String text = String.format("Beacon(%d):", minorID);
        displayText(text, 0, row ++);
        text = String.format("rssi=%d", beacon.getRssi());
        displayText(text, col, row ++);
        displayTime(beacon.getTime(), col, row ++);
        if(getBeaconMapper() != null) {
            String user = getBeaconMapper().lookupUser(minorID);
            text = String.format("Hello %s", user);
        } else {
            text = "Hello Unknown";
        }
        displayText(text, col, row);
    }

    public void displayHeartbeat(Beacon beacon) {
        String text = String.format("Heartbeat(%d)*:", beacon.getMinor());
        displayText(text, 0, 0);
        text = String.format("rssi=%d", beacon.getRssi());
        displayText(text, 2, 1);
        displayTime(beacon.getTime(), 2, 2);
        text = String.format("No other in range");
        displayText(text, 2, 3);
    }

    static String truncateName(String name) {
        int length = name.length();
        if(length > 8) {
            name = name.substring(0,7) + ".";
        }
        return name;
    }

    public void displayStatus(StatusInformation status){
        String name = truncateName(status.getScannerID());
        truncateName(name);
        String text = String.format("%s:%7d;%d", name, status.getHeartbeatCount(), status.getHeartbeatRSSI());
        displayText(text, 0, 0);
        Properties statusProps = status.getLastStatus();

        text = statusProps.getProperty("UptimeShort");
        displayText(text, 0, 1);
        String load = statusProps.getProperty("LoadAverage");
        displayText(load, 0, 2);
        text = String.format("S:%8d;M:%7d", status.getRawEventCount(), status.getPublishEventCount());
        displayText(text, 0, 3);
    }

    /**
     * display format:
     * 0: scannerID
     * 1: Beacon(%d)
     * 2:  rssi
     * 3:  timestamp
     * 4:  Hello user...
     */
    public void displayBeacon(BeaconInfo beacon) {
        clear();
        int col = 1;
        int row = 0;
        displayText(beacon.getScannerID(), 0, row ++);
        int minorID = beacon.getMinor();
        String text = String.format("Beacon(%d):", minorID);
        displayText(text, 0, row ++);
        text = String.format("rssi=%d", beacon.getRssi());
        displayText(text, col, row ++);
        displayTime(beacon.getTime(), col, row ++);
        if(beaconMapper != null) {
            String user = beaconMapper.lookupUser(minorID);
            text = String.format("Hello %s", user);
        } else {
            text = String.format("Hello Unknown");
        }
        displayText(text, col, row);
    }
    public void displayHeartbeat(BeaconInfo beacon) {
        int col = 1;
        int row = 0;
        displayText(beacon.getScannerID(), 0, row ++);
        int minorID = beacon.getMinor();
        String text = String.format("Heartbeat(%d):", minorID);
        displayText(text, 0, row ++);
        text = String.format("rssi=%d", beacon.getRssi());
        displayText(text, col, row ++);
        displayTime(beacon.getTime(), col, row ++);
        displayText("No other in range", col, row);
    }

    public void displayTime(long timeInMS, int col, int row) {
        String timestr = String.format("%tT", new Date(timeInMS));
        displayText(timestr, col, row);
    }

}
