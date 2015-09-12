package org.jboss.rhiot.beacon.lcd;

/**
 * A  84 * 48, this Mini LCD screen that has an expander that fits directly over the first 26 gpio header pins
 * of the raspberrypi.
 *
 * http://www.sunfounder.com/index.php?c=show&id=66&model=PCD8544%20Mini%20LCD
 * https://www.adrive.com/public/T6ahCT/PCD8544.zip
 */
public class MiniLcdPCD8544 extends AbstractLcdView {

    /**
     * Initialize the display to 6 rows, 14 cols
     * @return
     */
    public int init() {
        return init(6, 14);
    }
    /**
     * Initialize the display to the given size
     */
    public int init(int rows, int cols) {
        System.loadLibrary("lcdDisplay");
        return doInit(rows, cols);
    }

    /**
     * Erase the display
     */
    public void clear() {
        doClear();
    }

    /**
     * Display a text string starting at the given position. This will wrap around if the string is greater
     * than the number of columns on the display.
     */
    public void displayText(String text, int col, int row) {
        doText(text, col, row);
    }

    native private int doInit(int rows, int cols);
    native private int doClear();
    native private int doText(String text, int col, int row);
}
