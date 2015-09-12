package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.IBeaconMapper;
import org.jboss.rhiot.beacon.common.StatusInformation;

/**
 * Created by starksm on 7/11/15.
 */
public interface ScannerView {
    public int init();
    public void displayStatus(StatusInformation statusInformation);

    public void displayBeacon(BeaconInfo beaconInfo);
    public void displayHeartbeat(BeaconInfo beaconInfo);

    public boolean isDisplayBeaconsMode();

    public IBeaconMapper getBeaconMapper();
    public void setBeaconMapper(IBeaconMapper beaconMapper);
}
