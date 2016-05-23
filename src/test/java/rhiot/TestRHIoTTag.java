package rhiot;

import org.jboss.rhiot.beacon.bluez.*;

import java.nio.*;
import java.util.Date;

/**
 * Test the general scanner receipt and extraction of the custom TI sensortag RHIoTTag firmware that advertises
 * a subset of the sensor data in a hacked eddystone TLM packet's ServiceData structure.
 *
 * -Djava.library.path=/usr/local/lib must be specified on command line in order for this to load the scannerJni lib.
 * This also typically one runs as root or use sudo to enable proper access for the native code.
 */
public class TestRHIoTTag implements IAdvertEventCallback {
    @Override
    public boolean advertEvent(AdEventInfo info) {
        System.out.printf("+++ advertEvent, rssi=%d, time=%s\n", info.getRssi(), new Date(info.getTime()));
        RHIoTTag tag = RHIoTTag.create(info);
        if(tag != null) {
            System.out.printf("%s\n", tag.toFullString());
        }
        return false;
    }

    public static void main(String[] args) {
        int device = 0;
        if(args.length > 0)
            device = Integer.parseInt(args[0]);
        TestRHIoTTag test = new TestRHIoTTag();

        try {
            // Load the native library
            System.loadLibrary("scannerJni");

            String hci = "hci" + device;
            //HCIDump.enableDebugMode(true);
            HCIDump.setAdvertEventCallback(test);
            HCIDump.initScanner(hci, 512, ByteOrder.BIG_ENDIAN);
            long eventCount = 1;
            boolean running = true;
            while (running) {
                Thread.sleep(10);
                if(eventCount % 1000 == 0)
                    System.out.printf("event count=%d\n", eventCount);
            }
            HCIDump.freeScanner();
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
}
