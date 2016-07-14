package com.skobbler.ngx.sdktools.navigationui.autonight;

import java.util.Calendar;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.skobbler.ngx.SKCoordinate;

/**
 * Class that handles auto night related operations.
 */
public class SKToolsAutoNightManager {

    private static final String TAG = "SKToolsAutoNightManager";

    /**
     * Singleton instance for current class
     */
    private static SKToolsAutoNightManager instance;

    /**
     * the alarm manager used to set the alarm manager for the hourly notification, when autonight is on
     */
    private AlarmManager hourlyAlarmManager;

    /**
     * the pending alarm intent for the hourly alarm manager
     */
    private PendingIntent pendingHourlyAlarmIntent;

    /**
     * the alarm manager used to set the alarm for the calculate sunrise / sunset hours
     */
    private AlarmManager alarmManagerForAutoNightForCalculatedSunriseSunsetHours;

    /**
     * the pending alarm intent for the alarm manager used to recalculate the sunrise / sunset hours
     */
    private PendingIntent pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours;


    public static SKToolsAutoNightManager getInstance() {
        if (instance == null) {
            instance = new SKToolsAutoNightManager();
        }
        return instance;
    }

    /**
     * Sets the alarm and starts to listen for the times when an hour has passed, in the case when autonight is on.
     * @param context
     */
    public void setAlarmForHourlyNotification(Context context) {
        if (hourlyAlarmManager == null) {
            //if it already an existing alarm for hourly notification, cancel it
            cancelAlarmForForHourlyNotification();
            hourlyAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, SKToolsCalculateSunriseSunsetTimeAutoReceiver.class);
            pendingHourlyAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            hourlyAlarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
                    SKToolsSunriseSunsetCalculator.NR_OF_MILLISECONDS_IN_A_HOUR,
                    pendingHourlyAlarmIntent);

        }
    }

    /**
     * Sets the alarm and starts to listen for the times when an hour has passed, in the case when autonight is on.
     * @param context
     * @param startNow start now or after an hour
     */
    public void setAlarmForHourlyNotificationAfterKitKat(Context context, boolean startNow) {
        if (hourlyAlarmManager == null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //if it already an existing alarm for hourly notification, cancel it
            cancelAlarmForForHourlyNotification();
            hourlyAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, SKToolsCalculateSunriseSunsetTimeAutoReceiver.class);
            pendingHourlyAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            long timeToStart = System.currentTimeMillis();
            if (!startNow) {
                timeToStart += SKToolsSunriseSunsetCalculator.NR_OF_MILLISECONDS_IN_A_HOUR;
            }
            hourlyAlarmManager.setExact(AlarmManager.RTC, timeToStart,
                    pendingHourlyAlarmIntent);
        }
    }

    /**
     * Cancels the alarm for hourly notification.
     */

    public void cancelAlarmForForHourlyNotification() {
        if (hourlyAlarmManager != null && pendingHourlyAlarmIntent != null) {
            hourlyAlarmManager.cancel(pendingHourlyAlarmIntent);
            hourlyAlarmManager = null;
        }
    }

    /**
     * Sets auto day / night alarm according to user position
     * if the user position is null is set the alarm with fixed hours (8AM, 8PM),
     * otherwise is set the alarm for calculation of sunrise / sunset hours.
     * @param currentActivity
     */
    public void setAutoNightAlarmAccordingToUserPosition(SKCoordinate coordinate,
                                                         Activity currentActivity) {
        SKToolsSunriseSunsetCalculator.calculateSunriseSunsetHours(coordinate, SKToolsSunriseSunsetCalculator.OFFICIAL);
        setAlarmForDayNightModeWithSunriseSunset(currentActivity);
    }

    /**
     * Sets the alarm for sunrise / sunset and starts to listen for the times of changing the map
     * style (day/night)
     */
    public void setAlarmForDayNightModeWithSunriseSunset(Context context) {
        Log.d(TAG, "setAlarmForDayNightModeWithSunriseSunset");
        // if there is already an existing alarm then cancel it before starting a new one
        cancelAlarmForDayNightModeWithSunriseSunset();

        pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours = PendingIntent.getBroadcast(context, 0, new Intent(context, SKToolsChangeMapStyleAutoReceiver.class), 0);
        alarmManagerForAutoNightForCalculatedSunriseSunsetHours = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar mapStyleChangeCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (!SKToolsDateUtils.isDaytime()) {
            int actualHour = SKToolsDateUtils.getHourOfDay();
            int actualMinute = SKToolsDateUtils.getMinuteOfDay();
            int sunriseHour = SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR;
            int sunsetHour = SKToolsDateUtils.AUTO_NIGHT_SUNSET_HOUR;
            int sunriseMinute = SKToolsDateUtils.AUTO_NIGHT_SUNRISE_MINUTE;
            int sunsetMinute = SKToolsDateUtils.AUTO_NIGHT_SUNSET_MINUTE;
            String actual = actualHour + ":" + actualMinute;
            String sunrise = sunriseHour + ":" + sunriseMinute;
            String sunset = sunsetHour + ":" + sunsetMinute;
            DateTimeFormatter df = DateTimeFormat.forPattern("HH:mm");
            DateTime ac;
            DateTime limSunrise;
            DateTime limSunset;
            ac = df.parseLocalTime(actual).toDateTimeToday();
            limSunrise = df.parseLocalTime(sunrise).toDateTimeToday();
            limSunset = df.parseLocalTime(sunset).toDateTimeToday();

            if (limSunrise.isBefore(limSunset)) {
                if (ac.isBefore(limSunrise)) {
                    // set the hour for starting the day style
                    mapStyleChangeCalendar.set(Calendar.HOUR_OF_DAY, SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR);
                    mapStyleChangeCalendar.set(Calendar.MINUTE, SKToolsDateUtils.AUTO_NIGHT_SUNRISE_MINUTE);
                    mapStyleChangeCalendar.set(Calendar.SECOND, 0);
                    // set the alarm
                    alarmManagerForAutoNightForCalculatedSunriseSunsetHours.set(AlarmManager.RTC, mapStyleChangeCalendar.getTimeInMillis(),
                            pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours);
                }
            } else if (limSunset.isBefore(limSunrise)) {
                // set the hour for starting the day style
                mapStyleChangeCalendar.set(Calendar.HOUR_OF_DAY, SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR);
                mapStyleChangeCalendar.set(Calendar.MINUTE, SKToolsDateUtils.AUTO_NIGHT_SUNRISE_MINUTE);
                mapStyleChangeCalendar.set(Calendar.SECOND, 0);
                // set the alarm
                alarmManagerForAutoNightForCalculatedSunriseSunsetHours.set(AlarmManager.RTC, mapStyleChangeCalendar.getTimeInMillis(),
                        pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours);

            }
        } else {
            // set the hour for starting the night style
            mapStyleChangeCalendar.set(Calendar.HOUR_OF_DAY, SKToolsDateUtils.AUTO_NIGHT_SUNSET_HOUR);
            mapStyleChangeCalendar.set(Calendar.MINUTE, SKToolsDateUtils.AUTO_NIGHT_SUNSET_MINUTE);
            mapStyleChangeCalendar.set(Calendar.SECOND, 0);
            // set the alarm
            alarmManagerForAutoNightForCalculatedSunriseSunsetHours.set(AlarmManager.RTC, mapStyleChangeCalendar.getTimeInMillis(),
                    pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours);

        }
    }

    /**
     * Cancels the alarm for day / night mode with sunrise / sunset calculation.
     */
    public void cancelAlarmForDayNightModeWithSunriseSunset() {
        if (alarmManagerForAutoNightForCalculatedSunriseSunsetHours != null &&
                pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours != null) {
            alarmManagerForAutoNightForCalculatedSunriseSunsetHours.cancel
                    (pendingAlarmIntentForAutoNightForCalculatedSunriseSunsetHours);
        }
    }

    /**
     * Calculates sunrise and sunset hours
     */
    public void calculateSunriseSunsetHours(SKCoordinate coordinate) {
        if (SKToolsDateUtils.AUTO_NIGHT_SUNRISE_HOUR == 0 && SKToolsDateUtils.AUTO_NIGHT_SUNSET_HOUR == 0) {
            SKToolsSunriseSunsetCalculator.calculateSunriseSunsetHours(coordinate,
                    SKToolsSunriseSunsetCalculator.OFFICIAL);
        }
    }

    /**
     * Returns true if it is day time, false otherwise.
     * @return
     */
    public static boolean isDaytime() {
        return SKToolsDateUtils.isDaytime();
    }
}
