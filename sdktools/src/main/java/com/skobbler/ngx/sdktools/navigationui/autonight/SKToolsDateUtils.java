package com.skobbler.ngx.sdktools.navigationui.autonight;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Class used to manipulate date and time values
 */
final class SKToolsDateUtils {

    /**
     * the sunset hour (in 24h format) used for auto day / night mode option
     */
    protected static int AUTO_NIGHT_SUNSET_HOUR;

    /**
     * the sunrise minute (in 24h format) used for auto day / night mode option
     */
    protected static int AUTO_NIGHT_SUNRISE_MINUTE;

    /**
     * the sunset minute (in 24h format) used for auto day / night mode option
     */
    protected static int AUTO_NIGHT_SUNSET_MINUTE;

    /**
     * the sunrise hour (in 24h format) used for auto day / night mode option
     */
    protected static int AUTO_NIGHT_SUNRISE_HOUR;

    private SKToolsDateUtils() {}

    /**
     * Returns true if it is day time, false otherwise
     * @return
     */
    public static boolean isDaytime() {
        int actualHour = getHourOfDay();
        int actualMinute = getMinuteOfDay();
        String actual = actualHour + ":" + actualMinute;
        String sunrise = AUTO_NIGHT_SUNRISE_HOUR + ":" + AUTO_NIGHT_SUNRISE_MINUTE;
        String sunset = AUTO_NIGHT_SUNSET_HOUR + ":" + AUTO_NIGHT_SUNSET_MINUTE;
        DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");
        DateTime ac;
        DateTime limSunrise;
        DateTime limSunset;
        ac = df.parseLocalTime(actual).toDateTimeToday();
        limSunrise = df.parseLocalTime(sunrise).toDateTimeToday();
        limSunset = df.parseLocalTime(sunset).toDateTimeToday();

        boolean equalsSunrise = false;
        boolean equalsSunset = false;

        int actualMinutes = actualHour * 60 + actualMinute;
        int sunriseMinutes = AUTO_NIGHT_SUNRISE_HOUR * 60 + AUTO_NIGHT_SUNRISE_MINUTE;
        int sunsetMinutes = AUTO_NIGHT_SUNSET_HOUR * 60 + AUTO_NIGHT_SUNSET_MINUTE;

        if (actualMinutes == sunriseMinutes) {
            equalsSunrise = true;
        } else if (actualMinutes == sunsetMinutes) {
            equalsSunset = true;
        }

        if (limSunrise.isBefore(limSunset)) {
            if (ac.isBefore(limSunrise)) {
                return false;
            } else if ((limSunrise.isBefore(ac) || equalsSunrise) && ac.isBefore(limSunset)) {
                return true;
            } else if (limSunset.isBefore(ac) || equalsSunset) {
                return false;
            }
        } else if (limSunset.isBefore(limSunrise)) {
            if (limSunrise.isBefore(ac) || equalsSunrise) {
                return true;
            } else if ((limSunset.isBefore(ac) || equalsSunset) && ac.isBefore(limSunrise)) {
                return false;
            } else if (ac.isBefore(limSunset)) {
                return true;
            }
        }
        return true;
    }

    /**
     * Returns the current hour of the day as set on the device.
     * @return
     */
    public static int getHourOfDay() {
        SimpleDateFormat format = new SimpleDateFormat("H");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return Byte.parseByte(format.format(new Date()));
    }

    /**
     * Returns the current hour of the day as set on the device.
     * @return
     */
    public static int getMinuteOfDay() {
        SimpleDateFormat format = new SimpleDateFormat("m");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return Byte.parseByte(format.format(new Date()));
    }

}
