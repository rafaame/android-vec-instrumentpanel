package com.skobbler.ngx.sdktools.navigationui;


import java.util.ArrayList;
import java.util.List;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.routing.SKViaPoint;

/**
 * Class that provides customization options for calculating routes, navigation and free drive.
 */
public class SKToolsNavigationConfiguration {


    /**
     * The start coordinate of the route
     */
    private SKCoordinate startCoordinate;

    /**
     * The destination coordinate of the route
     */
    private SKCoordinate destinationCoordinate;

    /**
     * The via point coordinate list of the route
     */
    private List<SKViaPoint> viaPointCoordinateList;

    /**
     * Desired style to use during the day.
     */
    private SKMapViewStyle dayStyle;

    /**
     * Desired style to use during the night.
     */
    private SKMapViewStyle nightStyle;

    /**
     * The route mode. Default is Simulation.
     */
    private SKRouteSettings.SKRouteMode routeType;

    /**
     * Desired distance format.
     */
    private SKMaps.SKDistanceUnitType distanceUnitType;

    /**
     * speed warning in city in m/s
     */
    private double speedWarningThresholdInCity;

    /**
     * speed warning outside city in m/s.
     */
    private double speedWarningThresholdOutsideCity;

    /**
     * Enables automatic style switching according to time of day. Default is true.
     */
    private boolean automaticDayNight;

    /**
     * Indicates whether to avoid toll roads when calculating the route.
     */
    private boolean tollRoadsAvoided;

    /**
     * Indicates whether to avoid highways when calculating the route.
     */
    private boolean highWaysAvoided;

    /**
     * Indicates whether to avoid ferries when calculating the route
     */
    private boolean ferriesAvoided;

    /**
     * If true, free drive will be automatically started after reaching the destination.
     */
    private boolean continueFreeDriveAfterNavigationEnd;

    /**
     * Desired navigation type.
     */
    private SKNavigationSettings.SKNavigationType navigationType;

    /**
     * The path from log file
     */
    private String freeDriveNavigationFilePath;

    public SKToolsNavigationConfiguration() {
        viaPointCoordinateList = new ArrayList<SKViaPoint>();
        routeType = SKRouteSettings.SKRouteMode.EFFICIENT;
        distanceUnitType = SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS;
        navigationType = SKNavigationSettings.SKNavigationType.REAL;

        automaticDayNight = true;
        speedWarningThresholdInCity = 20.0;
        speedWarningThresholdOutsideCity = 20.0;
        tollRoadsAvoided = false;
        highWaysAvoided = false;
        ferriesAvoided = false;
        continueFreeDriveAfterNavigationEnd = true;
        freeDriveNavigationFilePath = "";
    }

    /**
     * Sets the route mode used for route calculation.
     *
     * @param routeType
     */
    public void setRouteType(SKRouteSettings.SKRouteMode routeType) {
        this.routeType = routeType;
    }

    /**
     * @return the route mode used for route calculation.
     */
    public SKRouteSettings.SKRouteMode getRouteType() {
        return routeType;
    }

    /**
     * @return the start coordinate of the route.
     */
    public SKCoordinate getStartCoordinate() {
        return startCoordinate;
    }

    /**
     * Sets the start coordinate of the route
     *
     * @param startCoordinate
     */
    public void setStartCoordinate(SKCoordinate startCoordinate) {
        this.startCoordinate = startCoordinate;
    }

    /**
     * @return the destination coordinate of the route
     */
    public SKCoordinate getDestinationCoordinate() {
        return destinationCoordinate;
    }

    /**
     * Sets the destination coordinate of the route
     *
     * @param destinationCoordinate
     */
    public void setDestinationCoordinate(SKCoordinate destinationCoordinate) {
        this.destinationCoordinate = destinationCoordinate;
    }

    /**
     * @return the via point coordinate list of the route
     */
    public List<SKViaPoint> getViaPointCoordinateList() {
        return viaPointCoordinateList;
    }

    /**
     * Sets the via point coordinate list of the route
     *
     * @param viaPointCoordinate
     */
    public void setViaPointCoordinateList(List<SKViaPoint> viaPointCoordinate) {
        this.viaPointCoordinateList = viaPointCoordinate;
    }

    /**
     * @return the day style.
     */
    public SKMapViewStyle getDayStyle() {
        return dayStyle;
    }

    /**
     * Sets the day style.
     *
     * @param dayStyle
     */
    public void setDayStyle(SKMapViewStyle dayStyle) {
        this.dayStyle = dayStyle;
    }

    /**
     * @return the night style.
     */
    public SKMapViewStyle getNightStyle() {
        return nightStyle;
    }

    /**
     * Sets the night style.
     *
     * @param nightStyle
     */
    public void setNightStyle(SKMapViewStyle nightStyle) {
        this.nightStyle = nightStyle;
    }

    /**
     * @return the distance unit.
     */
    public SKMaps.SKDistanceUnitType getDistanceUnitType() {
        return distanceUnitType;
    }

    /**
     * Sets the distance unit.
     *
     * @param distanceUnitType
     */
    public void setDistanceUnitType(SKMaps.SKDistanceUnitType distanceUnitType) {
        this.distanceUnitType = distanceUnitType;
    }

    /**
     * @return the threshold for speed warning callback inside cities in m/s.
     */
    public double getSpeedWarningThresholdInCity() {
        return speedWarningThresholdInCity;
    }

    /**
     * Sets the threshold for speed warning callback inside cities in m/s.
     *
     * @param speedWarningThresholdInCity
     */
    public void setSpeedWarningThresholdInCity(double speedWarningThresholdInCity) {
        this.speedWarningThresholdInCity = speedWarningThresholdInCity;
    }

    /**
     * @return the threshold for speed warning callback outside cities in m/s.
     */
    public double getSpeedWarningThresholdOutsideCity() {
        return speedWarningThresholdOutsideCity;
    }

    /**
     * Sets the threshold for speed warning callback outside cities in m/s.
     *
     * @param speedWarningThresholdOutsideCity
     */
    public void setSpeedWarningThresholdOutsideCity(double speedWarningThresholdOutsideCity) {
        this.speedWarningThresholdOutsideCity = speedWarningThresholdOutsideCity;
    }

    /**
     * @return boolean that indicates whether day night algorithm is taken into consideration.
     */
    public boolean isAutomaticDayNight() {
        return automaticDayNight;
    }

    /**
     * Sets a boolean that indicates whether day night algorithm is taken into consideration.
     *
     * @param automaticDayNight
     */
    public void setAutomaticDayNight(boolean automaticDayNight) {
        this.automaticDayNight = automaticDayNight;
    }

    /**
     * @return boolean that indicates whether to continue free drive after real navigation ends.
     */
    public boolean isContinueFreeDriveAfterNavigationEnd() {
        return continueFreeDriveAfterNavigationEnd;
    }

    /**
     * Sets a boolean that indicates whether to continue free drive after real navigation ends.
     *
     * @param continueFreeDriveAfterNavigationEnd
     */
    public void setContinueFreeDriveAfterNavigationEnd(boolean continueFreeDriveAfterNavigationEnd) {
        this.continueFreeDriveAfterNavigationEnd = continueFreeDriveAfterNavigationEnd;
    }

    /**
     * @return the navigation type
     */
    public SKNavigationSettings.SKNavigationType getNavigationType() {
        return navigationType;
    }

    /**
     * Sets the navigation type.
     *
     * @param navigationType
     */
    public void setNavigationType(SKNavigationSettings.SKNavigationType navigationType) {
        this.navigationType = navigationType;
    }

    /**
     * @return Returns a boolean that indicates whether to avoid ferries when
     * calculating the route.
     */
    public boolean isTollRoadsAvoided() {
        return tollRoadsAvoided;
    }

    /**
     * Sets a boolean that indicates whether to avoid ferries when calculating
     * the route.
     *
     * @param tollRoadsAvoided
     */
    public void setTollRoadsAvoided(boolean tollRoadsAvoided) {
        this.tollRoadsAvoided = tollRoadsAvoided;
    }

    /**
     * @return Returns a boolean that indicates whether to avoid highways roads
     * when calculating the route.
     */
    public boolean isHighWaysAvoided() {
        return highWaysAvoided;
    }

    /**
     * Sets a boolean that indicates whether to avoid highways roads when
     * calculating the route.
     *
     * @param highWaysAvoided
     */
    public void setHighWaysAvoided(boolean highWaysAvoided) {
        this.highWaysAvoided = highWaysAvoided;
    }

    /**
     * @return Returns a boolean that indicates whether to avoid ferries when
     * calculating the route.
     */
    public boolean isFerriesAvoided() {
        return ferriesAvoided;
    }

    /**
     * Sets a boolean that indicates whether to avoid ferries when calculating
     * the route.
     *
     * @param ferriesAvoided
     */
    public void setFerriesAvoided(boolean ferriesAvoided) {
        this.ferriesAvoided = ferriesAvoided;
    }

    /**
     * @return the path to the file used to make free drive navigation.
     */
    public String getFreeDriveNavigationFilePath() {
        return freeDriveNavigationFilePath;
    }

    /**
     * Sets the path to the file used to make free drive navigation.
     *
     * @param freeDriveNavigationFilePath
     */
    public void setFreeDriveNavigationFilePath(String freeDriveNavigationFilePath) {
        this.freeDriveNavigationFilePath = freeDriveNavigationFilePath;
    }
}
