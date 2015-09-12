package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.rhiot.beacon.common.IBeaconMapper;
import org.jboss.rhiot.beacon.common.StatusInformation;

/**
 * Created by starksm on 7/11/15.
 */
public interface ScannerView {
    public void displayStatus(StatusInformation statusInformation);

    public boolean isDisplayBeaconsMode();

    public IBeaconMapper getBeaconMapper();
    public void setBeaconMapper(IBeaconMapper beaconMapper);
}
