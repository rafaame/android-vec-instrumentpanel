package com.skobbler.ngx.sdktools.navigationui;


import android.app.Activity;
import android.content.res.Configuration;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.sdktools.navigationui.autonight.SKToolsAutoNightManager;


/**
 * Singleton class that provides various methods for changing the state of the
 * map.
 */
class SKToolsMapOperationsManager {

    /**
     * default value to set full screen mode on different devices
     */
    private static final double FULL_SCREEN_MINIMAL_SCREENSIZE = 3.85;

    /**
     * Day style
     */
    public static final byte DAY_STYLE = 0;

    /**
     * Night style
     */
    public static final byte NIGHT_STYLE = 1;

    /**
     * Other style
     */
    public static final byte OTHER_STYLE = 2;

    /**
     * Ids for annotations
     */
    public static final byte GREEN_PIN_ICON_ID = 0;

    public static final byte RED_PIN_ICON_ID = 1;

    public static final byte GREY_PIN_ICON_ID = 3;

    /**
     * Singleton instance of this class
     */
    private static SKToolsMapOperationsManager instance;

    /**
     * the map surface view
     */
    private SKMapSurfaceView mapView;

    /**
     * Last zoom before going in panning mode / overviewmode
     */
    private float zoomBeforeSwitch;

    /**
     * Gets the {@link SKToolsMapOperationsManager} object
     * @return
     */
    public static SKToolsMapOperationsManager getInstance() {
        if (instance == null) {
            instance = new SKToolsMapOperationsManager();
        }
        return instance;
    }

    /**
     * Sets the map view, necessary for handling operations on it.
     * @param mapView
     */
    public void setMapView(SKMapSurfaceView mapView) {
        this.mapView = mapView;
    }

    /**
     * draw the grey pin
     * @param longitude
     * @param latitude
     */
    public void drawGreyPinOnMap(double longitude, double latitude) {
        createAnnotation(GREY_PIN_ICON_ID, SKAnnotation.SK_ANNOTATION_TYPE_PURPLE, longitude, latitude,
                SKAnimationSettings.ANIMATION_PIN_DROP);
    }

    /**
     * Draws the starting point.
     * @param longitude
     * @param latitude
     */
    public void drawStartPoint(double longitude, double latitude) {
        createAnnotation(GREEN_PIN_ICON_ID, SKAnnotation.SK_ANNOTATION_TYPE_GREEN, longitude, latitude,
                SKAnimationSettings.ANIMATION_PIN_DROP);
    }

    /**
     * Draws the destination point.
     * @param longitude
     * @param latitude
     */
    public void drawDestinationPoint(double longitude, double latitude) {
        createAnnotation(RED_PIN_ICON_ID, SKAnnotation.SK_ANNOTATION_TYPE_RED, longitude, latitude,
                SKAnimationSettings.ANIMATION_PIN_DROP);
    }

    /**
     * Deletes the destination point.
     */
    public void deleteDestinationPoint() {
        mapView.deleteAnnotation(RED_PIN_ICON_ID);
    }

    /**
     * Draws the destiunation flag.
     * @param longitude
     * @param latitude
     */
    public void drawDestinationNavigationFlag(double longitude, double latitude) {
        createAnnotation(RED_PIN_ICON_ID, SKAnnotation.SK_ANNOTATION_TYPE_DESTINATION_FLAG, longitude, latitude,
                SKAnimationSettings.ANIMATION_NONE);
    }

    /**
     * Creates an annotation with a specific id, location and type.
     * @param id
     * @param type
     * @param longitude
     * @param latitude
     * @param annotationAnimationType
     */
    private void createAnnotation(int id, int type, double longitude, double latitude,
                                  SKAnimationSettings annotationAnimationType) {
        SKAnnotation annotation = new SKAnnotation(id);
        annotation.setAnnotationType(type);
        annotation.setLocation(new SKCoordinate(longitude, latitude));
        mapView.addAnnotation(annotation, annotationAnimationType);
    }

    /**
     * Sets map in overview mode.
     */
    public void switchToOverViewMode(Activity currentActivity, SKToolsNavigationConfiguration configuration) {
        zoomBeforeSwitch = mapView.getZoomLevel();
        zoomToRoute(currentActivity);
        SKMapSettings mapSettings = mapView.getMapSettings();
        mapSettings.setMapZoomingEnabled(true);
        mapSettings.setMapRotationEnabled(false);
        mapSettings.setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE_WITH_HEADING);
        mapSettings.setMapDisplayMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
        mapView.rotateTheMapToNorth();
    }

    /**
     * Sets map in panning mode.
     */
    public void startPanningMode() {

        zoomBeforeSwitch = mapView.getZoomLevel();
        SKMapSettings mapSettings = mapView.getMapSettings();
        mapSettings.setInertiaPanningEnabled(true);
        mapSettings.setMapZoomingEnabled(true);
        mapSettings.setMapRotationEnabled(true);
        mapView.getMapSettings().setCompassPosition(new SKScreenPoint(5, 5));
        mapView.getMapSettings().setCompassShown(true);
        mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE_WITH_HEADING);
        mapView.getMapSettings().setMapDisplayMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
    }


    /**
     * Sets the map in navigation mode
     */
    public void setMapInNavigationMode() {
        mapView.setZoom(zoomBeforeSwitch);
        mapView.getMapSettings().setMapZoomingEnabled(false);

        mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NAVIGATION);

        final SKPosition naviPosition = mapView.getCurrentGPSPosition(true);
        if (naviPosition != null) {
            mapView.rotateMapWithAngle((float) naviPosition.getHeading());
        }
    }

    /**
     * Changes the map style from day -> night or night-> day
     */
    public void switchDayNightStyle(SKToolsNavigationConfiguration configuration, int mapStyle) {
        int fastSwitchStyleIndex;
        if (mapStyle == DAY_STYLE) {
            fastSwitchStyleIndex = 0;
        } else {
            fastSwitchStyleIndex = 1;
        }
        mapView.getMapSettings().setMapStyle(
                new SKMapViewStyle(SKToolsUtils
                        .getMapStyleFilesFolderPath(configuration, mapStyle),
                        SKToolsUtils.getStyleFileName(mapStyle)));

        mapView.setFastSwitchStyle(fastSwitchStyleIndex);
    }


    /**
     * Changes the map display from 3d-> 2d and vice versa
     */
    public void switchMapDisplayMode(SKMapSettings.SKMapDisplayMode displayMode) {
        mapView.getMapSettings().setMapDisplayMode(displayMode);
        mapView.getMapSettings().setFollowerMode(SKToolsNavigationUIManager.getInstance().currentFollowerMode);
    }

    /**
     * Gets current map view styles: day/night/other.
     * @return
     */
    public int getCurrentMapStyle() {
        int mapStyle;
        SKMapViewStyle currentMapStyle = mapView.getMapSettings().getMapStyle();
        String dayStyleFileName = SKToolsUtils.getStyleFileName(SKToolsMapOperationsManager.DAY_STYLE);
        String nightStyleFileName = SKToolsUtils.getStyleFileName(SKToolsMapOperationsManager.NIGHT_STYLE);
        if (currentMapStyle.getStyleFileName().equals(dayStyleFileName)) {
            mapStyle = SKToolsMapOperationsManager.DAY_STYLE;
        } else if (currentMapStyle.getStyleFileName().equals(nightStyleFileName)) {
            mapStyle = SKToolsMapOperationsManager.NIGHT_STYLE;
        } else {
            mapStyle = SKToolsMapOperationsManager.OTHER_STYLE;
        }
        return mapStyle;
    }

    /**
     * Gets the map style before starting drive mode depending on autonight settings
     */
    public int getMapStyleBeforeStartDriveMode(boolean autoNightIsOn) {
        int currentMapStyle = getCurrentMapStyle();

        if (autoNightIsOn) {
            int correctMapStyleWhenStartDriveMode = getCorrectMapStyleForDriveModeWhenAutoNightIsOn(autoNightIsOn);
            return correctMapStyleWhenStartDriveMode;
        } else {
            return currentMapStyle;
        }
    }

    /**
     * Gets the correct map style (day/night) when auto night is on.
     * @param autoNightIsOn
     * @return
     */
    private int getCorrectMapStyleForDriveModeWhenAutoNightIsOn(boolean autoNightIsOn) {
        if (autoNightIsOn) {
            if (SKToolsLogicManager.lastUserPosition != null) {
                SKToolsAutoNightManager.getInstance().calculateSunriseSunsetHours(SKToolsLogicManager.lastUserPosition.getCoordinate());

                if (SKToolsAutoNightManager.isDaytime()) {
                    return DAY_STYLE;
                } else {
                    return NIGHT_STYLE;
                }
            }
        }
        return DAY_STYLE;
    }

    /**
     * Zooms to route.
     */
    public void zoomToRoute(Activity currentActivity) {
        int offsetPixelsTop = 100;
        if ((currentActivity.getResources().getConfiguration().screenLayout & Configuration
                .SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE
                || (currentActivity.getResources().getConfiguration().screenLayout & Configuration
                .SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            // large and xlarge
            SKRouteManager.getInstance().zoomToRoute(1.3f, 1.5f,
                    offsetPixelsTop, 10, 5, 5);
        } else if (SKToolsUtils.getDisplaySizeInches(currentActivity) < FULL_SCREEN_MINIMAL_SCREENSIZE) {
            // small
            SKRouteManager.getInstance().zoomToRoute(1.3f, 2.5f,
                    offsetPixelsTop, 10, 5, 5);
        } else {
            if (currentActivity.getResources().getConfiguration().screenLayout == Configuration.ORIENTATION_PORTRAIT) {
                SKRouteManager.getInstance().zoomToRoute(1.3f, 2.2f,
                        offsetPixelsTop, 10, 5, 5);
            } else {
                SKRouteManager.getInstance().zoomToRoute(1.3f, 2.2f,
                        0, 10, 5, 5);
            }
        }
    }


}
