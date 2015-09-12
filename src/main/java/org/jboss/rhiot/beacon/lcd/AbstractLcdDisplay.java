package org.jboss.rhiot.beacon.lcd;

/**
 * Created by starksm on 9/11/15.
 */
public abstract class AbstractLcdDisplay {
    private int nCols;
    private int lcdHandle;

    protected AbstractLcdDisplay() { }

    /**
     * Initialize the display to the given size.
     */
    public int init() {
        return init(4,20);
    }
    public int init(int rows, int cols) {
        return 0;
    }
    /**
     * Erase the display
     */
    public void clear() {

    }
    /**
     * Display a text string starting at the given position. This will wrap around if the string is greater
     * than the number of columns on the display.
     */
    public abstract void displayText(String text, int col, int row);
    /**
     * Display a time string as HH:MM:SS.ss
     */
    public abstract void displayTime(long timeInMS, int col, int row);
}
