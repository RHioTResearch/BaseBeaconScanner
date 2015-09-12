package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.rhiot.beacon.common.AbstractBeaconMapper;
import org.jboss.rhiot.beacon.common.StatusInformation;

/**
 * Created by starksm on 7/11/15.
 */
public interface ScannerView {
    public void displayStatus(StatusInformation statusInformation);

    public boolean isDisplayBeaconsMode();

    public AbstractBeaconMapper getBeaconMapper();
    public void setBeaconMapper(AbstractBeaconMapper beaconMapper);
}
