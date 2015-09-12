package lcd;

import java.util.Date;
import java.util.Properties;

import org.jboss.rhiot.beacon.bluez.BeaconInfo;
import org.jboss.rhiot.beacon.common.StatusInformation;
import org.jboss.rhiot.beacon.common.StatusProperties;
import org.jboss.rhiot.beacon.lcd.ConsoleMockDisplay;
import org.jboss.rhiot.beacon.lcd.MiniLcdPCD8544;
import org.jboss.rhiot.beacon.scannerjni.HealthStatus;
import org.junit.Test;

/**
 * Created by starksm on 9/12/15.
 */
public class TestLCD {
    @Test
    public void testMiniLcdPCD8544() throws InterruptedException {
        MiniLcdPCD8544 lcd = new MiniLcdPCD8544();
        lcd.init();
        lcd.clear();
        lcd.displayText("Testing, 1 2 3", 0, 0);
        Date now = new Date();
        String text = String.format("%tT", now);
        lcd.displayText(text, 0, 1);
        System.out.printf("Display should have testing line and time..., sleeping 10 secs\n");
        Thread.sleep(10000);
        System.out.printf("Done\n");
    }
    @Test
    public void testStatusDisplay() {
        ConsoleMockDisplay lcd = new ConsoleMockDisplay();
        lcd.init();
        lcd.clear();
        BeaconInfo info = new BeaconInfo("testStatusDisplay", true, 1, 2, System.currentTimeMillis());
        StatusInformation status = new StatusInformation();
        status.setScannerID("testStatusDisplay");
        status.addEvent(info, true);
        Properties props = new Properties();
        HealthStatus.SystemInfo systemInfo = HealthStatus.getSystemInfo();
        long uptimeDiff = systemInfo.getUptime();
        long days = uptimeDiff / (24*3600*1000);
        long hours = (uptimeDiff - days * 24*3600*1000) / (3600*1000);
        long minutes = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000) / (60*1000);
        long seconds = (uptimeDiff - days * 24*3600*1000 - hours*3600*1000 - minutes*60*1000) / 1000;
        String uptimeShort = String.format("UP D:%d H:%d M:%d S:%d", days, hours, minutes, seconds);
        props.setProperty(StatusProperties.UptimeShort.name(), uptimeShort);
        props.setProperty(StatusProperties.LoadAverage.name(), systemInfo.getLoadAverages());
        status.setLastStatus(props);
        lcd.displayStatus(status);
    }
}
