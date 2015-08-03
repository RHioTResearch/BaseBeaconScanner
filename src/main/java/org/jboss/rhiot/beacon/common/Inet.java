package org.jboss.rhiot.beacon.common;


import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Networking related utils
 */
public class Inet {
    public static String MACADDR_FORMAT = "%02x:%02x:%02x:%02x:%02x:%02x";

    public static class InterfaceConfig {
        private String macaddr;
        private List<InterfaceAddress> addressList;

        public InterfaceConfig(String macaddr, List<InterfaceAddress> addressList) {
            this.macaddr = macaddr;
            this.addressList = addressList;
        }

        public String getMacaddr() {
            return macaddr;
        }

        public List<InterfaceAddress> getAddressList() {
            return addressList;
        }
        public String toString() {
            return String.format("{macaddr=%s, addresses=%s}", macaddr, addressList);
        }
    }

    /**
     * Obtain the host IP address and mac address
     * @param hostIPAddress - the host ip dot decimal address as a char[]
     * @param macaddr - the host interface mac address as a hex char[]
     * @throws SocketException
     */
    public static void getHostInfo(char hostIPAddress[], char macaddr[]) throws SocketException {
        List<InterfaceConfig> ifaces = getAllAddress();
        // Just use the first interface
        for (InterfaceConfig config : ifaces) {
            if(config.getAddressList().size() > 0) {
                char[] mac = config.getMacaddr().toCharArray();
                System.arraycopy(mac, 0, macaddr, 0, mac.length);
                InterfaceAddress host = config.getAddressList().get(0);
                char[] ip = host.getAddress().getHostAddress().toCharArray();
                System.arraycopy(ip, 0, hostIPAddress, 0, ip.length);
            }
        }
    }

    /**
     * Return a list of all NetworkInterface hardware addresses in %x:%x:%x:%x:%x:%x format
     *
     * @return NetworkInterface hardware addresses
     * @throws SocketException
     */
    public static List<String> getAllHardwareAddress() throws SocketException {
        ArrayList<String> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            byte[] macaddr = iface.getHardwareAddress();
            if (macaddr == null)
                continue;
            String key = String.format(MACADDR_FORMAT, macaddr[0], macaddr[1], macaddr[2], macaddr[3], macaddr[4], macaddr[5]);
            addresses.add(key);
        }
        return addresses;
    }

    /**
     * Return a list of all NetworkInterface hardware addresses and addresses
     * @return
     * @throws SocketException
     */
    public static List<InterfaceConfig> getAllAddress() throws SocketException {
        ArrayList<InterfaceConfig> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            byte[] macaddr = iface.getHardwareAddress();
            if (macaddr == null)
                continue;
            String key = String.format(MACADDR_FORMAT, macaddr[0], macaddr[1], macaddr[2], macaddr[3], macaddr[4], macaddr[5]);
            List<InterfaceAddress> addressList = iface.getInterfaceAddresses();
            InterfaceConfig ic = new InterfaceConfig(key, addressList);
            addresses.add(ic);
        }
        return addresses;
    }

}
