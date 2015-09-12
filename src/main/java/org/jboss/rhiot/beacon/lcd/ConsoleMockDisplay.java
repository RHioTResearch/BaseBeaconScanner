package org.jboss.rhiot.beacon.lcd;

/**
 * Created by starksm on 9/12/15.
 */
public class ConsoleMockDisplay extends AbstractLcdView {
    @Override
    public int init() {
        return 0;
    }

    @Override
    public int init(int rows, int cols) {
        return 0;
    }

    @Override
    public void displayText(String text, int col, int row) {
        System.out.println(text);
    }
}
