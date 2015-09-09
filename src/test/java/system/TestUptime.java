package system;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test parsing the sytem uptime command output
 */
public class TestUptime {
    /**
     * Try expression for less than an hour;
     * 12:22:35 up 56 min,  5 users,  load average: 0.92, 0.78, 0.71
     * @param uptime
     * @return
     */
    static boolean parseLessThanHour(String uptime) {
        Pattern parse = Pattern.compile("up\\s+((\\d+) min?,).*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _minutes = matcher.group(2);
            String loadAvgs = matcher.group(3);

            long days = 0;
            long hours = 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);

            days = uptimeMS / (24 * 3600 * 1000);
            hours = (uptimeMS - days * 24 * 3600 * 1000) / (3600 * 1000);
            minutes = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000) / (60 * 1000);
            long seconds = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000 - minutes * 60 * 1000) / 1000;
            System.out.printf("Parsed uptime; days=%d,hours=%d,mins=%d,secs=%s;%s\n", days, hours, minutes, seconds, loadAvgs);
        }
        return matched;
    }

    /**
     * Try expression for less than 1 day up;
     * 14:25:50 up  2:59,  5 users,  load average: 0.21, 0.14, 0.14
     * @param uptime
     * @return
     */
    static boolean parseLessThanDay(String uptime) {
        Pattern parse = Pattern.compile("up\\s+(\\d+),\\s+(\\d+):(\\d+),.*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _hours = matcher.group(2);
            String _minutes = matcher.group(3);
            String loadAvgs = matcher.group(4);

            long days = 0;
            long hours = _hours != null ? Integer.parseInt(_hours) : 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);

            days = uptimeMS / (24 * 3600 * 1000);
            hours = (uptimeMS - days * 24 * 3600 * 1000) / (3600 * 1000);
            minutes = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000) / (60 * 1000);
            long seconds = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000 - minutes * 60 * 1000) / 1000;
            System.out.printf("Parsed uptime; days=%d,hours=%d,mins=%d,secs=%s;%s\n", days, hours, minutes, seconds, loadAvgs);
        }
        return matched;
    }

    /**
     * 01:25:06 up 1 day,  7:03,  2 users,  load average: 0.09, 0.16, 0.14
     * @param uptime
     * @return
     */
    static boolean parseMoreThanDay(String uptime) {
        Pattern parse = Pattern.compile("((\\d+) day[s]?,)?\\s+(\\d+):(\\d+),.*(load average[s]?:.*)");
        Matcher matcher = parse.matcher(uptime);
        boolean matched = false;
        if (matcher.find()) {
            matched = true;
            String _days = matcher.group(2);
            String _hours = matcher.group(3);
            String _minutes = matcher.group(4);
            String loadAvgs = matcher.group(5);

            long days = _days != null ? Integer.parseInt(_days) : 0;
            long hours = _hours != null ? Integer.parseInt(_hours) : 0;
            long minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
            long uptimeMS = (minutes * 60 * 1000) + (hours * 3600 * 1000) + (days * 24 * 3600 * 1000);

            days = uptimeMS / (24 * 3600 * 1000);
            hours = (uptimeMS - days * 24 * 3600 * 1000) / (3600 * 1000);
            minutes = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000) / (60 * 1000);
            long seconds = (uptimeMS - days * 24 * 3600 * 1000 - hours * 3600 * 1000 - minutes * 60 * 1000) / 1000;
            System.out.printf("Parsed uptime; days=%d,hours=%d,mins=%d,secs=%s;%s\n", days, hours, minutes, seconds, loadAvgs);
        }
        return matched;
    }

    static void parse(String uptime) {
        if(parseMoreThanDay(uptime))
            return;;
        if(parseLessThanHour(uptime))
            return;
        if(parseLessThanDay(uptime))
            return;
        Assert.fail("Failed to parse uptime string");
    }

    @Test
    public void testParseLessThanHour() {
        String uptime= " 12:22:35 up 56 min,  5 users,  load average: 0.92, 0.78, 0.71";
        parse(uptime);
    }
    @Test
    public void testParseLessThanDay() {
        String uptime= " 14:25:50 up  2:59,  5 users,  load average: 0.21, 0.14, 0.14\n";
        parse(uptime);
    }
    @Test
    public void testParseOneDay() {
        String uptime= "01:25:06 up 1 day,  7:03,  2 users,  load average: 0.09, 0.16, 0.14";
        parse(uptime);

    }
    @Test
    public void testParseManyDays() {
        String uptime = "18:25  up 3 days,  9:01, 12 users, load averages: 1.78 2.10 2.23";
        parse(uptime);

    }
}
