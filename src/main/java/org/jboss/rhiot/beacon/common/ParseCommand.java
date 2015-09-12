package org.jboss.rhiot.beacon.common;/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.rhiot.beacon.lcd.LcdDisplayType;

/**
 * The command line args for the scanner
 *
 * @author Scott Stark (sstark@redhat.com) (C) 2014 Red Hat Inc.
 */
@Parameters(commandDescription = "Parse the hcidump output into beacon msgs pushed to a msg broker")
public class ParseCommand {
    @Parameter(names = "-scannerID",
        description = "Specify the ID of the scanner reading the beacon events. If this is a string with a comma separated list of names, the scanner will cycle through them. If this is the string {IP}, the host ip address will be used.")
    public String scannerID = "{IP}";
    @Parameter(names = "-heartbeatUUID", description = "Specify the UUID of the beacon used to signal the scanner heartbeat event")
    public String heartbeatUUID = "";

    @Parameter(names = "-clientID",
        description = "Specify the clientID to connect to the msg broker with")
    public String clientID = "";
    @Parameter(names = "-username",
        description = "Specify the sshUsername to connect to the msg broker with")
    public String username = null;
    @Parameter(names = "-password",
        description = "Specify the sshPassword to connect to the msg broker with")
    public String password = null;
    @Parameter(names = "-brokerURL",
        description = "Specify the brokerURL to connect to the msg broker with; default tcp://localhost:1883")
    public String brokerURL = "tcp://localhost:1883";
    @Parameter(names = "-destinationName",
        description = "Specify the name of the destination on the msg broker to publish to; default beaconEvents")
    public String destinationName = "beaconEvents";
    @Parameter(names = "-statusDestinationName",
        description = "Specify the name of the status health destination; default scannerHealth")
    public String statusDestinationName = "scannerHealth";
    @Parameter(names = "-statusInterval",
        description = "Specify the interval in seconds between health status messages, <= 0 means no messages; default 30")
    public int statusInterval = 30;

    @Parameter(names = "-pubType",
        description = "Specify the MsgPublisherType enum for the publisher implementation to use; default AMQP_QPID")
    public MsgPublisherType pubType = MsgPublisherType.AMQP_QPID;
    @Parameter(names = "-useQueues",
        description = "Specify whether destination is a queue; default false == destination is a topic")
    public boolean useQueues = false;
    @Parameter(names = "-lcdType",
        description = "Specify the LcdDisplayType enum for the LCD implementation to use; default PCD8544")
    public LcdDisplayType lcdType = LcdDisplayType.PCD8544;

    @Parameter(names = "-analyzeWindow",
        description = "Specify the number of seconds in the analyzeMode time window, default is 1.")
    public int analyzeWindow = 1;
    @Parameter(names = "-hciDev",
        description = "Specify the name of the host controller interface to use; default hci0")
    public String hciDev = "hci0";
    @Parameter(names = "-skipPublish",
        description = "Indicate that the parsed beacons should not be published")
    public boolean skipPublish;
    @Parameter(names = "-noParsing",
        description = "Indicate that the hcidump stream should not be parsed, just made available")
    public boolean noParsing;
    @Parameter(names = "-skipHeartbeat",
        description = "Don't publish the heartbeat messages. Useful to limit the noise when testing the scanner.")
    public boolean skipHeartbeat = false;
    @Parameter(names = "-noBrokerReconnect",
        description = "Don't try to reconnect to the broker on failure, just exit")
    public boolean noBrokerReconnect = false;
    @Parameter(names = "-skipScannerView",
        description = "Skip the scanner view display of closest beacon")
    public boolean skipScannerView = false;
    @Parameter(names = "-analzyeMode",
        description = "Run the scanner in a mode that simply collects beacon readings and reports unique beacons seen in a time window")
    public boolean analyzeMode = false;
    @Parameter(names = "-asyncMode",
        description = "Indicate that the parsed beacons should be published using async delivery mode")
    public boolean asyncMode = false;
    @Parameter(names = "-batteryTestMode",
        description = "Simply monitor the raw heartbeat beacon events and publish them to the destinationName")
    public boolean batteryTestMode = false;
    @Parameter(names = "-useScannerConf",
            description = "Load the scanner.conf file and populate the command line args from it. Searches current dir and home dir.")
    public boolean useScannerConf = false;
    @Parameter(names = "-bcastAddress",
        description = "Address to broadcast scanner status to as backup to statusQueue if non-empty; default empty")
    public String bcastAddress;
    @Parameter(names = "-bcastPort",
        description = "Port to broadcast scanner status to as backup to statusQueue if non-empty; default 12345")
    public int bcastPort = 12345;
    @Parameter(names = "-batchCount",
        description = "Specify a maxium number of events the scanner should combine before sending to broker; default 0 means no batching")
    public int batchCount = 0;

    @Parameter(names = "-beaconMapping",
        description = "Specify the source of the beacon id to user name mapping. The following formats are understood:\n"
            + "string of id1=user1,id2=user2,... mappings"
            + "file:path to java properties file with beaconid=userid mappings\n"
    )
    public String beaconMapping = null;

    @Parameter(names = {"-help", "-h", "-?"}, help = true)
    public boolean help;

    public String getScannerID() {
        return scannerID;
    }

    public String getHeartbeatUUID() {
        return heartbeatUUID;
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getStatusDestinationName() {
        return statusDestinationName;
    }

    public int getStatusInterval() {
        return statusInterval;
    }

    public MsgPublisherType getPubType() {
        return pubType;
    }

    public boolean isUseQueues() {
        return useQueues;
    }

    public int getAnalyzeWindow() {
        return analyzeWindow;
    }

    public String getHciDev() {
        return hciDev;
    }

    public boolean isSkipPublish() {
        return skipPublish;
    }

    public boolean isNoParsing() {
        return noParsing;
    }

    public boolean isSkipHeartbeat() {
        return skipHeartbeat;
    }

    public boolean isNoBrokerReconnect() {
        return noBrokerReconnect;
    }

    public boolean isSkipScannerView() {
        return skipScannerView;
    }

    public boolean isAnalyzeMode() {
        return analyzeMode;
    }

    public boolean isAsyncMode() {
        return asyncMode;
    }

    public boolean isBatteryTestMode() {
        return batteryTestMode;
    }

    public int getBatchCount() {
        return batchCount;
    }

    public String getBcastAddress() {
        return bcastAddress;
    }

    public int getBcastPort() {
        return bcastPort;
    }

    @Override
    public String toString() {
        return "ParseCommand{" +
                "scannerID='" + scannerID + '\'' +
                ", heartbeatUUID='" + heartbeatUUID + '\'' +
                ", clientID='" + clientID + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", brokerURL='" + brokerURL + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", statusDestinationName='" + statusDestinationName + '\'' +
                ", statusInterval=" + statusInterval +
                ", pubType=" + pubType +
                ", useQueues=" + useQueues +
                ", analyzeWindow=" + analyzeWindow +
                ", hciDev='" + hciDev + '\'' +
                ", skipPublish=" + skipPublish +
                ", noParsing=" + noParsing +
                ", skipHeartbeat=" + skipHeartbeat +
                ", noBrokerReconnect=" + noBrokerReconnect +
                ", skipScannerView=" + skipScannerView +
                ", analyzeMode=" + analyzeMode +
                ", asyncMode=" + asyncMode +
                ", batteryTestMode=" + batteryTestMode +
                ", bcastAddress='" + bcastAddress + '\'' +
                ", bcastPort=" + bcastPort +
                ", batchCount=" + batchCount +
                ", beaconMapping='" + beaconMapping + '\'' +
                '}';
    }

    /**
     * If scannerID is the string {IP}, replace it with the host IP address
     */
    public void replaceScannerID() throws SocketException {
        if(scannerID.compareTo("{IP}") == 0) {
            char hostIPAddress[] = new char[128];
            char macaddr[] = new char[32];
                Inet.getHostInfo(hostIPAddress, macaddr);
                scannerID = new String(hostIPAddress);
        }
    }
    public static ParseCommand parseArgs(List<String> args) {
        String[] argsArray = new String[args.size()];
        args.toArray(argsArray);
        return parseArgs(argsArray);
    }
    public static ParseCommand parseArgs(String[] args) {
        ParseCommand cmdArgs = new ParseCommand();
        JCommander cmdArgParser = new JCommander(cmdArgs);
        cmdArgParser.parse(args);

        if(cmdArgs.help)
            cmdArgParser.usage();
        if(cmdArgs.useScannerConf) {
            // Load the scanner.conf and reparse the arguments
            try {
                String[] newArgs = loadScannerConf();
                if(newArgs != null) {
                    // Parse new args into existing cmdArgs instance
                    cmdArgParser = new JCommander(cmdArgs);
                    cmdArgParser.parse(newArgs);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cmdArgs;
    }

    /**
     * Search the current directory and user home for a scanner.conf properties file
     * @return the command line args for the scanner.conf properties if found, null otherwise
     * @throws IOException
     */
    public static String[] loadScannerConf() throws IOException {
        String[] newArgs = null;
        // First check current directory
        File scannerConf = new File("scanner.conf");
        if(scannerConf.exists() == false) {
            // Check user home directory
            String home = System.getenv("HOME");
            scannerConf = new File(home, "scanner.conf");
        }
        if(scannerConf.exists()) {
            // Load the properties and build a new args array from them
            FileReader confReader = new FileReader(scannerConf);
            Properties scannerProps = new Properties();
            scannerProps.load(confReader);
            ArrayList tmp = new ArrayList();
            for(String key : scannerProps.stringPropertyNames()) {
                tmp.add("-"+key);
                tmp.add(scannerProps.getProperty(key));
            }
            newArgs = new String[tmp.size()];
            tmp.toArray(newArgs);
        }
        return newArgs;
    }
}
