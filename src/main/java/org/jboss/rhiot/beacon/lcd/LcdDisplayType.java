package org.jboss.rhiot.beacon.lcd;

/**
 * Types of LCD displays we support
 */
public enum LcdDisplayType {
    // Currently maps to WiringPiLcdDisplay. May need other flavors of the HD44780U in future.
    HD44780U,
    // A Nokia/Phillips 48x84 lcd display with PCD8544 controller
    PCD8544,
    // A console mock display
    CONSOLE,
    INVALID_LCD_TYPE
}
