package system;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import org.jboss.rhiot.beacon.common.ParseCommand;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by starksm on 9/11/15.
 */
public class TestScannerConf {
    @Test
    public void testParseCommnd() {
        ParseCommand cmd = new ParseCommand();
        JCommander cmdParser = new JCommander(cmd);
        String[] args = {"-scannerID", "{IP}", "-statusInterval", "45", "-useQueues", "-useScannerConf"};
        cmdParser.parse(args);
        List<ParameterDescription> params = cmdParser.getParameters();
        for(ParameterDescription param : params) {
            if(param.isAssigned()) {
                int arity = param.getParameter().arity();
                Object defaultValue = param.getDefault();
                boolean assigned = param.isAssigned();
                System.out.printf("%s, arity=%d, default=%s, assigned=%s\n", param.getLongestName(), arity, defaultValue.getClass(), assigned);
            }
        }
    }
    @Test
    public void testScannerConfOverride() {
        Properties scannerConf = new Properties();
        scannerConf.setProperty("scannerID", "{IP}");
        scannerConf.setProperty("heartbeatUUID", "DAF246CEF20111E4B116123B93F75CBA");
        scannerConf.setProperty("brokerURL", "192.168.1.107:5672");
        scannerConf.setProperty("username", "demo-user");
        scannerConf.setProperty("password", "2015-summit-user");
        scannerConf.setProperty("beaconMapping", "301=Scott,300=Tony,303=Anthony");

        ParseCommand cmd = new ParseCommand();
        JCommander cmdParser = new JCommander(cmd);
        String[] args = {"-scannerID", "{IP}", "-statusInterval", "45", "-useQueues", "-useScannerConf", "-brokerURL", "invalid", "-username", "admin"};
        cmdParser.parse(args);

        ArrayList<String> tmp = new ArrayList<>();
        for(String key : scannerConf.stringPropertyNames()) {
            tmp.add("-"+key);
            String value = scannerConf.getProperty(key);
            tmp.add(value);
        }
        String[] newArgs = new String[tmp.size()];
        tmp.toArray(newArgs);
        cmdParser = new JCommander(cmd);
        cmdParser.parse(newArgs);

        Assert.assertEquals("scannerID", "{IP}", cmd.scannerID);
        Assert.assertEquals("heartbeatUUID", "DAF246CEF20111E4B116123B93F75CBA", cmd.heartbeatUUID);
        Assert.assertEquals("brokerURL", "192.168.1.107:5672", cmd.brokerURL);
        Assert.assertEquals("username", "demo-user", cmd.username);
        Assert.assertEquals("password", "2015-summit-user", cmd.password);
        Assert.assertEquals("beaconMapping", "301=Scott,300=Tony,303=Anthony", cmd.beaconMapping);
        Assert.assertEquals("useQueues", true, cmd.useQueues);

    }
}
