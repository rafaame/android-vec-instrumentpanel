package com.skobbler.ngx.sdktools.navigationui;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;

import com.skobbler.ngx.R;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.search.SKSearchResultParent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that handles specific operations for the Navigation UI.
 */
final class SKToolsUtils {

    /**
     * the number of km/h in 1 m/s
     */
    private static final double SPEED_IN_KILOMETRES = 3.6;

    /**
     * number of mi/h in 1 m/s
     */
    private static final double SPEED_IN_MILES = 2.2369;

    /**
     * the number of meters in a km
     */
    private static final int METERS_IN_KM = 1000;

    /**
     * the number of meters in a mile
     */
    private static final double METERS_IN_MILE = 1609.34;

    /**
     * converter from meters to feet
     */
    private static final double METERS_TO_FEET = 3.2808399;

    /**
     * converter from meters to yards
     */
    private static final double METERS_TO_YARDS = 1.0936133;

    /**
     * the number of yards in a mile
     */
    private static final int YARDS_IN_MILE = 1760;

    /**
     * the number of feet in a yard
     */
    private static final int FEET_IN_YARD = 3;

    /**
     * the number of feet in a mile
     */
    private static final int FEET_IN_MILE = 5280;

    /**
     * the limit of feet where the distance should be converted into miles
     */
    private static final int LIMIT_TO_MILES = 1500;

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private SKToolsUtils() { }

    /**
     * Get the configuration json file name according to the mapStyle parameter.
     * @param mapStyle . Possible values are:
     * <p/>
     * {@link SKToolsMapOperationsManager#DAY_STYLE}
     * <p/>
     * {@link SKToolsMapOperationsManager#NIGHT_STYLE}
     * @return the name of the style file. ex: "daystyle.json"
     */
    public static String getStyleFileName(int mapStyle) {
        switch (mapStyle) {
            case SKToolsMapOperationsManager.DAY_STYLE:
                return "daystyle.json";
            case SKToolsMapOperationsManager.NIGHT_STYLE:
                return "nightstyle.json";
        }
        return null;
    }

    /**
     * Gets the path for the style folder according to the mapStyle parameter.
     * @param configuration
     * @param mapStyle Possible values are:
     * <p/>
     * {@link SKToolsMapOperationsManager#DAY_STYLE}
     * <p/>
     * {@link SKToolsMapOperationsManager#NIGHT_STYLE}
     * @return the full path to the style files folder.
     */
    public static String getMapStyleFilesFolderPath(SKToolsNavigationConfiguration configuration, int mapStyle) {
        switch (mapStyle) {
            case SKToolsMapOperationsManager.DAY_STYLE:
                return configuration.getDayStyle().getResourceFolderPath();
            case SKToolsMapOperationsManager.NIGHT_STYLE:
                return configuration.getNightStyle().getResourceFolderPath();
            default:
                break;
        }
        return null;
    }

    /**
     * Method used to convert speed from m/s into km/h or mi/h (according to the
     * distance unit set from Setting option)
     * @param initialSpeed - the speed in m/s
     * @param distanceUnitType
     * @return an int value for speed in km/h or mi/h
     */
    public static int getSpeedByUnit(double initialSpeed, SKMaps.SKDistanceUnitType distanceUnitType) {
        double tempSpeed = initialSpeed;
        if (distanceUnitType == SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS) {
            tempSpeed *= SPEED_IN_KILOMETRES;
        } else {
            tempSpeed *= SPEED_IN_MILES;
        }
        return (int) Math.round(tempSpeed);
    }

    /**
     * @return speed text (km/h or mph) by distance unit
     */
    public static String getSpeedTextByUnit(Activity activity, SKMaps.SKDistanceUnitType distanceUnitType) {
        String currentSpeedUnit;
        if (distanceUnitType == SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS) {
            currentSpeedUnit = activity.getResources().getString(R.string.kmh_label);
        } else {
            currentSpeedUnit = activity.getResources().getString(R.string.mph_label);
        }
        return currentSpeedUnit;
    }


    /**
     * Generate a value suitable for use in setId
     * This value will not collide with ID values generated at build time by aapt for R.id.
     * @return a generated ID value
     */
    public static int generateViewId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            for (; ; ) {
                final int result = sNextGeneratedId.get();
                // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
                int newValue = result + 1;
                if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
                if (sNextGeneratedId.compareAndSet(result, newValue)) {
                    return result;
                }
            }
        } else {
            return View.generateViewId();
        }
    }

    /**
     * Decodes a file given by its path to a Bitmap object
     * @param pathToFile
     * @return the Bitmap object if the decoding was made succesfully and null if any errors appeared during the process
     */
    public static Bitmap decodeFileToBitmap(String pathToFile) {
        Bitmap decodedFile = null;
        try {
            decodedFile = BitmapFactory.decodeFile(pathToFile);
        } catch (OutOfMemoryError ofmerr) {
            return null;
        }
        return decodedFile;
    }

    /**
     * elapsed time in hours/minutes
     * @return elapsed time as string
     */
    public static String formatTime(long elapsedTimeInSeconds) {
        final String format = String.format("%%0%dd", 2);
        final StringBuilder time = new StringBuilder();
        time.append(String.format(format, elapsedTimeInSeconds / 3600)).append(":")
                .append(String.format(format, (elapsedTimeInSeconds % 3600) / 60));
        return time.toString();
    }

    /**
     * converts a distance given in meters to the according distance in yards
     * @param distanceInMeters
     * @return
     */
    private static double distanceInYards(double distanceInMeters) {
        if (distanceInMeters != -1) {
            return distanceInMeters *= METERS_TO_YARDS;
        } else {
            return distanceInMeters;
        }
    }

    /**
     * converts a distance given in meters to the according distance in feet
     * @param distanceInMeters
     * @return
     */
    private static double distanceInFeet(double distanceInMeters) {
        if (distanceInMeters != -1) {
            return distanceInMeters * METERS_TO_YARDS * FEET_IN_YARD;
        } else {
            return distanceInMeters;
        }
    }

    /**
     * Converts (to imperial units if necessary) and formats as string a
     * distance value given in meters.
     * @param distanceValue distance value in meters
     * @param activity activity object used to get the app preferences and the
     * distance unit labels
     * @return
     */
    public static String convertAndFormatDistance(double distanceValue, SKMaps.SKDistanceUnitType distanceUnitType,
                                                  Activity activity) {

        if (distanceUnitType != SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS) {
            // convert meters to feet or yards if needed
            if (distanceUnitType == SKMaps.SKDistanceUnitType.DISTANCE_UNIT_MILES_FEET) {
                distanceValue = (float) distanceInFeet(distanceValue);
            } else {
                distanceValue = (float) distanceInYards(distanceValue);
            }
        }

        String distanceValueText;

        if (distanceUnitType == SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS) {
            if (distanceValue >= METERS_IN_KM) {
                distanceValue /= METERS_IN_KM;
                if (distanceValue >= 10) {
                    // if distance is >= 10 km => display distance without any
                    // decimals
                    distanceValueText =
                            (Math.round(distanceValue) + " " + activity.getResources().getString(R.string.km_label));
                } else {
                    // distance displayed in kilometers
                    distanceValueText =
                            (((float) Math.round(distanceValue * 10) / 10)) + " "
                                    + activity.getResources().getString(R.string.km_label);
                }
            } else {
                // distance displayed in meters
                distanceValueText =
                        ((int) distanceValue) + " " + activity.getResources().getString(R.string.meters_label);
            }
        } else if (distanceUnitType == SKMaps.SKDistanceUnitType.DISTANCE_UNIT_MILES_FEET) {
            // if the distance in feet > 1500 => convert it in miles (FMA-2577)
            if (distanceValue >= LIMIT_TO_MILES) {
                distanceValue /= FEET_IN_MILE;
                if (distanceValue >= 10) {
                    // for routing if the distance is > 10 should be rounded to
                    // be an int; rounded distance displayed in miles
                    distanceValueText =
                            (Math.round(distanceValue) + " " + activity.getResources().getString(R.string.mi_label));

                } else {
                    // distance displayed in miles
                    distanceValueText =
                            (((float) Math.round(distanceValue * 10) / 10)) + " "
                                    + activity.getResources().getString(R.string.mi_label);
                }
            } else {
                // distance displayed in feet
                distanceValueText =
                        ((int) distanceValue) + " " + activity.getResources().getString(R.string.feet_label);
            }
        } else {
            if (distanceValue >= METERS_IN_KM) {
                distanceValue /= YARDS_IN_MILE;
                if (distanceValue >= 10) {
                    distanceValueText =
                            (Math.round(distanceValue) + " " + activity.getResources().getString(R.string.mi_label));
                } else {
                    // distance displayed in miles
                    distanceValueText =
                            (((float) Math.round(distanceValue * 10) / 10)) + " "
                                    + activity.getResources().getString(R.string.mi_label);
                }
            } else {
                // distance displayed in yards
                distanceValueText =
                        ((int) distanceValue) + " " + activity.getResources().getString(R.string.yards_label);
            }
        }

        if ((distanceValueText != null) && distanceValueText.startsWith("0 ")) {
            return "";
        }
        return distanceValueText;

    }

    /**
     * Returns the formatted address/vicinity of this place object (to be
     * displayed in list items).
     * @return
     */
    public static String getFormattedAddress(List<SKSearchResultParent> parents) {
        if (parents != null) {
            final StringBuilder builder = new StringBuilder();
            for (SKSearchResultParent skSearchResultParent : parents) {
                builder.append(skSearchResultParent.getParentName() + " ");
            }

            String formattedAddress = builder.toString().replaceAll("\n(\\s)*$", "");
            return formattedAddress.trim().equals("") ? "" : formattedAddress;
        }
        return "";
    }


    /**
     * converts the distance given in feet/yards/miles/km to the according distance in meters
     * @param distance
     * @param initialUnit: 0 - feet
     * 1 - yards
     * 2 - mile
     * 3 - km
     * @return distance in meters
     */
    public static double distanceInMeters(double distance, int initialUnit) {
        if (distance != -1) {
            switch (initialUnit) {
                case 0:
                    return distance /= METERS_TO_FEET;
                case 1:
                    return distance /= METERS_TO_YARDS;
                case 2:
                    return distance *= METERS_IN_MILE;
                case 3:
                    return distance *= METERS_IN_KM;
            }
        }
        return distance;
    }

    /**
     * Checks if the current device has a GPS module (hardware)
     * @return true if the current device has GPS
     */
    public static boolean hasGpsModule(final Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        for (final String provider : locationManager.getAllProviders()) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Checks if the current device has a  NETWORK module (hardware)
     * @return true if the current device has NETWORK
     */
    public static boolean hasNetworkModule(final Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        for (final String provider : locationManager.getAllProviders()) {
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the display size in inches.
     * @return the value in inches
     */
    public static double getDisplaySizeInches(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        double x = Math.pow((double) dm.widthPixels / (double) dm.densityDpi, 2);
        double y = Math.pow((double) dm.heightPixels / (double) dm.densityDpi, 2);

        return Math.sqrt(x + y);
    }
}
