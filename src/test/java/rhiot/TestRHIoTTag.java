package rhiot;

import org.jboss.rhiot.beacon.bluez.AdEventInfo;
import org.jboss.rhiot.beacon.bluez.AdStructure;
import org.jboss.rhiot.beacon.bluez.HCIDump;
import org.jboss.rhiot.beacon.bluez.IAdvertEventCallback;

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
        AdStructure tagData = info.getADSOfType(0x16);
        if(tagData != null) {
            String data = tagData.dataAsHexString();
            if(data.startsWith("AAFE20")) {
                System.out.printf("Found RHIoTTag data type!!!\n");
                ByteBuffer buffer = ByteBuffer.wrap(tagData.getData());
                // Skip the service id 2 bytes
                buffer.get(); buffer.get();
                /*
                  uint8_t   frameType;      // TLM
                  uint8_t   version;        // 0x00 for now
                  uint8_t   vBatt[2];       // Battery Voltage, 1mV/bit, Big Endian
                  uint8_t   temp[2];        // Temperature. Signed 8.8 fixed point
                  uint8_t   advCnt[4];      // Adv count since power-up/reboot
                  uint8_t   secCnt[4];      // Time since power-up/reboot in 0.1 second resolution
                  // Non-standard TLM data
                  uint8_t   keys;                       //  Bit 0: left key (user button), Bit 1: right key (power button), Bit 2: reed relay
                  uint8_t   lux[2];         // raw optical sensor data, BE
                 */
                byte frameType = buffer.get();
                byte version = buffer.get();
                short vBatt = buffer.getShort();
                byte[] temp = {buffer.get(), buffer.get()};
                double tempC = temp[0] + temp[1]/ 256.0;
                double tempF = 1.8*tempC + 32;
                int advCnt = buffer.getInt();
                int secCnt = buffer.getInt();
                byte keys = buffer.get();
                String keysStr = "";
                if((keys & 0x1) != 0)
                    keysStr += "Left|";
                if((keys & 0x2) != 0)
                    keysStr += "Right|";
                if((keys & 0x4) != 0)
                    keysStr += "Reed";
                short lux = buffer.getShort();
                System.out.printf("vbatt: %dmV, temp:%.2f/%.2f, advCnt: %d, secCnt: %d, lux: %d, keys: %s\n",
                        vBatt, tempC, tempF, advCnt, secCnt, lux, keysStr);
            }
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
