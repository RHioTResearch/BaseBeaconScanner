package org.jboss.rhiot.beacon.lcd;

/**
 * Created by starksm on 9/11/15.
 */
public abstract class AbstractLcdDisplay {

    protected AbstractLcdDisplay() { }

    /**
     * Initialize the display to its default size
     * @return
     */
    public abstract int init();
    /**
     * Initialize the display to the given size.
     */
    public abstract int init(int rows, int cols);
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
