package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.logging.Logger;
import org.jboss.rhiot.beacon.bluez.HCIDump;
import org.jboss.rhiot.beacon.common.Beacon;
import org.jboss.rhiot.beacon.common.IBeaconMapper;
import org.jboss.rhiot.beacon.common.MsgType;
import org.jboss.rhiot.beacon.common.ParseCommand;
import org.jboss.rhiot.beacon.beaconmaps.file.PropertiesMapper;
import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.EventsBucket;
import org.jboss.rhiot.beacon.common.EventsWindow;
import org.jboss.rhiot.beacon.common.MsgPublisher;
import org.jboss.rhiot.beacon.common.MsgPublisherFactory;
import org.jboss.rhiot.beacon.common.StatusInformation;
import org.jboss.rhiot.beacon.lcd.AbstractLcdView;
import org.jboss.rhiot.beacon.lcd.LcdDisplayType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A parser for the {@link BeaconInfo} events generated by the native bluez hcidump integration.
 * @see HCIDump
 */
public class HCIDumpParser {
    private static Logger log = Logger.getLogger(HCIDumpParser.class);
    private static String STOP_MARKER_FILE = "/var/run/scannerd.STOP";

    /** Command line argument information */
    private ParseCommand parseCommand;
    /** The interface for the messaging layer publisher */
    private MsgPublisher publisher;
    /** The thread for the publishing beacon_info events via the MsgPublisher */
    private Thread consumerThread;
    /** The beacon_info event consumer class running in background */
    private BeaconEventConsumer eventConsumer = new BeaconEventConsumer();
    /** Shared queue for producer/consumer message exchange */
    private ConcurrentLinkedDeque<EventsBucket> eventExchanger = new ConcurrentLinkedDeque<>();
    /** An information class published by the HealthStatus task */
    private StatusInformation statusInformation = new StatusInformation();
    /** The ScannerView implementation used to output information about the scanner */
    private ScannerView scannerView;
    /** A background status monitor task class */
    private HealthStatus statusMonitor = new HealthStatus();
    /** The time window of collected beacon_info events */
    private EventsWindow timeWindow = new EventsWindow();
    private IBeaconMapper beaconMapper;
    private volatile boolean stopped;
    private int lastRSSI[] = new int[10];
    private int heartbeatCount = 0;
    private long eventCount = 0;
    private long maxEventCount = 0;
    private long lastMarkerCheckTime = 0;

    /**
     * Remove the stop marker file if it exists
     */
    public static void removeStopMarker() {
        File test = new File(STOP_MARKER_FILE);
        boolean exists = test.exists();
        if(exists) {
            if(!test.delete()) {
                log.warnf("Failed to remove stop marker: %s", STOP_MARKER_FILE);
            }
        }
    }

    /**
     * Test if the stop marker file exists
     * @return true if the stop marker file exists
     */
    public static boolean stopMarkerExists() {
        File test = new File(STOP_MARKER_FILE);
        boolean stop = test.exists();
        if(stop) {
            log.info("Found STOP marker file, will exit...");
        }
        return stop;
    }

    public HCIDumpParser(ParseCommand cmdArgs) {
        this.parseCommand = cmdArgs;
    }

    public IBeaconMapper getBeaconMapper() {
        return beaconMapper;
    }

    public void setBeaconMapper(IBeaconMapper beaconMapper) {
        this.beaconMapper = beaconMapper;
    }

    public void run() {
        try {
            start();
        } catch (Exception e) {
            log.error("Failed to start scanner", e);
        }
    }

    public void start() throws Exception {
        stopped = false;
        eventConsumer.setParseCommand(parseCommand);
        String clientID = parseCommand.clientID;
        if (clientID.isEmpty())
            clientID = parseCommand.scannerID;
        timeWindow.reset(parseCommand.analyzeWindow);
        // Setup the status information
        statusInformation.setScannerID(parseCommand.getScannerID());
        statusInformation.setStatusInterval(parseCommand.statusInterval);
        statusInformation.setStatusQueue(parseCommand.getStatusDestinationName());
        statusInformation.setBcastAddress(parseCommand.getBcastAddress());
        statusInformation.setBcastPort(parseCommand.getBcastPort());

        if (parseCommand.isAnalyzeMode()) {
            log.infof("Running in analyze mode, window=%d seconds, begin=%d\n", parseCommand.getAnalyzeWindow(),
                timeWindow.getBegin());
        }
        else if (!parseCommand.isSkipPublish()) {
            String username = parseCommand.getUsername();
            String password = parseCommand.getPassword();
            publisher = MsgPublisherFactory.newMsgPublisher(parseCommand.getPubType(), parseCommand.getBrokerURL(), username, password, clientID);
            publisher.setUseTopics(!parseCommand.isUseQueues());
            log.infof("setUseTopics: %s\n", publisher.isUseTopics() ? "true" : "false");
            publisher.setDestinationName(parseCommand.getDestinationName());
            if (parseCommand.batchCount > 0) {
                publisher.setUseTransactions(true);
                log.infof("Enabled transactions\n");
            }
            publisher.start(parseCommand.isAsyncMode());
            log.info("Publisher started");

            // Create a thread for the consumer unless running in battery test mode
            if(!parseCommand.isBatteryTestMode()) {
                eventConsumer.init(eventExchanger, publisher, statusInformation);
                consumerThread = new Thread(eventConsumer::publishEvents, "BeaconEventConsumer");
                consumerThread.setDaemon(true);
                consumerThread.start();
                log.infof("Started event consumer thread\n");
            }
        }
        else {
            log.infof("Skipping publish of parsed beacons\n");
        }

        // If the status interval is > 0, start the health status monitor
        if(parseCommand.getStatusInterval() > 0) {
            statusMonitor.start(publisher, statusInformation);
        }

        // Load the beacon mapping
        if(parseCommand.beaconMapping != null) {
            loadBeaconMapping(parseCommand.beaconMapping);
        }

        // Try to load the scanner view
        if(parseCommand.lcdType != LcdDisplayType.INVALID_LCD_TYPE) {
            log.infof("Trying to load LCD type: %s", parseCommand.lcdType);
            try {
                scannerView = AbstractLcdView.getLcdDisplayInstance(parseCommand.lcdType);
                log.info("Loaded LCD instance");
                scannerView.init();
                if(beaconMapper != null)
                    scannerView.setBeaconMapper(beaconMapper);
            } catch (Throwable error) {
                log.warn("Failed to load LCD instance", error);
            }
        }
    }

    public void stop() throws Exception {
        stopped = true;
        if(consumerThread != null)
            consumerThread.interrupt();
        if(publisher != null)
            publisher.stop();
        statusMonitor.stop();
    }

    /**
     * Callback from native stack
     * @param rawInfo - the raw direct ByteBuffer store shared with native stack
     */
    public boolean beaconEvent(ByteBuffer rawInfo) {
        // First get a read only ByteBuffer view for efficient testing of the event info
        BeaconInfo info = new BeaconInfo(rawInfo);
        info.setScannerID(parseCommand.scannerID);
        eventCount ++;
        if(log.isTraceEnabled()) {
            log.tracef("beaconEvent(), uuid=%s, major=%d, minor=%d, rssi=%d\n",
                info.uuid, info.major, info.minor, info.rssi);
        }

        // Check for a termination marker every 1000 events or 5 seconds
        boolean stop = false;
        long elapsed = info.time - lastMarkerCheckTime;
        if((eventCount % 1000) == 0 || elapsed > 5000) {
            lastMarkerCheckTime = info.time;
            stop = stopMarkerExists();
            log.infof("beaconEvent(time=%d), status eventCount=%d, stop=%b\n", info.time, eventCount, stop);
        }
        // Check max event count limit
        if(maxEventCount > 0 && eventCount >= maxEventCount)
            stop = true;

        // Check for heartbeat
        boolean isHeartbeat = parseCommand.heartbeatUUID.compareTo(info.uuid) == 0;
        if(parseCommand.isBatteryTestMode()) {
            // Send the raw unaveraged heartbeat info, or ignore non-heartbeat events
            if(isHeartbeat)
                sendRawHeartbeat(info);
        } else {
            // Merge the event into the current time window
            EventsBucket bucket = timeWindow.addEvent(info, isHeartbeat);
            statusInformation.addEvent(info, isHeartbeat);
            // Now handle the bucket if a new one has been created
            if (bucket != null) {
                if (!parseCommand.isSkipPublish()) {
                    eventExchanger.offerLast(bucket);
                } else {
                    if (parseCommand.isAnalyzeMode()) {
                        printBeaconCounts(bucket);
                    } else {
                        if (!isHeartbeat || (isHeartbeat && !parseCommand.isSkipHeartbeat()))
                            printBeaconCounts(info, bucket);
                    }
                }
                // Display either the closest beacon or status
                if (scannerView != null) {
                    if (scannerView.isDisplayBeaconsMode())
                        displayClosestBeacon(bucket);
                    else
                        displayStatus();
                }
            }
        }
        // If stop is true, notify any callers in waitForStop
        if(stop) {
            synchronized (this) {
                stopped = true;
                this.notifyAll();
            }
        }
        return stop;
    }

    /**
     *
     */
    public synchronized void waitForStop() {
        try {
            while(stopped == false)
                this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void printBeaconCounts(BeaconInfo beacon, EventsBucket bucket) {
        log.infof("Window: parsed(%s):\n", beacon.toString());
        printBeaconCounts(bucket);
    }
    void printBeaconCounts(EventsBucket bucket) {
        StringBuilder tmp = new StringBuilder();
        bucket.toString(tmp);
        log.infof("%s\n", tmp.toString());
    }

    void sendRawHeartbeat(BeaconInfo info) {
        Beacon beacon = new Beacon(parseCommand.getScannerID(), info.uuid, info.code, info.manufacturer, info.major, info.minor,
            info.power, info.rssi, info.time);
        beacon.setMessageType(MsgType.SCANNER_HEARTBEAT.ordinal());
        publisher.publishStatus(beacon);
        lastRSSI[heartbeatCount%10] = info.rssi;
        heartbeatCount ++;
        if(heartbeatCount % 100 == 0) {
            log.infof("Last[10].RSSI:");
            for(int n = 0; n < 10; n ++)
                log.infof("%d,", lastRSSI[n]);
            log.infof("\n");
        }
    }

    /**
     * Display the closest beacon seen in the given even bucket
     * @param bucket collection of beacon events seen in last time window
     */
    void displayClosestBeacon(EventsBucket bucket) {
        int maxRSSI = -100;
        BeaconInfo closest = null;
        BeaconInfo heartbeat = null;
        for(Map.Entry<Integer,BeaconInfo> iter : bucket.getBucket().entrySet()) {
            if(iter.getValue().getRssi() > maxRSSI) {
                BeaconInfo info = iter.getValue();
                if(parseCommand.heartbeatUUID.compareTo(info.getUuid()) == 0)
                    heartbeat = info;
                else {
                    maxRSSI = info.rssi;
                    closest = info;
                }
            }
        }
        if(closest != null) {
            log.infof("closestBeacon: %s", closest);
            scannerView.displayBeacon(closest);
        } else if(heartbeat != null) {
            // The only beacon seen was the heartbeat beacon, so display it
            scannerView.displayHeartbeat(heartbeat);
        }
    }

    /**
     * Display the current scanner status on the scannerview
     */
    void displayStatus() {
        scannerView.displayStatus(statusInformation);
    }

    private void loadBeaconMapping(String beaconMapping) {
        if (beaconMapping.startsWith("file:")) {
            File propFile = new File(beaconMapping.substring(5));
            try {
                this.beaconMapper = new PropertiesMapper(propFile);
            } catch (IOException e) {
                log.warn("Failed to load beacon mapping file: " + beaconMapping, e);
            }
        } else {
            this.beaconMapper = new PropertiesMapper(beaconMapping);
        }
    }
}
