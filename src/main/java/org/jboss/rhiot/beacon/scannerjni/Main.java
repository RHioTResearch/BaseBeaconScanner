package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.logging.Logger;
import org.jboss.rhiot.beacon.bluez.HCIDump;
import org.jboss.rhiot.beacon.common.Inet;
import org.jboss.rhiot.beacon.common.ParseCommand;

import java.net.SocketException;

/**
 * The main entry point for the java scanner that integrates with the JNI bluez stack code
 * Enable the jboss logmanager by passing -Djava.util.logging.manager=org.jboss.logmanager.LogManager
 * @see HCIDump
 */
public class Main {
    private static Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        System.loadLibrary("scannerJni");
        log.info("Loaded native scannerJni library");

        ParseCommand cmdArgs = ParseCommand.parseArgs(args);

        // If scannerID is the string {IP}, replace it with the host IP address
        try {
            cmdArgs.replaceScannerID();
        } catch (SocketException e) {
            log.warn("Failed to read host address info", e);
        }

        HCIDumpParser parser = new HCIDumpParser(cmdArgs);

        try {
            // Start the scanner parser handler threads other than the native stack handler
            parser.start();
            // Setup the native bluetooth stack integration, callbacks, and stack thread
            HCIDump.setRawEventCallback(parser::beaconEvent);
            HCIDump.initScanner(cmdArgs.hciDev);
            // Wait for an external stop notification via the marker file
            parser.waitForStop();
            // Shutdown the parser
            parser.stop();
        } catch (Exception e) {
            log.error("Scanner exiting on exception", e);
        }
        log.infof("End scanning");
        System.exit(0);
    }
}
