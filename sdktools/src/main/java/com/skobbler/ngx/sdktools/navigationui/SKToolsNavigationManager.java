package com.skobbler.ngx.sdktools.navigationui;

import android.app.Activity;
import com.skobbler.ngx.map.SKMapViewHolder;


public class SKToolsNavigationManager {

    public SKToolsNavigationManager(Activity activity, int rootId) {
        SKToolsLogicManager.getInstance().setActivity(activity, rootId);
    }

    /**
     * Starts a route calculation.
     * @param configuration
     * @param mapHolder
     */
    public void launchRouteCalculation(SKToolsNavigationConfiguration configuration, SKMapViewHolder mapHolder) {
        SKToolsLogicManager.getInstance().calculateRoute(configuration, mapHolder);
    }

    /**
     * Removes the screen with the route calculation.
     */
    public void removeRouteCalculationScreen(){
        SKToolsLogicManager.getInstance().removeRouteCalculationScreen();
    }

    /**
     * Starts the navigation
     * @param configuration
     * @param mapHolder
     */
    public void startNavigation(SKToolsNavigationConfiguration configuration, SKMapViewHolder mapHolder) {
        SKToolsLogicManager.getInstance().startNavigation(configuration, mapHolder, false);
    }

    /**
     * Stops the navigation.
     */
    public void stopNavigation() {
        SKToolsLogicManager.getInstance().stopNavigation();
    }

    /**
     * Starts free drive.
     * @param configuration
     * @param mapHolder
     */
    public void startFreeDriveWithConfiguration(SKToolsNavigationConfiguration configuration,
                                                SKMapViewHolder mapHolder) {
        SKToolsLogicManager.getInstance().startNavigation(configuration, mapHolder, true);
    }

    /**
     * Method that should be called when the orientation of the activity has changed.
     */
    public void notifyOrientationChanged() {
        SKToolsLogicManager.getInstance().notifyOrientationChanged();
    }

    /**
     * Sets the listener
     * @param navigationListener
     */
    public void setNavigationListener(SKToolsNavigationListener navigationListener) {
        SKToolsLogicManager.getInstance().setNavigationListener(navigationListener);
    }
}
