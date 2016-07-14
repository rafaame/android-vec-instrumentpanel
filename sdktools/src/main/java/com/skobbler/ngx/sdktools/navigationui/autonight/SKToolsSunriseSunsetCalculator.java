package com.skobbler.ngx.sdktools.navigationui.autonight;

import java.util.Calendar;
import java.util.TimeZone;
import android.util.Log;
import com.skobbler.ngx.SKCoordinate;

/**
 * This class calculates the sunrise and sunset times according to user location.
 * Is based on Ed Williams algorihtm.
 * http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 */
class SKToolsSunriseSunsetCalculator {

    /**
     * constants for Sun's zenith values for sunrise/sunset
     */
    public static final double OFFICIAL = 90.5;

    public static final double CIVIL = 96.0;

    public static final double NAUTICAL = 102.0;

    public static final double ASTRONOMICAL = 108.0;

    public static final int NR_OF_MILLISECONDS_IN_A_HOUR = 3600000;

    private static final String TAG = "SunriseSunsetCalculator";

    public static void calculateSunriseSunsetHours(SKCoordinate coordinate, double zenith) {
        calculateTime(coordinate.getLatitude(), coordinate.getLongitude(), zenith, true);
        calculateTime(coordinate.getLatitude(), coordinate.getLongitude(), zenith, false);
    }

    private static void calculateTime(double latitude, double longitude, double zenith, boolean calculateSunrise) {

        double approximateTime;
        double meanAnomaly;
        double localHour;
        double universalTime;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // first calculate the day of the year
        double N1 = Math.floor(275.0 * currentMonth / 9.0);
        double N2 = Math.floor((currentMonth + 9.0) / 12.0);
        double N3 = (1 + Math.floor((currentYear - 4.0 * Math.floor(currentYear / 4.0) + 2.0) / 3.0));
        double dayOfYear = N1 - (N2 * N3) + currentDay - 30.0;

        // convert the longitude to hour value and calculate an approximate time
        double longHour = longitude / 15.0;
        if (calculateSunrise) {
            approximateTime = dayOfYear + ((6.0 - longHour) / 24.0);
        } else {
            approximateTime = dayOfYear + ((18.0 - longHour) / 24.0);
        }

        // calculate the Sun's mean anomaly

        meanAnomaly = (0.9856 * approximateTime) - 3.289;

        // calculate the Sun's true longitude

        double sunLongitude = meanAnomaly + (1.916 * Math.sin(Math.toRadians(meanAnomaly))) + (0.020 * Math.sin(2 * Math.toRadians(meanAnomaly))) + 282.634;

        sunLongitude = getNormalizedValue(sunLongitude, 360);

        // calculate the Sun's right ascension

        double sunRightAscension = Math.toDegrees(Math.atan(0.91764 * Math
                .tan(Math.toRadians(sunLongitude))));
        sunRightAscension = getNormalizedValue(sunRightAscension, 360);
        // right ascension value needs to be in the same quadrant as L

        double longitudeQuadrant = (Math.floor(sunLongitude / 90.0)) * 90.0;
        double rightAscensionQuadrant = (Math.floor(sunRightAscension / 90.0)) * 90.0;
        sunRightAscension = sunRightAscension + (longitudeQuadrant - rightAscensionQuadrant);

        // right ascension value needs to be converted into hours

        sunRightAscension = sunRightAscension / 15.0;

        // calculate the Sun's declination

        double sunSinDeclination = 0.39782 * Math.sin(Math.toRadians(sunLongitude));
        double sunCosDeclination = Math.cos(Math.asin(sunSinDeclination));

        // calculate the Sun's local hour angle

        double cosLocalHour = (Math.cos(Math.toRadians(zenith)) - (sunSinDeclination * Math.sin(Math.toRadians(latitude)))) / (sunCosDeclination * Math.cos(Math.toRadians
                (latitude)));

        if (cosLocalHour > 1) {
            return;

        }
        if (cosLocalHour < -1) {
            return;
        }

        // finish calculating localHour and convert into hours

        if (calculateSunrise) {
            localHour = 360.0 - Math.toDegrees(Math.acos(cosLocalHour));
        } else {
            localHour = Math.toDegrees(Math.acos(cosLocalHour));
        }
        localHour = localHour / 15.0;

        // calculate local mean time of rising/setting

        double localMeanTime = localHour + sunRightAscension - (0.06571 * approximateTime) - 6.622;

        // adjust back to UTC

        universalTime = localMeanTime - longHour;
        universalTime = getNormalizedValue(universalTime, 24);

        int hour = (int) Math.floor(universalTime);
        int hourSeconds = (int) (3600 * (universalTime - hour));
        int minute = hourSeconds / 60;

        if (calculateSunrise) {
            SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR = hour;
            SKToolsDateUtils.AUTO_NIGHT_SUNRISE_MINUTE = minute;
            Log.d(TAG, "Sunrise : " + SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR + ":" + SKToolsDateUtils.AUTO_NIGHT_SUNRISE_MINUTE);
        } else {
            SKToolsDateUtils.AUTO_NIGHT_SUNSET_HOUR = hour;
            SKToolsDateUtils.AUTO_NIGHT_SUNSET_MINUTE = minute;
            Log.d(TAG, "Sunset : " + SKToolsDateUtils.AUTO_NIGHT_SUNSET_HOUR + ":" + SKToolsDateUtils.AUTO_NIGHT_SUNSET_MINUTE);
        }
    }

    /**
     * normalizes a value within a given range
     * @param value
     * @param maxRange
     * @return normalize value
     */
    private static double getNormalizedValue(double value, double maxRange) {
        while (value > maxRange) {
            value -= maxRange;
        }
        while (value < 0) {
            value += maxRange;
        }
        return value;
    }
}