package lcd;

import java.util.Date;

import org.jboss.rhiot.beacon.lcd.MiniLcdPCD8544;
import org.junit.Test;

/**
 * Created by starksm on 9/12/15.
 */
public class TestLCD {
    @Test
    public void testMiniLcdPCD8544() {
        MiniLcdPCD8544 lcd = new MiniLcdPCD8544();
        lcd.init();
        lcd.displayText("Testing, 1 2 3", 0, 0);
        Date now = new Date();
        String text = String.format("%tT", now);
        lcd.displayText(text, 0, 1);
    }
}
