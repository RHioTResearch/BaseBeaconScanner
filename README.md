# BaseBeaconScanner
The framework independent part of the BLE scanner. Build and install this using:

  mvn install

This does depend on the [BeaconScannerJNI](https://github.com/RHioTResearch/BeaconScannerJNI.git) project in order to load
the beacon scanner native library.

In order to have the scanner display information on an attached LCD display, you also need to build the
[LCDDisplays](https://github.com/RHioTResearch/LCDDisplays.git) project to create the lcdDisplay native code used by the
org.jboss.rhiot.beacon.lcd package.
