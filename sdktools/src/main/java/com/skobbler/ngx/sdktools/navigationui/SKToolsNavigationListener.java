package com.skobbler.ngx.sdktools.navigationui;

/**
 * Listener for navigation ui callbacks related to route / navigation.
 */
public interface SKToolsNavigationListener {

    /**
     * Called when  navigation was started.
     */
    void onNavigationStarted();

    /**
     * Called when navigation has ended.
     */
    void onNavigationEnded();

    /**
     * Called when the route calculation was started.
     */
    void onRouteCalculationStarted();

    /**
     * Called when the route calculation has ended.
     */
    void onRouteCalculationCompleted();

    /**
     * Called when the route calculation was canceled.
     */
    void onRouteCalculationCanceled();

}
