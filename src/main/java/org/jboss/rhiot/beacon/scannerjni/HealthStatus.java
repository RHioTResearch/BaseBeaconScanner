package org.jboss.rhiot.beacon.scannerjni;

import org.jboss.logging.Logger;
import org.jboss.rhiot.beacon.common.EventsBucket;
import org.jboss.rhiot.beacon.common.Inet;
import org.jboss.rhiot.beacon.common.MsgPublisher;
import org.jboss.rhiot.beacon.common.StatusInformation;
import org.jboss.rhiot.beacon.common.StatusProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by starksm on 7/11/15.
 */
public class HealthStatus {
    private static Logger log = Logger.getLogger(HealthStatus.class);

    private MsgPublisher publisher;
    private StatusInformation statusInformation;
    private Thread monitorThread;
    private volatile boolean running;

    static class SystemInfo {
        private long uptime;
        private String loadAverages;
        private long totalram;
        private long freeram;

        public SystemInfo(long uptime, String loadAverages) {
            this.uptime = uptime;
            this.loadAverages = loadAverages;
        }

        public long getUptime() {
            return uptime;
        }

        public void setUptime(long uptime) {
            this.uptime = uptime;
        }

        public String getLoadAverages() {
            return loadAverages;
        }

        public void setLoadAverages(String loadAverages) {
            this.loadAverages = loadAverages;
        }
    }

    public MsgPublisher getPublisher() {
        return publisher;
    }

    public String getStatusQueue() {
        return statusInformation.getStatusQueue();
    }

    public int getStatusInterval() {
        return statusInformation.getStatusInterval();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /** Begin monitoring in the background, sending status messages to the indicated queue via the publisher
     */
    public void start(MsgPublisher publisher, StatusInformation statusInformation) {
        running = true;
        this.statusInformation = statusInformation;
        this.publisher = publisher;
        monitorThread = new Thread(this::monitorStatus, "HealthStatus");
        monitorThread.start();
        log.infof("HealthStatus::start, runnnig with statusInterval: %d\n", statusInformation.getStatusInterval());
    }

    /**
     * Reset any counters
     */
    public void reset() {
    }
    public void stop() {
        running = false;
        monitorThread.interrupt();
    }

    public static String determineSystemType() {
        return System.getProperty("os.arch");
    }
    public static String determineSystemOS() {
        return System.getProperty("os.name");
    }
    /**
     * Try expression for less than an hour;
     * 12:22:35 up 56 min,  5 users,  load average: 0.92, 0.78, 0.71
     * @param uptime
     * @return
     */
    static boolean parseLessThanHour(String uptime, SystemInfo systemInfo) {
        Pattern parse = Pattern.compile("up\\s+((\\d+) min?,).*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _minutes = matcher.group(2);
            String loadAvgs = matcher.group(3);

            long days = 0;
            long hours = 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);
            systemInfo.setUptime(uptimeMS);
            systemInfo.setLoadAverages(loadAvgs);
        }
        return matched;
    }

    /**
     * Try expression for less than 1 day up;
     * 14:25:50 up  2:59,  5 users,  load average: 0.21, 0.14, 0.14
     * @param uptime
     * @return
     */
    static boolean parseLessThanDay(String uptime, SystemInfo systemInfo) {
        Pattern parse = Pattern.compile("up\\s+(\\d+),\\s+(\\d+):(\\d+),.*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _hours = matcher.group(2);
            String _minutes = matcher.group(3);
            String loadAvgs = matcher.group(4);

            long days = 0;
            long hours = _hours != null ? Integer.parseInt(_hours) : 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);
            systemInfo.setUptime(uptimeMS);
            systemInfo.setLoadAverages(loadAvgs);
        }
        return matched;
    }

    /**
     * 01:25:06 up 1 day,  7:03,  2 users,  load average: 0.09, 0.16, 0.14
     * @param uptime
     * @return
     */
    static boolean parseMoreThanDay(String uptime, SystemInfo systemInfo) {
        Pattern parse = Pattern.compile("((\\d+) day[s]?,)?\\s+(\\d+):(\\d+),.*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _days = matcher.group(2);
            String _hours = matcher.group(3);
            String _minutes = matcher.group(4);
            String loadAvgs = matcher.group(5);

            long days = _days != null ? Integer.parseInt(_days) : 0;
            long hours = _hours != null ? Integer.parseInt(_hours) : 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);
            systemInfo.setUptime(uptimeMS);
            systemInfo.setLoadAverages(loadAvgs);
        }
        return matched;
    }

    static void parse(String uptime, SystemInfo systemInfo) {
        if(parseMoreThanDay(uptime, systemInfo))
            return;;
        if(parseLessThanHour(uptime, systemInfo))
            return;
        if(parseLessThanDay(uptime, systemInfo))
            return;
    }

    public static SystemInfo getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo(0, "0.00 0.00 0.00");
        try {
            Runtime runtime = Runtime.getRuntime();
            Process uptimeProc = runtime.exec("uptime");
            BufferedReader in = new BufferedReader(new InputStreamReader(uptimeProc.getInputStream()));
            String line = in.readLine();
            if(!parseMoreThanDay(line, systemInfo)) {
                if(!parseLessThanDay(line, systemInfo)) {
                    if(!parseLessThanHour(line, systemInfo)) {
                        log.warnf("Failed to parse system uptime: %s", line);
                    }
                }
            }

            long freeram = runtime.freeMemory();
            long totalram = runtime.totalMemory();
            systemInfo.freeram = freeram;
            systemInfo.totalram = totalram;
        } catch (IOException e) {
            log.warn("Failed to get uptime", e);
        }
        return systemInfo;
    }

    protected void monitorStatus() {
        int statusInterval = statusInformation.getStatusInterval();
        String statusQueue = statusInformation.getStatusQueue();
        String scannerID = statusInformation.getScannerID();
        Properties statusProperties = new Properties();
        String ScannerID = StatusProperties.ScannerID.name();
        String HostIPAddress = StatusProperties.HostIPAddress.name();
        String MACAddress = StatusProperties.MACAddress.name();
        String SystemType = StatusProperties.SystemType.name();
        String SystemOS = StatusProperties.SystemOS.name();
        String SystemTime = StatusProperties.SystemTime.name();
        String SystemTimeMS = StatusProperties.SystemTimeMS.name();
        String Uptime = StatusProperties.Uptime.name();
        String UptimeShort = StatusProperties.UptimeShort.name();
        String SystemUptime = StatusProperties.SystemUptime.name();
        String LoadAverage = StatusProperties.LoadAverage.name();
        String Procs = StatusProperties.Procs.name();
        String RawEventCount = StatusProperties.RawEventCount.name();
        String PublishEventCount = StatusProperties.PublishEventCount.name();
        String HeartbeatCount = StatusProperties.HeartbeatCount.name();
        String HeartbeatRSSI = StatusProperties.HeartbeatRSSI.name();
        String EventsWindow = StatusProperties.EventsWindow.name();
        String ActiveBeacons = StatusProperties.ActiveBeacons.name();
        String MemTotal = StatusProperties.MemTotal.name();
        String MemFree = StatusProperties.MemFree.name();
        String MemActive = StatusProperties.MemActive.name();
        String SwapTotal = StatusProperties.SwapTotal.name();
        String SwapFree = StatusProperties.SwapFree.name();

        // Determine the scanner type
        String systemType = determineSystemType();
        log.infof("Determined SystemType as: %s\n", systemType);
        String systemOS = determineSystemOS();
        log.infof("Determined SystemOS as: %s\n", systemOS);

        SystemInfo beginInfo = getSystemInfo();
// Send an initial hello status msg with the host inet address
        char hostIPAddress[] = new char[128];
        char macaddr[] = new char[32];
        try {
            Inet.getHostInfo(hostIPAddress, macaddr);
        } catch (SocketException e) {
            log.warn("Failed to read host address info", e);
        }

        while(running) {
            statusProperties.put(ScannerID, scannerID);
            statusProperties.put(HostIPAddress, hostIPAddress);
            statusProperties.put(MACAddress, macaddr);
            statusProperties.put(SystemType, systemType);
            statusProperties.put(SystemOS, systemOS);

            // Time
            long ms = System.currentTimeMillis();
            String timestr = new Date(ms).toString();
            statusProperties.put(SystemTime, timestr);
            statusProperties.put(SystemTimeMS, "" + ms);
            StringBuilder status = new StringBuilder("--- HealthStatus: ");
            status.append(timestr);
            status.append('\n');

            // Get the load average
            SystemInfo info = getSystemInfo();
            // Create the status message properties
            statusProperties.put(LoadAverage, info.getLoadAverages());
            statusProperties.put(RawEventCount, ""+statusInformation.getRawEventCount());
            statusProperties.put(PublishEventCount, "" + statusInformation.getPublishEventCount());
            statusProperties.put(HeartbeatCount, "" + statusInformation.getHeartbeatCount());
            statusProperties.put(HeartbeatRSSI, "" + statusInformation.getHeartbeatRSSI());
            String tmp = String.format("RawEventCount: %d, PublishEventCount: %d, HeartbeatCount: %d, HeartbeatRSSI: %d\n",
                statusInformation.getRawEventCount(), statusInformation.getPublishEventCount(),
                statusInformation.getHeartbeatCount(), statusInformation.getHeartbeatRSSI());
            status.append(tmp);

            // Events bucket info
            EventsBucket eventsBucket = statusInformation.getStatusWindow();
            if(eventsBucket != null) {
                StringBuilder eventsBucketStr = new StringBuilder();
                eventsBucket.toSimpleString(eventsBucketStr);
                statusProperties.put(EventsWindow, eventsBucketStr.toString());
                status.append(String.format("EventsBucket[%d]: %s\n", eventsBucket.size(), eventsBucketStr.toString()));
                statusProperties.put(ActiveBeacons, ""+eventsBucket.size());
            }

            // System uptime, load, procs, memory info
            int mb = 1024*1024;
            long uptimeDiff = info.uptime - beginInfo.uptime;
            long days = uptimeDiff / (24*3600*1000);
            long hours = (uptimeDiff - days * 24*3600*1000) / (3600*1000);
            long minutes = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000) / (60*1000);
            long seconds = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000 - minutes*60*1000) / 1000;
            String uptime = String.format("uptime: %d, days:%d, hrs:%d, min:%d, sec:%d", uptimeDiff, days, hours, minutes, seconds);
            String uptimeShort = String.format("UP D:%d H:%d M:%d S:%d", days, hours, minutes, seconds);
            statusProperties.put(Uptime, uptime);
            statusProperties.put(UptimeShort, uptimeShort);

            status.append("Scanner ");
            status.append(uptime);
            status.append('\n');

            // Calcualte system uptime
            uptimeDiff = info.uptime;
            days = uptimeDiff / (24*3600*1000);
            hours = (uptimeDiff - days * 24*3600*1000) / (3600*1000);
            minutes = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000) / (60*1000);
            seconds = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000 - minutes*60*1000) / 1000;
            uptime = String.format("uptime: %d, days:%d, hrs:%d, min:%d, sec:%d", uptimeDiff, days, hours, minutes, seconds);
            statusProperties.put(SystemUptime, uptime);
            status.append("System ");
            status.append(uptime);
            status.append('\n');

            status.append("loadavg: ");
            status.append(info.loadAverages);
            status.append('\n');
            statusProperties.put(LoadAverage, info.loadAverages);

            statusProperties.put(MemTotal, ""+info.totalram);
            statusProperties.put(MemActive, ""+(info.totalram - info.freeram));
            statusProperties.put(MemFree, "" + info.freeram);
            status.append(String.format("MemTotal: %d;  MemFree: %d\n", info.totalram, info.freeram));

            // Publish the status
            statusInformation.setLastStatus(statusProperties);
            log.info(status.toString());

            // Wait for statusInterval before next status message
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(statusInterval));
            } catch (InterruptedException e) {
                log.warn("Exiting on InterruptedException");
                running = false;
            }
        }
    }
}
