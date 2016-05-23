package org.jboss.rhiot.beacon.bluez;

import org.jboss.rhiot.beacon.common.Beacon;
import org.jboss.rhiot.beacon.scannerjni.HealthStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * This is the Java to native bridge class that exposes the bluez hcidump information as Java objects. It two different
 * modes of running based on what callback type is enabled.
 * {@link #setEventCallback(IEventCallback)} and {@link #setRawEventCallback(IRawEventCallback)} provide iBeacon
 * type of filtering of the data to provide a BeaconInfo callback object.
 * The {@link #setAdvertEventCallback(IAdvertEventCallback)} is a more general scanning mode that provides all
 * BLE advertising events in a AdEventInfo callback object. This is a superset mode of the beacon scanning mode
 * since a beacon event is just an AdEventInfo event with a specific structure for the manufacturer specific
 * AdStructure.
 */
public class HCIDump {
    static final int beacon_info_SIZEOF = 80;
    static final int UUID_OFFSET = 0;
    static final int IS_HEARTBEAT_OFFSET = 36;
    static final int count_OFFSET = 40;
    static final int code_OFFSET = 44;
    static final int manufacturer_OFFSET = 48;
    static final int major_OFFSET = 52;
    static final int minor_OFFSET = 56;
    static final int power_OFFSET = 60;
    static final int calibrated_power_OFFSET = 64;
    static final int rssi_OFFSET = 68;
    static final int time_OFFSET = 72;
    /*
        typedef struct beacon_info {
            char uuid[36];
            bool isHeartbeat;
            int32_t count;
            int32_t code;
            int32_t manufacturer;
            int32_t major;
            int32_t minor;
            int32_t power;
            int32_t calibrated_power;
            int32_t rssi;
            int64_t time;
        } beacon_info;
        root@debian8x64:~/BeaconScannerJNI# Debug/tests/testBeaconBuffer
        sizeof(beacon_info) = 80
        offsetof(beacon_info.isHeartbeat) = 36
        offsetof(beacon_info.count) = 40
        offsetof(beacon_info.code) = 44
        offsetof(beacon_info.manufacturer) = 48
        offsetof(beacon_info.major) = 52
        offsetof(beacon_info.minor) = 56
        offsetof(beacon_info.power) = 60
        offsetof(beacon_info.calibrated_power) = 64
        offsetof(beacon_info.rssi) = 68
        offsetof(beacon_info.time) = 72
    */

    static final int ADI_total_length_OFFSET = 0;
    static final int ADI_bdaddr_type_OFFSET = 4;
    static final int ADI_bdaddr_OFFSET = 5;
    static final int ADI_count_OFFSET = 11;
    static final int ADI_rssi_OFFSET = 12;
    static final int ADI_time_OFFSET = 16;
    static final int ADI_data_OFFSET = 24;
    /*
        OR

        typedef struct ad_data_inline {
            uint8_t	bdaddr_type;
            uint8_t bdaddr[6];
            uint8_t count;
            int32_t rssi;
            int64_t time;
            ad_structure data[];
        } ad_data_inline;

        sizeof(ad_data_inline) = 24
        offsetof(ad_data_inline.total_length) = 0
        offsetof(ad_data_inline.bdaddr_type) = 4
        offsetof(ad_data_inline.bdaddr) = 5
        offsetof(ad_data_inline.count) = 11
        offsetof(ad_data_inline.rssi) = 12
        offsetof(ad_data_inline.time) = 16
        offsetof(ad_data_inline.data) = 24
     */
    private static ByteBuffer theNativeBuffer;
    private static volatile int eventCount = 0;
    private static IRawEventCallback rawEventCallback;
    private static IEventCallback eventCallback;
    private static IAdvertEventCallback advertEventCallback;
    private static String scannerID;

    /** Map the given ByteBuffer to a direct byte buffer that shares memory
      contents between C/Java so that the native byte[] is read through
      the bb instance. This should not be called directly, rather it is called by initScanner.

    @param bb - the java ByteBuffer instance to map to a native byte[] address.
    @param device - the bluetooth hci device number.
    @param isGeneral - is the scanner running for general BLE ad events or just beacons
    */
    public native static void allocScanner(ByteBuffer bb, int device, boolean isGeneral);
    /** Free the native btye buffer array
    */
    public native static void freeScanner();
    /** Enable/disable verbose debug mode output from the native scanner
    */
    public native static void enableDebugMode(boolean flag);

    public static IRawEventCallback getRawEventCallback() {
        return rawEventCallback;
    }

    public static void setRawEventCallback(IRawEventCallback rawEventCallback) {
        HCIDump.rawEventCallback = rawEventCallback;
    }

    public static IEventCallback getEventCallback() {
        return eventCallback;
    }

    public static void setEventCallback(IEventCallback eventCallback) {
        HCIDump.eventCallback = eventCallback;
    }

    public static IAdvertEventCallback getAdvertEventCallback() {
        return advertEventCallback;
    }

    public static void setAdvertEventCallback(IAdvertEventCallback advertEventCallback) {
        HCIDump.advertEventCallback = advertEventCallback;
    }

    public static String getScannerID() {
        return scannerID;
    }

    public static void setScannerID(String scannerID) {
        HCIDump.scannerID = scannerID;
    }

    /**
     * Setup the native scanner stack for the given hciDev interface. This allocates the direct ByteBuffer used by
     * the native stack and starts the scanner running by calling allocScanner. This is a legacy convienence method
     * that calls initScanner(hciDev, beacon_info_SIZEOF, ByteOrder.LITTLE_ENDIAN)
     *
     * @param hciDev - the host controller interface (for example, hci0)
     * @see #allocScanner(ByteBuffer, int, boolean)
     * @see #initScanner(String, int, ByteOrder)
     */
    public static void initScanner(String hciDev) {
        initScanner(hciDev, beacon_info_SIZEOF, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Setup the native scanner stack for the given hciDev interface. This allocates the direct ByteBuffer used by
     * the native stack and starts the scanner running by calling allocScanner. This invokes the allocScanner
     * method with an isGeneral flag that is set based on whether the advertEventCallback has been configured.
     *
     * @see #allocScanner(ByteBuffer, int, boolean)
     * @see #setAdvertEventCallback(IAdvertEventCallback)
     *
     * @param hciDev - the host controller interface (for example, hci0)
     * @param maxBufferSize - the maximum amount of memory to allocate for the native buffer
     * @param order - the endian order of the buffer
     */
    public static void initScanner(String hciDev, int maxBufferSize, ByteOrder order) {
        char devNumber = hciDev.charAt(hciDev.length()-1);
        int device = devNumber - '0';
        ByteBuffer bb = ByteBuffer.allocateDirect(maxBufferSize);
        bb.order(order);
        HCIDump.theNativeBuffer = bb;
        // Set the general scanning mode flag based on whether there is an advertEventCallback
        boolean isGeneral = advertEventCallback != null;
        HCIDump.allocScanner(bb, device, isGeneral);
    }

    /**
     *
     * @param info
     * @param buffer
     */
    public static void freezeBeaconInfo(BeaconInfo info, ByteBuffer buffer) {
        byte uuid[] = new byte[36];
        int uuidLength = 0;
        for (int n = 0; n < uuid.length; n++) {
            byte bn = buffer.get(n);
            if(bn == 0)
                break;
            uuid[n] = bn;
            uuidLength ++;
        }
        info.uuid = new String(uuid, 0, uuidLength);
        info.isHeartbeat = buffer.getInt(IS_HEARTBEAT_OFFSET) != 0;
        info.count = buffer.getInt(count_OFFSET);
        info.code = buffer.getInt(code_OFFSET);
        info.manufacturer = buffer.getInt(manufacturer_OFFSET);
        info.major = buffer.getInt(major_OFFSET);
        info.minor = buffer.getInt(minor_OFFSET);
        info.power = buffer.getInt(power_OFFSET);
        info.calibrated_power = buffer.getInt(calibrated_power_OFFSET);
        info.rssi = buffer.getInt(rssi_OFFSET);
        info.time = buffer.getLong(time_OFFSET);
    }

    /**
     * Extract the inlined ad_data_inline from the ByteBuffer into the info argument.
     * @param info
     * @param buffer
     */
    public static void freezeAdEventInfo(AdEventInfo info, ByteBuffer buffer) {
        //System.out.printf("Begin.freezeAdEventInfo\n");
        int totalLength = buffer.getInt(ADI_total_length_OFFSET);
        //System.out.printf("totalLength=%d\n", totalLength);

        /** The type of the bdaddr; 0 = Public, 1 = Random, other = Reserved */
        int	bdaddr_type = buffer.get(ADI_bdaddr_type_OFFSET);
        //System.out.printf("bdaddr_type=%d\n", bdaddr_type);
        info.setBdaddrType(bdaddr_type);

        /** The address of the advertising packet */
        byte[] bdaddr = new byte[6];
        for(int n = 0; n < 6; n ++)
            bdaddr[n] = buffer.get(ADI_bdaddr_OFFSET + n);
        info.setBDaddr(bdaddr);
        //System.out.printf("bdaddr=%2X:%2X:%2X:%2X:%2X:%2X\n", bdaddr[0], bdaddr[1], bdaddr[2], bdaddr[3], bdaddr[4], bdaddr[5]);

        /** The count of the data[] elements */
        int count = buffer.get(ADI_count_OFFSET);
        System.out.printf("count=%d\n", count);
        info.setCount(count);

        /** The rssi of the advertising packet */
        int rssi = buffer.getInt(ADI_rssi_OFFSET);
        System.out.printf("rssi=%d\n", rssi);
        info.setRssi(rssi);

        /** The time the advertising packet was received */
        long time = buffer.getLong(ADI_time_OFFSET);
        info.setTime(time);

        /** The advertising data structures in the packet. The actual length the data, overall length of structure is length+2
         typedef struct ad_structure {
            uint8_t length;
            uint8_t type;
            uint8_t data[31];
        } ad_structure;
        */
        int offset = ADI_data_OFFSET;
        ArrayList<AdStructure> adData = new ArrayList<>();
        //System.out.printf("Freeze: ADS.count=%d\n", count);
        for(int i = 0; i < count; i ++) {
            int length = buffer.get(offset ++);
            int type = buffer.get(offset ++);
            byte data[] = new byte[length];
            for(int j = 0; j < length; j ++) {
                data[j] = buffer.get(offset ++);
            }
            AdStructure ads = new AdStructure(type, data);
            adData.add(ads);
        }
        //System.out.printf("End.freezeAdEventInfo\n");
        info.setData(adData);
    }

    /**
     * Callback from native code to indicate that theNativeBuffer has been updated with new event data. This happens
     * from the thread that runs the scanner loop and has attached itself to this JavaVM instance. This will dispatch
     * to the advertEventCallback, rawEventCallback, or eventCallback in that preferred order.
     */
    public static boolean eventNotification() {
        boolean stop = false;
        eventCount ++;

        if(advertEventCallback != null) {
            ByteBuffer readOnly = theNativeBuffer.asReadOnlyBuffer();
            readOnly.order(ByteOrder.LITTLE_ENDIAN);
            AdEventInfo info = new AdEventInfo();
            freezeAdEventInfo(info, readOnly);
            stop = advertEventCallback.advertEvent(info);
            return stop;
        }

        if(rawEventCallback != null) {
            try {
                ByteBuffer readOnly = theNativeBuffer.asReadOnlyBuffer();
                readOnly.order(ByteOrder.LITTLE_ENDIAN);
                stop = rawEventCallback.beaconEvent(readOnly);
                return stop;
            } catch (Throwable e) {
                System.err.printf("Error during dispatch to rawEventCallback");
                e.printStackTrace(System.err);
            }
        }

        // Read the native buffer via theNativeBuffer
        try {
            byte uuid[] = new byte[36];
            int uuidLength = 0;
            for (int n = 0; n < uuid.length; n++) {
                byte bn = theNativeBuffer.get(n);
                if(bn == 0)
                    break;
                uuid[n] = bn;
                uuidLength ++;
            }
            String uuidStr = new String(uuid, 0, uuidLength);
            boolean isHeartbeat = theNativeBuffer.getInt(IS_HEARTBEAT_OFFSET) != 0;
            int count = theNativeBuffer.getInt(count_OFFSET);
            int code = theNativeBuffer.getInt(code_OFFSET);
            int manufacturer = theNativeBuffer.getInt(manufacturer_OFFSET);
            int major = theNativeBuffer.getInt(major_OFFSET);
            int minor = theNativeBuffer.getInt(minor_OFFSET);
            int power = theNativeBuffer.getInt(power_OFFSET);
            int calibrated_power = theNativeBuffer.getInt(calibrated_power_OFFSET);
            int rssi = theNativeBuffer.getInt(rssi_OFFSET);
            long time = theNativeBuffer.getLong(time_OFFSET);
            if(eventCallback != null) {
                Beacon beacon = new Beacon(scannerID, uuidStr, code, manufacturer, major, minor, power, rssi, time);
                beacon.setHeartbeat(isHeartbeat);
                beacon.setCount(count);
                stop = eventCallback.beaconEvent(beacon);
            } else {
                System.out.printf("event(%d): %s,%d,%d rssi=%d, time=%d\n", System.currentTimeMillis(), uuidStr, major, minor, rssi, time);
                ByteBuffer readOnly = theNativeBuffer.asReadOnlyBuffer();
                readOnly.order(ByteOrder.LITTLE_ENDIAN);
                BeaconInfo info = new BeaconInfo(readOnly);
                info.setScannerID(scannerID);
                System.out.printf("%s\n", info);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return stop;
    }

    /**
     * Simple main entry point to validate the receipt of the beacon info messages from the bluez stack. This
     * sets the java.library.path to /usr/local/lib so that libscannerJni.so must be installed to that location.
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        int device = 0;
        if(args.length > 0)
            device = Integer.parseInt(args[0]);
        try {
            // Load the native library
            System.setProperty("java.library.path", "/usr/local/lib");
            System.loadLibrary("scannerJni");

            ByteBuffer bb = ByteBuffer.allocateDirect(beacon_info_SIZEOF);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            HCIDump.theNativeBuffer = bb;
            HCIDump.allocScanner(bb, device, false);
            eventCount = 1;
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
