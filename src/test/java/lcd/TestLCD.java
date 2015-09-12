package lcd;

import java.util.Date;

import org.jboss.rhiot.beacon.lcd.MiniLcdPCD8544;
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
}
