package com.skobbler.ngx.sdktools.navigationui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.skobbler.ngx.R;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteSettings;

/**
 * This class handles the user interface related to the navigation experience.
 */
class SKToolsNavigationUIManager {

    /**
     * Singleton instance for current class
     */
    private static volatile SKToolsNavigationUIManager instance = null;

    /**
     * OSM street types
     */
    private static final int OSM_STREET_TYPE_MOTORWAY = 9;

    private static final int OSM_STREET_TYPE_MOTORWAY_LINK = 10;

    private static final int OSM_STREET_TYPE_PRIMARY = 13;

    private static final int OSM_STREET_TYPE_PRIMARY_LINK = 14;

    private static final int OSM_STREET_TYPE_TRUNK = 24;

    private static final int OSM_STREET_TYPE_TRUNK_LINK = 25;

    /**
     * the list with the country codes for which the top panel has a different
     * color
     */
    private static final String[] signPostsCountryExceptions = {"DE", "AT", "GB", "IE", "CH", "US"};


    private enum NavigationMode {

        SETTINGS,

        ROUTE_INFO,

        ROUTE_OVERVIEW,

        PANNING,

        ROADBLOCK,

        FOLLOWER,

        PRE_NAVIGATION,

        POST_NAVIGATION;

    }

    private enum CompassStates {
        HISTORICAL_POSITIONS, PEDESTRIAN_COMPASS, NORTH_ORIENTED
    }

    /**
     * Compass option selected
     */

    private CompassStates compassStates = CompassStates.HISTORICAL_POSITIONS;

    /**
     * the current navigation mode pedestrian, car, bike
     */

    private NavigationMode currentNavigationMode;
    /**
     * current compass image
     */
    protected ImageView currentCompasImage;

    /**
     * the compass panel
     */
    private ViewGroup compassViewPanel;

    /**
     * the current activity
     */
    private Activity currentActivity;

    /**
     * the settings from navigations
     */
    private ViewGroup settingsPanel;

    /*
    the view for pre navigation
     */
    private ViewGroup preNavigationPanel;

    /*
    the view for pre navigation increase decrease buttons
     */
    private ViewGroup navigationSimulationPanel;

    /**
     * the back button
     */
    private ViewGroup backButtonPanel;

    /**
     * route overview panel
     */
    private ViewGroup routeOverviewPanel;

    /**
     * the view for re routing
     */
    private ViewGroup reRoutingPanel;

    /**
     * arriving navigation distance panel
     */
    private ViewGroup routeDistancePanel;

    /**
     * the  position me button
     */
    private ViewGroup positionMeButtonPanel;

    /**
     * top navigation panel
     */
    private ViewGroup topCurrentNavigationPanel;

    /**
     * top navigation panel
     */
    private ViewGroup topNextNavigationPanel;

    /**
     * the menu from tapping mode
     */
    private ViewGroup menuOptions;

    /**
     * root layout - to this will be added all views
     */
    private ViewGroup rootLayout;

    /**
     * road block screen
     */
    private ViewGroup roadBlockPanel;

    /**
     * current street name panel from free drive mode
     */
    private ViewGroup freeDriveCurrentStreetPanel;

    /**
     * via point panel
     */
    private ViewGroup viaPointPanel;

    /**
     * top navigation panel
     */
    private ViewGroup speedPanel;

    /**
     * the view flipper for estimated and arriving time panels
     */
    private ViewGroup arrivingETATimeGroupPanels;

    /**
     * bottom right panel with estimated time
     */
    private ViewGroup estimatedTimePanel;

    /**
     * bottom right panel with arriving time
     */
    private ViewGroup arrivingTimePanel;

    /**
     * navigation arriving time text
     */
    private TextView arrivingTimeText;

    /**
     * navigation estimated time text
     */
    private TextView estimatedTimeText;

    /**
     * true if the estimated time panel is visible, false if the arriving time
     * panel is visible
     */
    private boolean estimatedTimePanelVisible;

    /**
     * top current navigation distance & street panel
     */
    private LinearLayout topCurrentNavigationDistanceStreetPanel;

    /**
     * the image for the visual advice
     */
    private ImageView nextAdviceImageView;

    /**
     * next advice image distance panel
     */
    private RelativeLayout nextAdviceImageDistancePanel;

    /**
     * next advice street name panel
     */
    private RelativeLayout nextAdviceStreetNamePanel;

    /**
     * next advice street name text
     */
    private TextView nextAdviceStreetNameTextView;

    /**
     * arriving navigation distance text
     */
    private TextView routeDistanceText;

    /**
     * arriving navigation distance text value
     */
    private TextView routeDistanceTextValue;

    /**
     * the alternative routes buttons
     */
    private TextView[] altRoutesButtons;

    /**
     * true, if free drive selected
     */
    private boolean isFreeDrive;

    /**
     * true if the country code for destination is US, false otherwise
     */
    private boolean isUS;

    /**
     * true if the country code is different from US, false otherwise
     */
    private boolean isDefaultSpeedSign;

    /**
     * flag that indicates if there is a speed limit on the current road
     */
    private boolean speedLimitAvailable;

    /**
     * current street name from free drive mode
     */
    private String currentStreetNameFreeDriveString;

    /**
     * true if the background color for top panel is the default one, false
     * otherwise
     */
    private boolean isDefaultTopPanelBackgroundColor = true;

    /**
     * the drawable id for the current advice background
     */
    private int currentAdviceBackgroundDrawableId;

    /**
     * the drawable id for the next advice background
     */
    private int nextAdviceBackgroundDrawableId;

    /**
     * the thread for speed exceeded
     */
    private SpeedExceededThread speedExceededThread;

    /**
     * current street type
     */
    protected int nextStreetType;

    /**
     * next street type
     */
    protected int secondNextStreetType;

    /**
     * the current estimated total distance of the navigation trip
     */
    private long navigationTotalDistance;

    /**
     * current speed limit
     */
    protected double currentSpeedLimit;

    /**
     * true if the speed limit is exceeded, false otherwise
     */
    protected boolean speedLimitExceeded;

    /**
     * false when we start a navigation, becomes true when first advice is
     * received
     */
    protected boolean firstAdviceReceived;

    /**
     * true if next advice is visible, false otherwise
     */
    protected boolean isNextAdviceVisible;

    /**
     * true when the application starts
     */
    private boolean firstTimeNavigation = true;

    /**
     * the currently estimated duration of the route to the navi destination
     */
    protected int timeToDestination;

    protected int initialTimeToDestination;

    /**
     * whether to display the navigation flag or not
     */
    public boolean showDestinationReachedFlag;

    /**
     * the distance estimated between the user current position and destination
     */
    public static int distanceEstimatedUntilDestination;

    /**
     * current speed
     */
    private double currentSpeed;

    /**
     * route distance string
     */
    protected String routeDistanceString;

    /**
     * route distance value string
     */
    protected String routeDistanceValueString;

    /**
     * current street name
     */
    protected TextView currentAdviceName;

    /**
     * current advice distance
     */
    protected TextView currentAdviceDistance;

    /**
     * current advice street name string
     */
    protected String currentVisualAdviceStreetName;

    /**
     * current advice distance string
     */
    protected int currentVisualAdviceDistance;

    /**
     * next advice street name
     */
    protected String nextVisualAdviceStreetName;

    /**
     * current speed text from free drive mode
     */
    protected TextView currentSpeedText;

    /**
     * current speed text from free drive mode
     */
    protected TextView currentSpeedTextValue;

    /**
     * current speed string
     */
    protected String currentSpeedString;

    /**
     * next advice distance
     */
    protected int nextVisualAdviceDistance;

    /**
     * next advice distance text
     */
    protected TextView nextAdviceDistanceTextView;

    /**
     * current speed unit for navigation mode
     */
    private SKMaps.SKDistanceUnitType distanceUnitType;

    /**
     * the image for the visual advice
     */
    protected ImageView currentAdviceImage;
    /**
     * The route mode.
     */
    private SKRouteSettings.SKRouteMode routeType;

    /**
     * country code
     */
    protected String currentCountryCode;
    /**
     * current follower mode
     */
    public SKMapSettings.SKMapFollowerMode currentFollowerMode= SKMapSettings.SKMapFollowerMode.HISTORIC_POSITION;
    /**
     * Click listener for settings menu views
     */
    private OnClickListener settingsItemsClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            SKToolsLogicManager.getInstance().handleSettingsItemsClick(v);
        }
    };

    /**
     * Click listener for the rest of the views
     */
    private OnClickListener itemsClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            SKToolsLogicManager.getInstance().handleItemsClick(view);
        }
    };

    /**
     * Block roads list item click
     */
    private AdapterView.OnItemClickListener blockRoadsListItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SKToolsLogicManager.getInstance().handleBlockRoadsItemsClick(parent, position);
        }
    };

    /**
     * Creates a single instance of {@link SKToolsNavigationUIManager}
     *
     * @return
     */
    public static SKToolsNavigationUIManager getInstance() {
        if (instance == null) {
            synchronized (SKToolsNavigationUIManager.class) {
                if (instance == null) {
                    instance = new SKToolsNavigationUIManager();
                }
            }
        }
        return instance;
    }

    /**
     * Sets the current activity.
     *
     * @param activity
     * @param rootId
     */
    protected void setActivity(Activity activity, int rootId) {
        this.currentActivity = activity;
        rootLayout = (ViewGroup) currentActivity.findViewById(rootId);
    }


    /**
     * Inflates navigation relates views.
     *
     * @param activity
     */
    protected void inflateNavigationViews(Activity activity) {

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                LayoutInflater inflater = currentActivity.getLayoutInflater();

                inflateSettingsMenu();

                android.widget.RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                backButtonPanel =
                        (ViewGroup) inflater.inflate(R.layout.element_navigation_back_button, null, false);
                rootLayout.addView(backButtonPanel, relativeLayoutParams);
                backButtonPanel.setId(SKToolsUtils.generateViewId());
                backButtonPanel.setVisibility(View.GONE);
                backButtonPanel.findViewById(R.id.navigation_top_back_button).setOnClickListener
                        (itemsClickListener);

                android.widget.RelativeLayout.LayoutParams routeOverviewRelativeLayoutParams = new RelativeLayout
                        .LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                routeOverviewRelativeLayoutParams.addRule(RelativeLayout.BELOW, backButtonPanel.getId());

                routeOverviewPanel =
                        (ViewGroup) inflater.inflate(R.layout.element_navigation_route_overview_panel, null, false);
                rootLayout.addView(routeOverviewPanel, routeOverviewRelativeLayoutParams);
                routeOverviewPanel.setVisibility(View.GONE);

                roadBlockPanel = (ViewGroup) inflater.inflate(R.layout.element_navigation_roadblocks_list,
                        null,
                        false);
                rootLayout.addView(roadBlockPanel, routeOverviewRelativeLayoutParams);
                roadBlockPanel.setVisibility(View.GONE);


                reRoutingPanel = (ViewGroup) inflater.inflate(R.layout.element_navigation_rerouting_panel,
                        null,
                        false);
                android.widget.RelativeLayout.LayoutParams reRoutingPanelParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                reRoutingPanelParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                rootLayout.addView(reRoutingPanel, reRoutingPanelParams);
                reRoutingPanel.setVisibility(View.GONE);

                menuOptions = (ViewGroup) inflater.inflate(R.layout.element_navigation_menu_options, null,
                        false);
                rootLayout.addView(menuOptions, relativeLayoutParams);
                menuOptions.setVisibility(View.GONE);

                topCurrentNavigationPanel = (ViewGroup) inflater.inflate(R.layout
                        .element_navigation_current_advice_panel, null, false);
                android.widget.RelativeLayout.LayoutParams topCurrentAdviceParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                topCurrentAdviceParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                rootLayout.addView(topCurrentNavigationPanel, topCurrentAdviceParams);
                topCurrentNavigationPanel.setId(SKToolsUtils.generateViewId());
                topCurrentNavigationPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                topCurrentNavigationPanel.setVisibility(View.GONE);

                topCurrentNavigationDistanceStreetPanel = (LinearLayout) topCurrentNavigationPanel.findViewById(R.id
                        .current_advice_text_holder);
                topCurrentNavigationDistanceStreetPanel.setOnClickListener(itemsClickListener);
                RelativeLayout topCurrentNavigationImagePanel = (RelativeLayout) topCurrentNavigationPanel
                        .findViewById(R.id
                                .current_advice_image_holder);
                topCurrentNavigationImagePanel.setOnClickListener(itemsClickListener);
                currentAdviceImage = (ImageView) topCurrentNavigationImagePanel.findViewById(R.id
                        .current_advice_image_turn);
                currentAdviceName = (TextView) topCurrentNavigationDistanceStreetPanel.findViewById(R.id
                        .current_advice_street_text);
                currentAdviceName.setSelected(true);
                currentAdviceDistance =
                        (TextView) topCurrentNavigationDistanceStreetPanel.findViewById(R.id
                                .current_advice_distance_text);


                // next advice panel
                topNextNavigationPanel = (ViewGroup) inflater.inflate(R.layout
                                .element_navigation_next_advice_panel,
                        null,
                        false);
                android.widget.RelativeLayout.LayoutParams nextAdviceParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                nextAdviceParams.addRule(RelativeLayout.BELOW, topCurrentNavigationPanel.getId());
                rootLayout.addView(topNextNavigationPanel, nextAdviceParams);
                topNextNavigationPanel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                topNextNavigationPanel.setVisibility(View.GONE);

                nextAdviceImageDistancePanel = (RelativeLayout) topNextNavigationPanel.findViewById(R.id
                        .next_image_turn_advice_distance_layout);
                nextAdviceImageView = (ImageView) nextAdviceImageDistancePanel.findViewById(R.id
                        .next_image_turn_advice);
                nextAdviceDistanceTextView = (TextView) nextAdviceImageDistancePanel.findViewById(R.id
                        .next_advice_distance_text);
                nextAdviceStreetNamePanel = (RelativeLayout) topNextNavigationPanel.findViewById(R.id
                        .next_advice_street_name_text_layout);
                nextAdviceStreetNameTextView = (TextView) nextAdviceStreetNamePanel.findViewById(R.id
                        .next_advice_street_name_text);
                nextAdviceStreetNameTextView.setSelected(true);

                freeDriveCurrentStreetPanel =
                        (ViewGroup) inflater.inflate(R.layout.element_free_drive_current_street_panel, null, false);
                android.widget.RelativeLayout.LayoutParams freeDrivePanelParams = new RelativeLayout.LayoutParams
                        (RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                freeDrivePanelParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                rootLayout.addView(freeDriveCurrentStreetPanel, freeDrivePanelParams);
                freeDriveCurrentStreetPanel.setVisibility(View.GONE);
                TextView freeDriveCurrentStreetText =
                        (TextView) freeDriveCurrentStreetPanel.findViewById(R.id.free_drive_current_street_text);
                freeDriveCurrentStreetText.setText("");

                viaPointPanel =
                        (ViewGroup) inflater.inflate(R.layout.element_navigation_via_point_panel, null, false);
                rootLayout.addView(viaPointPanel, freeDrivePanelParams);
                viaPointPanel.setVisibility(View.GONE);
                TextView viaPointText =
                        (TextView) viaPointPanel.findViewById(R.id.via_point_text_view);
                viaPointText.setText("");

                inflateBottomPanels();
            }
        });
    }


    /**
     * sets the corresponding image for compass panel
     */


    protected void setTheCorrespondingImageForCompassPanel(SKMapSettings mapSettings) {

        if (SKToolsLogicManager.getInstance().startPedestrian) {
            compassStates = CompassStates.HISTORICAL_POSITIONS;
        }
        if (compassViewPanel != null) {
            ImageView compassPanelImageView = (ImageView) compassViewPanel.findViewById(R.id.pedestrian_compass_panel_image_view);

            switch (compassStates) {
                case HISTORICAL_POSITIONS:
                    compassStates = CompassStates.PEDESTRIAN_COMPASS;
                    Toast.makeText(this.currentActivity, "The map will turn based on the device compass. It will point in your movement direction.", Toast.LENGTH_SHORT).show();
                    mapSettings.setFollowerMode(SKMapSettings.SKMapFollowerMode.POSITION_PLUS_HEADING);
                    currentFollowerMode = SKMapSettings.SKMapFollowerMode.POSITION_PLUS_HEADING;
                    compassPanelImageView.setBackgroundResource(R.drawable.icon_compass);
                    SKToolsLogicManager.getInstance().startPedestrian = false;
                    break;
                case PEDESTRIAN_COMPASS:
                    compassStates = CompassStates.NORTH_ORIENTED;
                    Toast.makeText(this.currentActivity, "The map will not turn. It will always stay northbound.", Toast.LENGTH_SHORT).show();
                    mapSettings.setFollowerMode(SKMapSettings.SKMapFollowerMode.POSITION);
                    currentFollowerMode = SKMapSettings.SKMapFollowerMode.POSITION;
                    compassPanelImageView.setBackgroundResource(R.drawable.icon_north_oriented);
                    break;
                case NORTH_ORIENTED:
                    compassStates = CompassStates.HISTORICAL_POSITIONS;
                    Toast.makeText(this.currentActivity, "The map will turn based on your recent positions.", Toast.LENGTH_SHORT).show();
                    mapSettings.setFollowerMode(SKMapSettings.SKMapFollowerMode.HISTORIC_POSITION);
                    currentFollowerMode = SKMapSettings.SKMapFollowerMode.HISTORIC_POSITION;
                    compassPanelImageView.setBackgroundResource(R.drawable.icon_historical_positions);
                    break;
            }
        }

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
     * Inflates the bottom views (speed panels, eta, route distance).
     */
    private void inflateBottomPanels() {
        LayoutInflater inflater = currentActivity.getLayoutInflater();
        speedPanel = (ViewGroup) inflater.inflate(R.layout
                        .element_navigation_speed_panel, null,
                false);
        android.widget.RelativeLayout.LayoutParams currentSpeedParams = new RelativeLayout.LayoutParams
                (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        currentSpeedParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        currentSpeedParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        rootLayout.addView(speedPanel, currentSpeedParams);
        speedPanel.setId(SKToolsUtils.generateViewId());
        speedPanel.setVisibility(View.GONE);
        currentSpeedText = (TextView) speedPanel.findViewById(R.id.free_drive_current_speed_text);
        currentSpeedTextValue = (TextView) speedPanel.findViewById(R.id
                .free_drive_current_speed_text_value);
        speedPanel.setOnClickListener(null);


        compassViewPanel = (ViewGroup) inflater.inflate(R.layout.element_pedestrian_compass_panel, null, false);
        android.widget.RelativeLayout.LayoutParams compassPanelParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        compassPanelParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        compassPanelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        rootLayout.addView(compassViewPanel, compassPanelParams);
        compassViewPanel.setId(SKToolsUtils.generateViewId());
        compassViewPanel.setVisibility(View.GONE);
        currentCompasImage = (ImageView) compassViewPanel.findViewById(R.id.pedestrian_compass_panel_image_view);
        compassViewPanel.findViewById(R.id.pedestrian_compass_panel_layout).setOnClickListener(itemsClickListener);


        arrivingETATimeGroupPanels = (ViewGroup) inflater.inflate(R.layout
                .element_navigation_eta_arriving_group_panels, null, false);
        android.widget.RelativeLayout.LayoutParams etaGroupPanelsParams = new RelativeLayout.LayoutParams
                (RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        etaGroupPanelsParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        etaGroupPanelsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rootLayout.addView(arrivingETATimeGroupPanels, etaGroupPanelsParams);
        arrivingETATimeGroupPanels.setId(SKToolsUtils.generateViewId());
        arrivingETATimeGroupPanels.setVisibility(View.GONE);
        estimatedTimePanel = (ViewGroup) arrivingETATimeGroupPanels.findViewById(R.id
                .navigation_bottom_right_estimated_panel);
        estimatedTimePanel.setOnClickListener(itemsClickListener);
        arrivingTimePanel = (ViewGroup) arrivingETATimeGroupPanels.findViewById(R.id
                .navigation_bottom_right_arriving_panel);
        arrivingTimePanel.setOnClickListener(itemsClickListener);
        estimatedTimeText = (TextView) estimatedTimePanel.findViewById(R.id.estimated_navigation_time_text);
        arrivingTimeText = (TextView) arrivingTimePanel.findViewById(R.id.arriving_time_text);

        android.widget.RelativeLayout.LayoutParams routeDistanceParams;
        if (currentActivity.getResources().getConfiguration().orientation == Configuration
                .ORIENTATION_PORTRAIT) {
            routeDistanceParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            routeDistanceParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            routeDistanceParams.addRule(RelativeLayout.LEFT_OF, arrivingETATimeGroupPanels.getId());
            if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
                routeDistanceParams.addRule(RelativeLayout.RIGHT_OF, compassViewPanel.getId());
            } else {
                routeDistanceParams.addRule(RelativeLayout.RIGHT_OF, speedPanel.getId());
            }
        } else {
            routeDistanceParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            routeDistanceParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            routeDistanceParams.addRule(RelativeLayout.ABOVE, arrivingETATimeGroupPanels.getId());
        }

        routeDistancePanel = (ViewGroup) inflater.inflate(R.layout
                        .element_navigation_route_distance, null,
                false);
        rootLayout.addView(routeDistancePanel, routeDistanceParams);
        routeDistancePanel.setVisibility(View.GONE);
        routeDistanceText = (TextView) routeDistancePanel.findViewById(R.id.arriving_distance_text);
        routeDistanceTextValue = (TextView) routeDistancePanel.findViewById(R.id
                .arriving_distance_text_value);
        android.widget.RelativeLayout.LayoutParams positionMeParams;
        positionMeButtonPanel =
                (ViewGroup) inflater.inflate(R.layout.element_position_me_button, null, false);
        positionMeParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rootLayout.addView(positionMeButtonPanel, positionMeParams);
        positionMeButtonPanel.setVisibility(View.GONE);
        positionMeButtonPanel.findViewById(R.id.position_me_real_navigation_button).setOnClickListener
                (itemsClickListener);
        TextView dayNightText = ((TextView) settingsPanel.findViewById(R.id
                .navigation_settings_view_mode_text));
        if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
            switchMapMode(SKMapSettings.SKMapDisplayMode.MODE_2D);
        }
    }

    /**
     * Inflates simulation navigation type buttons.
     */
    public void inflateSimulationViews() {
        LayoutInflater inflater = currentActivity.getLayoutInflater();
        navigationSimulationPanel = (ViewGroup) inflater.inflate(R.layout.element_navigation_simulation_buttons,
                null, false);
        RelativeLayout.LayoutParams simulationPanelParams = new RelativeLayout.LayoutParams(RelativeLayout.
                LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        navigationSimulationPanel.setLayoutParams(simulationPanelParams);
        rootLayout.addView(navigationSimulationPanel, simulationPanelParams);
        navigationSimulationPanel.findViewById(R.id.menu_back_follower_mode_button).setOnClickListener
                (itemsClickListener);
        navigationSimulationPanel.findViewById(R.id.navigation_increase_speed).setOnClickListener(itemsClickListener);
        navigationSimulationPanel.findViewById(R.id.navigation_decrease_speed).setOnClickListener(itemsClickListener);
    }

    /**
     * Inflates simulation menu.
     */
    private void inflateSettingsMenu() {
        LayoutInflater inflater = currentActivity.getLayoutInflater();

        settingsPanel = (ViewGroup) inflater.inflate(R.layout.element_navigation_settings, null, false);
        RelativeLayout.LayoutParams settingsPanelParams = new RelativeLayout.LayoutParams(RelativeLayout.
                LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        rootLayout.addView(settingsPanel, settingsPanelParams);
        settingsPanel.setVisibility(View.GONE);

        settingsPanel.findViewById(R.id.navigation_settings_audio_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_day_night_mode_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_overview_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_route_info_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_roadblock_info_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_panning_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_view_mode_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_quit_button).setOnClickListener
                (settingsItemsClickListener);
        settingsPanel.findViewById(R.id.navigation_settings_back_button).setOnClickListener
                (settingsItemsClickListener);
    }

    /**
     * Inflates the pre navigation views, that contain the panels with alternative routes.
     */
    private void inflatePreNavigationViews() {
        LayoutInflater inflater = currentActivity.getLayoutInflater();
        preNavigationPanel = (ViewGroup) inflater.inflate(R.layout.element_pre_navigation_buttons_panel, null,
                false);
        RelativeLayout.LayoutParams preNavigationPanelParams = new RelativeLayout.LayoutParams(RelativeLayout.
                LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rootLayout.addView(preNavigationPanel, preNavigationPanelParams);
        preNavigationPanel.setVisibility(View.GONE);

        preNavigationPanel.findViewById(R.id.first_route).setOnClickListener(itemsClickListener);
        preNavigationPanel.findViewById(R.id.second_route).setOnClickListener(itemsClickListener);
        preNavigationPanel.findViewById(R.id.third_route).setOnClickListener(itemsClickListener);
        preNavigationPanel.findViewById(R.id.cancel_pre_navigation_button).setOnClickListener(itemsClickListener);
        preNavigationPanel.findViewById(R.id.menu_back_prenavigation_button).setOnClickListener(itemsClickListener);
        preNavigationPanel.findViewById(R.id.start_navigation_button).setOnClickListener
                (itemsClickListener);
    }

    /**
     * Shows the start navigation button from pre navigation panel.
     */
    public void showStartNavigationPanel() {
        preNavigationPanel.findViewById(R.id.start_navigation_button).setVisibility(View.VISIBLE);
    }

    /**
     * Sets the pre navigation buttons, related to alternative routes with corresponding time and distance.
     *
     * @param id
     * @param time
     * @param distance
     */
    public void sePreNavigationButtons(final int id, final String time, final String distance) {
        if (preNavigationPanel != null) {
            final TextView oneRoute = (TextView) preNavigationPanel.findViewById(R.id.first_route);
            final TextView twoRoutes = (TextView) preNavigationPanel.findViewById(R.id.second_route);
            final TextView threeRoutes = (TextView) preNavigationPanel.findViewById(R.id.third_route);

            altRoutesButtons =
                    new TextView[]{(TextView) preNavigationPanel.findViewById(R.id.first_route),
                            (TextView) preNavigationPanel.findViewById(R.id.second_route),
                            (TextView) preNavigationPanel.findViewById(R.id.third_route)};
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (id == 0) {
                        oneRoute.setVisibility(View.VISIBLE);
                        twoRoutes.setVisibility(View.GONE);
                        threeRoutes.setVisibility(View.GONE);
                        altRoutesButtons[0].setText(time + "\n"
                                + distance);

                    } else if (id == 1) {
                        twoRoutes.setVisibility(View.VISIBLE);
                        threeRoutes.setVisibility(View.GONE);
                        altRoutesButtons[1].setText(time + "\n"
                                + distance);

                    } else if (id == 2) {
                        threeRoutes.setVisibility(View.VISIBLE);
                        altRoutesButtons[2].setText(time + "\n"
                                + distance);

                    }
                }
            });
        }
    }

    /**
     * Shows the pre navigation screen.
     */
    public void showPreNavigationScreen() {
        inflatePreNavigationViews();
        showViewIfNotVisible(preNavigationPanel);
        currentNavigationMode = NavigationMode.PRE_NAVIGATION;
    }

    /**
     * Selects an alternative route button depending of the index.
     *
     * @param routeIndex
     */
    public void selectAlternativeRoute(int routeIndex) {
        if (altRoutesButtons == null) {
            return;
        }
        for (TextView b : altRoutesButtons) {
            b.setSelected(false);
        }

        switch (routeIndex) {
            case 0:
                altRoutesButtons[0].setBackgroundColor(currentActivity.getResources().getColor(R.color.blue_panel_night_background));
                altRoutesButtons[1].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[2].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[0].setSelected(true);
                break;
            case 1:
                altRoutesButtons[0].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[1].setBackgroundColor(currentActivity.getResources().getColor(R.color.blue_panel_night_background));
                altRoutesButtons[2].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[1].setSelected(true);
                break;
            case 2:
                altRoutesButtons[0].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[1].setBackgroundColor(currentActivity.getResources().getColor(R.color.grey_options_group));
                altRoutesButtons[2].setBackgroundColor(currentActivity.getResources().getColor(R.color.blue_panel_night_background));
                altRoutesButtons[2].setSelected(true);
                break;

        }
    }

    /**
     * Handles back button in pre navigation and follower mode.
     */
    public void handleNavigationBackButton() {

        if (currentNavigationMode == NavigationMode.PRE_NAVIGATION) {
            Button backButton = (Button) currentActivity.findViewById(R.id.menu_back_prenavigation_button);
            Button cancelButton = (Button) currentActivity.findViewById(R.id.cancel_pre_navigation_button);
            if (backButton.getText().equals(">")) {
                backButton.setText("<");
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                backButton.setText(">");
                cancelButton.setVisibility(View.GONE);
            }
        } else if (currentNavigationMode == NavigationMode.FOLLOWER) {
            Button backFollowerModeButton = (Button) currentActivity.findViewById(R.id.menu_back_follower_mode_button);
            RelativeLayout increaseDecreaseLayout = (RelativeLayout) currentActivity.findViewById(R.id
                    .increase_decrease_layout);
            if (backFollowerModeButton.getText().equals(">")) {
                backFollowerModeButton.setText("<");
                increaseDecreaseLayout.setVisibility(View.VISIBLE);
            } else {
                backFollowerModeButton.setText(">");
                increaseDecreaseLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Changes the menu settings for free drive.
     *
     * @param isLandscape
     */
    public void setSettingsMenuForFreeDrive(boolean isLandscape) {
        if (!isLandscape) {
            currentActivity.findViewById(R.id.nav_settings_second_row).setVisibility(View.GONE);

            TextView routeInfo = (TextView) currentActivity.findViewById(R.id.navigation_settings_roadblock_info_text);
            routeInfo.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_routeinfo, 0, 0);
            routeInfo.setText(currentActivity.getResources().getString(R.string
                    .navigate_settings_routeinfo));
        } else {
            currentActivity.findViewById(R.id.navigation_settings_overview_button).setVisibility(View.GONE);
            currentActivity.findViewById(R.id.navigation_settings_roadblock_info_button).setVisibility(View.GONE);
        }

    }

    /**
     * hide view is visible
     *
     * @param target
     */
    private void hideViewIfVisible(ViewGroup target) {
        if (target != null && target.getVisibility() == View.VISIBLE) {
            target.setVisibility(View.GONE);
        }
    }

    /**
     * shows the view if not visible
     *
     * @param target
     */
    private void showViewIfNotVisible(ViewGroup target) {
        if (target != null && target.getVisibility() == View.GONE) {
            target.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the block road screen.
     *
     * @param distanceUnit
     * @param distanceToDestination
     */
    public void showRoadBlockMode(SKMaps.SKDistanceUnitType distanceUnit, long distanceToDestination) {
        currentNavigationMode = NavigationMode.ROADBLOCK;

        List<String> items = getRoadBlocksOptionsList(distanceUnit, distanceToDestination);
        final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(currentActivity,
                android.R.layout.simple_list_item_1, items);
        currentActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final ListView listView = (ListView) roadBlockPanel
                        .findViewById(R.id.roadblock_list);
                listView.setAdapter(listAdapter);
                listView.setOnItemClickListener(blockRoadsListItemClickListener);
                roadBlockPanel.setVisibility(View.VISIBLE);
                backButtonPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Gets the list with road block distance options.
     *
     * @param distanceUnit
     * @param distanceToDestination
     * @return
     */
    private List<String> getRoadBlocksOptionsList(SKMaps.SKDistanceUnitType distanceUnit, long distanceToDestination) {
        List<String> sourceList = new ArrayList<String>();
        List<String> roadBlocksList = new LinkedList<String>();
        String[] list;
        switch (distanceUnit) {
            case DISTANCE_UNIT_KILOMETER_METERS:
                list = currentActivity.getResources().getStringArray(R.array.road_blocks_in_meters);
                break;
            case DISTANCE_UNIT_MILES_FEET:
                list = currentActivity.getResources().getStringArray(R.array.road_blocks_in_feet);
                break;
            case DISTANCE_UNIT_MILES_YARDS:
                list = currentActivity.getResources().getStringArray(R.array.road_blocks_in_yards);
                break;
            default:
                list = currentActivity.getResources().getStringArray(R.array.road_blocks_in_meters);
                break;
        }
        Collections.addAll(roadBlocksList, list);
        final long distance = distanceToDestination;
        // we initialize the sourceList with the elements in the roadBlocksList
        // that are smaller than the distance to destination
        if (distance < 500) {
            sourceList.addAll(roadBlocksList.subList(0, 2));
        } else if (distance < 2000) {
            sourceList.addAll(roadBlocksList.subList(0, 3));
        } else if (distance < 5000) {
            sourceList.addAll(roadBlocksList.subList(0, 4));
        } else if (distance < 10000) {
            sourceList.addAll(roadBlocksList.subList(0, 5));
        } else if (distance < 150000) {
            sourceList.addAll(roadBlocksList.subList(0, 6));
        } else {
            sourceList.addAll(roadBlocksList);
        }

        // if the road has no blocks, we remove the "Unblock all" option
        if (!SKToolsLogicManager.getInstance().isRoadBlocked()) {
            sourceList.remove(0);
        }

        return sourceList;
    }


    /**
     * Checks if is in pre navigation mode.
     *
     * @return
     */
    public boolean isPreNavigationMode() {
        return currentNavigationMode == NavigationMode.PRE_NAVIGATION;
    }

    /**
     * Checks if the navigation is in follower mode.
     *
     * @return
     */
    public boolean isFollowerMode() {
        return currentNavigationMode == NavigationMode.FOLLOWER;
    }

    /**
     * Checks if the navigation is in panning mode
     * @return
     */
    public boolean isPanningMode() {
        return currentNavigationMode == NavigationMode.PANNING;
    }

    /**
     * Sets the navigation in follower mode.
     */
    public void setFollowerMode() {
        currentNavigationMode = NavigationMode.FOLLOWER;
    }

    /**
     * Shows the panel from the main navigation screen.
     *
     * @param isSimulationMode
     */
    public void showFollowerModePanels(boolean isSimulationMode) {

        hideViewIfVisible(positionMeButtonPanel);
        hideViewIfVisible(settingsPanel);
        hideViewIfVisible(roadBlockPanel);
        hideViewIfVisible(backButtonPanel);
        hideViewIfVisible(routeOverviewPanel);

        if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
            showViewIfNotVisible(compassViewPanel);
        } else {
            showViewIfNotVisible(speedPanel);
        }

        if (!isFreeDrive) {
            showViewIfNotVisible(routeDistancePanel);
            showViewIfNotVisible(topCurrentNavigationPanel);
            showViewIfNotVisible(arrivingETATimeGroupPanels);
        } else {
            showViewIfNotVisible(freeDriveCurrentStreetPanel);
        }

        if (isSimulationMode) {
            showViewIfNotVisible(navigationSimulationPanel);
        }
    }

    /**
     * Shows the panning mode screen.
     *
     * @param isNavigationTypeReal
     */
    public void showPanningMode(boolean isNavigationTypeReal) {
        currentNavigationMode = NavigationMode.PANNING;
        if (isNavigationTypeReal) {
            showViewIfNotVisible(positionMeButtonPanel);
        }
        hideViewIfVisible(navigationSimulationPanel);
        hideNavigationOrFreeDrivePanels();
        showViewIfNotVisible(backButtonPanel);
        cancelSpeedExceededThread();
    }

    /**
     * Shows the setting menu screen.
     */
    public void showSettingsMode() {
        currentNavigationMode = NavigationMode.SETTINGS;
        hideViewIfVisible(navigationSimulationPanel);
        initialiseVolumeSeekBar();
        hideNavigationOrFreeDrivePanels();
        showViewIfNotVisible(settingsPanel);
    }

    /**
     * Hides the navigation or free drive specific panels
     */
    private void hideNavigationOrFreeDrivePanels() {
        hideViewIfVisible(topNextNavigationPanel);
        hideViewIfVisible(topCurrentNavigationPanel);
        hideViewIfVisible(routeOverviewPanel);
        hideViewIfVisible(reRoutingPanel);
        hideViewIfVisible(freeDriveCurrentStreetPanel);
        hideBottomAndLeftPanels();
    }

    /**
     * Shows the overview screen.
     *
     * @param address
     */
    public void showOverviewMode(String address) {
        currentNavigationMode = NavigationMode.ROUTE_OVERVIEW;

        hideViewIfVisible(topNextNavigationPanel);
        hideViewIfVisible(topCurrentNavigationPanel);
        hideBottomAndLeftPanels();

        routeOverviewPanel.findViewById(R.id.navigation_route_overview_starting_position_layout).setVisibility(View
                .GONE);

        ((TextView) routeOverviewPanel.findViewById(R.id
                .navigation_route_overview_destination_text)).setText(address);
        routeOverviewPanel.setVisibility(View.VISIBLE);
        backButtonPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the route info screen in simple navigation mode.
     *
     * @param startAddress
     * @param destinationAddress
     */
    public void showRouteInfoScreen(String startAddress, String destinationAddress) {
        currentNavigationMode = NavigationMode.ROUTE_INFO;

        hideViewIfVisible(topNextNavigationPanel);
        hideViewIfVisible(topCurrentNavigationPanel);
        hideBottomAndLeftPanels();

        ((TextView) routeOverviewPanel.findViewById(R.id
                .navigation_route_overview_current_position_text)).setText(startAddress);
        ((TextView) routeOverviewPanel.findViewById(R.id
                .navigation_route_overview_destination_text)).setText(destinationAddress);

        routeOverviewPanel.findViewById(R.id.navigation_route_overview_starting_position_layout).setVisibility(View
                .VISIBLE);
        routeOverviewPanel.setVisibility(View.VISIBLE);
        backButtonPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the route info screen from free drive mode.
     */
    public void showRouteInfoFreeDriveScreen() {
        currentNavigationMode = NavigationMode.ROUTE_INFO;

        hideViewIfVisible(freeDriveCurrentStreetPanel);
        hideBottomAndLeftPanels();
        routeOverviewPanel.findViewById(R.id.navigation_route_overview_starting_position_layout).setVisibility(View
                .GONE);

        routeOverviewPanel.findViewById(R.id.navigation_route_overview_destination_layout).setVisibility(View
                .VISIBLE);

        ((TextView) routeOverviewPanel.findViewById(R.id
                .navigation_route_overview_destination_label)).setText(R.string.
                current_position);
        ((TextView) routeOverviewPanel.findViewById(R.id
                .navigation_route_overview_destination_text)).setText(currentStreetNameFreeDriveString);

        routeOverviewPanel.setVisibility(View.VISIBLE);
        backButtonPanel.setVisibility(View.VISIBLE);
    }

    /**
     * removes the top panels
     */
    public void hideTopPanels() {
        hideViewIfVisible(reRoutingPanel);
        hideViewIfVisible(topNextNavigationPanel);
        hideViewIfVisible(topCurrentNavigationPanel);
        hideViewIfVisible(freeDriveCurrentStreetPanel);
        hideViewIfVisible(navigationSimulationPanel);
        hideViewIfVisible(viaPointPanel);
    }

    /**
     * removes the bottom and bottom left panels
     */
    public void hideBottomAndLeftPanels() {
        hideViewIfVisible(routeDistancePanel);
        hideViewIfVisible(speedPanel);
        hideViewIfVisible(arrivingETATimeGroupPanels);
        hideViewIfVisible(compassViewPanel);
    }

    /**
     * Hide the settings menu screen.
     */
    public void hideSettingsPanel() {
        hideViewIfVisible(settingsPanel);
    }

    /**
     * Shows the rerouting panel.
     */
    public void showReroutingPanel() {
        showViewIfNotVisible(reRoutingPanel);
    }

    /**
     * Shows the exit navigation dialog.
     */
    public void showExitNavigationDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                currentActivity);

        alertDialog.setTitle(R.string.exit_navigation_dialog_title);
        alertDialog.setMessage(currentActivity.getResources().getString(
                R.string.exit_navigation_dialog_message));
        alertDialog.setPositiveButton(
                currentActivity.getResources().getString(R.string.ok_label),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        isFreeDrive = false;
                        currentNavigationMode = NavigationMode.POST_NAVIGATION;
                        SKToolsLogicManager.getInstance().stopNavigation();
                    }
                });
        alertDialog.setNegativeButton(
                currentActivity.getResources().getString(R.string.cancel_label),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    /**
     * Shows a dialog that notifies that the route calculation failed.
     *
     * @param statusCode
     */
    public void showRouteCalculationFailedDialog(final SKRouteListener.SKRoutingErrorCode statusCode) {
        currentActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                String dialogMessage;
                final Resources res = currentActivity.getResources();
                switch (statusCode) {
                    case SAME_START_AND_DESTINATION:
                        dialogMessage = res
                                .getString(R.string.route_same_start_and_destination);
                        break;
                    case INVALID_START:
                        dialogMessage = res.getString(R.string.route_invalid_start);
                        break;
                    case INVALID_DESTINATION:
                        dialogMessage = res
                                .getString(R.string.route_invalid_destination);
                        break;
                    case INTERNAL_ERROR:
                        dialogMessage = res
                                .getString(R.string.route_unknown_server_error);
                        break;
                    case ROUTE_CANNOT_BE_CALCULATED:
                    default:
                        dialogMessage = res
                                .getString(R.string.route_cannot_be_calculated);
                        break;
                }

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        currentActivity);
                alertDialog.setTitle(R.string.routing_server_error);
                alertDialog.setMessage(dialogMessage);
                alertDialog.setNeutralButton(res.getString(R.string.ok_label),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    /**
     * Resets the values to a default value.
     *
     * @param distanceUnit
     */
    public void reset(SKMaps.SKDistanceUnitType distanceUnit) {
        distanceUnitType = distanceUnit;
        nextVisualAdviceDistance = 0;
        routeDistanceString = "";
        nextVisualAdviceStreetName = "";
        currentVisualAdviceDistance = 0;
        currentVisualAdviceStreetName = "";
        estimatedTimePanelVisible = true;
        isNextAdviceVisible = false;
        firstAdviceReceived = false;
        timeToDestination = 0;
        currentSpeedLimit = 0;
        currentSpeed = 0;
        initialTimeToDestination = 0;
    }

    /**
     * Handles the navigation state update.
     *
     * @param skNavigationState
     * @param mapStyle
     */
    public void handleNavigationState(final SKNavigationState skNavigationState,
                                      final int mapStyle) {

        if (currentNavigationMode == NavigationMode.FOLLOWER) {

            currentActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    hideViewIfVisible(reRoutingPanel);
                    if (currentNavigationMode == NavigationMode.FOLLOWER) {
                        showViewIfNotVisible(topCurrentNavigationPanel);
                        showViewIfNotVisible(routeDistancePanel);
                        if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
                            showViewIfNotVisible(compassViewPanel);
                        } else {
                            showViewIfNotVisible(speedPanel);
                        }

                    }

                    currentCountryCode = skNavigationState.getCountryCode();
                    distanceEstimatedUntilDestination = (int) Math.round(skNavigationState
                            .getDistanceToDestination());

                    String currentVisualAdviceImage = skNavigationState.getCurrentAdviceVisualAdviceFile();

                    Bitmap decodedAdvice = SKToolsUtils.decodeFileToBitmap(currentVisualAdviceImage);
                    if (decodedAdvice != null) {
                        currentAdviceImage.setImageBitmap(decodedAdvice);
                        currentAdviceImage.setVisibility(View.VISIBLE);
                    }

                    String nextStreetName = skNavigationState.getCurrentAdviceNextStreetName();
                    String nextAdviceNextStreetName = skNavigationState.getNextAdviceNextStreetName();
                    if (nextAdviceNextStreetName != null && nextAdviceNextStreetName.equals("")) {
                        nextAdviceNextStreetName = null;
                        skNavigationState.setNextAdviceNextStreetName(null);
                    }
                    String exitNumber = skNavigationState.getCurrentAdviceExitNumber();
                    if (nextStreetName != null) {
                        nextStreetName =
                                nextStreetName.replace("\u021B", "\u0163").replace("\u021A", "\u0162")
                                        .replace("\u0218", "\u015E").replace("\u0219", "\u015F");
                    }
                    String countryCode = skNavigationState.getCountryCode();
                    String nextVisualAdviceFile = skNavigationState.getNextAdviceVisualAdviceFile();
                    if (nextVisualAdviceFile != null && nextVisualAdviceFile.equals("")) {
                        nextVisualAdviceFile = null;
                        skNavigationState.setNextAdviceVisualAdviceFile(null);
                    }
                    showDestinationReachedFlag = skNavigationState.isLastAdvice();

                    if (initialTimeToDestination == 0) {
                        initialTimeToDestination = skNavigationState.getCurrentAdviceTimeToDestination();
                    }

                    nextStreetType = skNavigationState.getCurrentAdviceNextOsmStreetType().getValue();
                    secondNextStreetType = skNavigationState.getNextAdviceNextOsmStreetType().getValue();

                    final int currentDistanceToAdvice = skNavigationState.getCurrentAdviceDistanceToAdvice();
                    int nextDistanceToAdvice = skNavigationState.getNextAdviceDistanceToAdvice();

                    // speed values
                    if (currentSpeed == 0 || currentSpeed != skNavigationState.getCurrentSpeed()) {
                        currentSpeed = skNavigationState.getCurrentSpeed();
                        currentSpeedString = String.valueOf(SKToolsUtils.getSpeedByUnit(currentSpeed,
                                distanceUnitType));
                        currentSpeedText.setText(currentSpeedString);
                        currentSpeedTextValue.setText(SKToolsUtils.getSpeedTextByUnit(currentActivity,
                                distanceUnitType));
                    }

                    if (currentSpeedLimit != skNavigationState.getCurrentSpeedLimit()) {
                        currentSpeedLimit = skNavigationState.getCurrentSpeedLimit();
                        handleSpeedLimitAvailable(countryCode, distanceUnitType, mapStyle);
                    }

                    if (navigationTotalDistance == 0) {
                        navigationTotalDistance = distanceEstimatedUntilDestination;
                    }

                    // set next advice content & visibility
                    if (nextVisualAdviceFile != null) {
                        if (nextVisualAdviceDistance != nextDistanceToAdvice) {
                            nextVisualAdviceDistance = nextDistanceToAdvice;
                            nextAdviceDistanceTextView.setText(SKNavigationManager.getInstance().formatDistance
                                    (nextDistanceToAdvice));
                        }
                        if (nextVisualAdviceStreetName != null &&
                                !nextVisualAdviceStreetName.equals
                                        (nextAdviceNextStreetName)) {
                            nextVisualAdviceStreetName = nextAdviceNextStreetName;
                            nextAdviceStreetNameTextView.setText
                                    (nextAdviceNextStreetName);
                        }

                        Bitmap adviceFile = SKToolsUtils.decodeFileToBitmap(nextVisualAdviceFile);
                        if (adviceFile != null) {
                            nextAdviceImageView.setImageBitmap(adviceFile);
                            nextAdviceImageView.setVisibility(View.VISIBLE);
                        }

                        setNextAdviceStreetNameVisibility();
                    }

                    if (currentNavigationMode == NavigationMode.FOLLOWER && firstAdviceReceived) {

                        if (!isNextAdviceVisible) {
                            if (nextVisualAdviceFile != null) {
                                isNextAdviceVisible = true;
                                showNextAdvice();
                            }
                        } else {
                            if (nextVisualAdviceFile == null) {
                                isNextAdviceVisible = false;
                                topNextNavigationPanel.setVisibility(View.GONE);
                            }
                        }
                    }

                    // set current advice content
                    if (currentAdviceDistance != null &&
                            currentVisualAdviceDistance != currentDistanceToAdvice) {
                        currentVisualAdviceDistance = currentDistanceToAdvice;
                        currentAdviceDistance.setText(SKNavigationManager.getInstance().formatDistance
                                (currentDistanceToAdvice));
                    }
                    if (currentAdviceName != null && !showDestinationReachedFlag) {
                        if (exitNumber != null && exitNumber.length() > 0) {
                            String currentAdvice = currentActivity
                                    .getResources().getString(
                                            R.string.exit_highway_advice_label) + " " + exitNumber;
                            if (nextStreetName != null && nextStreetName.length() > 0) {
                                currentAdvice = currentAdvice + " " +
                                        nextStreetName;
                            }
                            currentAdviceName.setText(currentAdvice);
                            currentVisualAdviceStreetName = currentAdvice;
                        } else {
                            if (currentVisualAdviceStreetName != null &&
                                    !currentVisualAdviceStreetName.equals
                                            (nextStreetName)) {
                                currentVisualAdviceStreetName = nextStreetName;
                                currentAdviceName.setText(nextStreetName);
                            }
                        }
                    }

                    if (showDestinationReachedFlag) {
                        if (currentAdviceImage != null) {
                            currentAdviceImage.setImageResource(R.drawable.ic_destination_advise_black);
                        }
                        if (currentAdviceName != null) {
                            currentVisualAdviceStreetName =
                                    currentActivity.getResources().getString(
                                            R.string.destination_reached_info_text);
                            currentAdviceName.setText(
                                    currentVisualAdviceStreetName);
                        }
                        if (currentAdviceDistance != null) {
                            currentVisualAdviceDistance = 0;
                            currentAdviceDistance.setVisibility(View.GONE);
                        }
                        disableNextAdvice();
                    }


                    // set estimated/arriving time
                    if ((timeToDestination < 120) || (timeToDestination - 60 >=
                            skNavigationState.getCurrentAdviceTimeToDestination()) || (timeToDestination + 60 <
                            skNavigationState.getCurrentAdviceTimeToDestination())) {

                        timeToDestination = skNavigationState.getCurrentAdviceTimeToDestination();
                        if (estimatedTimePanelVisible) {
                            showEstimatedTime();
                        } else {
                            showArrivingTime();
                        }
                    }

                    String[] distanceToDestinationSplit = SKNavigationManager.getInstance().formatDistance
                            (distanceEstimatedUntilDestination).split(" ");
                    if (!routeDistanceString.equals(distanceToDestinationSplit[0])) {
                        routeDistanceString = distanceToDestinationSplit[0];
                        if (distanceToDestinationSplit.length > 1) {
                            routeDistanceValueString = distanceToDestinationSplit[1];
                        }
                        setRouteDistanceFields();
                    }

                    // when we receive the first advice we show the panels that were set accordingly
                    if (!firstAdviceReceived) {
                        firstAdviceReceived = true;

                        if (currentNavigationMode == NavigationMode.FOLLOWER) {
                            showViewIfNotVisible(topCurrentNavigationPanel);

                            if (nextVisualAdviceFile != null) {
                                isNextAdviceVisible = true;
                                showNextAdvice();
                            } else {
                                isNextAdviceVisible = false;
                            }
                            if (currentAdviceDistance != null &&
                                    !showDestinationReachedFlag) {
                                currentAdviceDistance.setVisibility(View.VISIBLE);
                            }
                            if (!firstTimeNavigation) {
                                topCurrentNavigationPanel.bringToFront();
                            }

                            showViewIfNotVisible(routeDistancePanel);
                            showViewIfNotVisible(arrivingETATimeGroupPanels);
                        }
                    }
                }
            });
        }

    }

    /**
     * sets top panels background colour
     */
    public void setTopPanelsBackgroundColour(int mapStyle, boolean currentAdviceChanged,
                                             boolean nextAdviceChanged) {
        if (Arrays.asList(signPostsCountryExceptions).contains(currentCountryCode)) {
            isDefaultTopPanelBackgroundColor = false;
            if (currentAdviceChanged || !nextAdviceChanged) {
                verifyStreetType(mapStyle, nextStreetType, true);
            }
            if (nextStreetType == secondNextStreetType) {
                if (nextAdviceChanged && isNextAdviceVisible ||
                        !currentAdviceChanged && !nextAdviceChanged) {
                    verifyStreetType(mapStyle, 0, false);
                }
            } else {
                if (nextAdviceChanged && isNextAdviceVisible ||
                        !currentAdviceChanged && !nextAdviceChanged) {
                    verifyStreetType(mapStyle, secondNextStreetType, false);
                }
            }
        } else {
            if (!isDefaultTopPanelBackgroundColor) {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    if (currentAdviceChanged || !nextAdviceChanged) {
                        setTopPanelsStyle(R.color.white, R.color.black, true);
                    }
                    if (nextAdviceChanged && isNextAdviceVisible ||
                            !currentAdviceChanged && !nextAdviceChanged) {
                        setTopPanelsStyle(R.color.white, R.color.black, false);
                    }

                } else {
                    if (currentAdviceChanged || !nextAdviceChanged) {
                        setTopPanelsStyle(R.color.navigation_style_night,
                                R.color.gray, true);
                    }
                    if (nextAdviceChanged && isNextAdviceVisible ||
                            !currentAdviceChanged && !nextAdviceChanged) {
                        setTopPanelsStyle(R.color.navigation_style_night,
                                R.color.gray, false);
                    }
                }
            }
            isDefaultTopPanelBackgroundColor = true;
        }
    }


    /**
     * verifies the street type and sets the colors for top panels
     *
     * @param mapStyle
     * @param streetType
     * @param forCurrent
     */
    private void verifyStreetType(int mapStyle, int streetType, boolean forCurrent) {
        if (streetType == OSM_STREET_TYPE_MOTORWAY || streetType == OSM_STREET_TYPE_MOTORWAY_LINK) {
            if ((currentCountryCode.equals("CH")) || (currentCountryCode.equals("US"))) {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.green_panel_day_background, R.color.white, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.green_panel_night_background, R.color.green_panel_night_text, forCurrent);
                }
            } else {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.blue_panel_day_background, R.color.white, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.blue_panel_night_background, R.color.blue_panel_night_text, forCurrent);
                }
            }
        } else if (streetType == OSM_STREET_TYPE_PRIMARY || streetType == OSM_STREET_TYPE_PRIMARY_LINK) {
            if (currentCountryCode.equals("CH")) {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.blue_panel_day_background, R.color.white, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.blue_panel_night_background, R.color.blue_panel_night_text, forCurrent);
                }
            } else if (currentCountryCode.equals("US")) {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.green_panel_day_background, R.color.white, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.green_panel_night_background, R.color.green_panel_night_text, forCurrent);
                }
            } else {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.yellow_panel_day_background, R.color.black, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.yellow_panel_night_background, R.color.yellow_panel_night_text,
                            forCurrent);
                }
            }
        } else if (streetType == OSM_STREET_TYPE_TRUNK || streetType == OSM_STREET_TYPE_TRUNK_LINK) {
            if ((currentCountryCode.equals("GB")) || (currentCountryCode.equals("US"))) {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.green_panel_day_background, R.color.white, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.green_panel_night_background, R.color.green_panel_night_text, forCurrent);
                }
            } else {
                if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                    setTopPanelsStyle(R.color.white, R.color.black, forCurrent);
                } else {
                    setTopPanelsStyle(R.color.navigation_style_night,
                            R.color.gray, forCurrent);
                }
            }
        } else {
            if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                setTopPanelsStyle(R.color.white, R.color.black, forCurrent);
            } else {
                setTopPanelsStyle(R.color.navigation_style_night,
                        R.color.gray, forCurrent);
            }
        }
    }

    /**
     * sets the background for top panel and the text color for the texts inside
     * it
     *
     * @param drawableId
     * @param textColor
     * @param forCurrentAdvice
     */
    protected void setTopPanelsStyle(int drawableId, int textColor, boolean forCurrentAdvice) {
        if (!isFreeDrive) {
            if (forCurrentAdvice) {
                if (topCurrentNavigationDistanceStreetPanel != null) {
                    topCurrentNavigationDistanceStreetPanel.setBackgroundColor(currentActivity.getResources()
                            .getColor(drawableId));
                }
                if (topCurrentNavigationPanel != null) {
                    RelativeLayout topCurrentNavigationImagePanel = (RelativeLayout) topCurrentNavigationPanel
                            .findViewById(R.id
                                    .current_advice_image_holder);
                    if (topCurrentNavigationImagePanel != null) {
                        topCurrentNavigationImagePanel.setBackgroundColor(currentActivity.getResources().getColor
                                (drawableId));
                    }
                }
                if (currentAdviceDistance != null) {
                    currentAdviceDistance.setTextColor(currentActivity.getResources().getColor(textColor));
                }
                if (currentAdviceName != null) {
                    currentAdviceName.setTextColor(currentActivity.getResources().getColor(textColor));
                }
                currentAdviceBackgroundDrawableId = drawableId;
            } else {
                if (nextAdviceImageDistancePanel != null) {
                    nextAdviceImageDistancePanel.setBackgroundColor(currentActivity.getResources().getColor
                            (drawableId));
                }
                if (nextAdviceStreetNamePanel != null) {
                    nextAdviceStreetNamePanel.setBackgroundColor(currentActivity.getResources().getColor
                            (drawableId));
                }
                if (nextAdviceDistanceTextView != null) {
                    nextAdviceDistanceTextView.setTextColor(currentActivity.getResources().getColor(textColor));
                }
                if (nextAdviceStreetNameTextView != null) {
                    nextAdviceStreetNameTextView.setTextColor(currentActivity.getResources().getColor(textColor));
                }
                nextAdviceBackgroundDrawableId = drawableId;
                setNextAdviceOverlayVisibility();
            }
        } else {
            TextView freeDriveCurrentStreetText = (TextView) freeDriveCurrentStreetPanel.findViewById(R.id
                    .free_drive_current_street_text);
            if (freeDriveCurrentStreetText != null) {
                freeDriveCurrentStreetText.setBackgroundColor(currentActivity.getResources().getColor
                        (drawableId));
                freeDriveCurrentStreetText.setTextColor(currentActivity.getResources().getColor(textColor));
            }
            currentAdviceBackgroundDrawableId = drawableId;
        }
    }


    /**
     * sets the advice overlay semi transparent visibility
     */
    public void setNextAdviceOverlayVisibility() {
        if (currentAdviceBackgroundDrawableId == nextAdviceBackgroundDrawableId) {
            topNextNavigationPanel.findViewById(R.id.navigation_next_advice_image_distance_overlay_background)
                    .setVisibility(View.VISIBLE);
            if (nextVisualAdviceStreetName != null) {
                topNextNavigationPanel.findViewById(R.id.navigation_next_advice_street_overlay_background)
                        .setVisibility(View.VISIBLE);
            } else {
                topNextNavigationPanel.findViewById(R.id.navigation_next_advice_street_overlay_background)
                        .setVisibility(View.GONE);
            }
        } else {
            topNextNavigationPanel.findViewById(R.id.navigation_next_advice_image_distance_overlay_background)
                    .setVisibility(View.GONE);
            topNextNavigationPanel.findViewById(R.id.navigation_next_advice_street_overlay_background).setVisibility
                    (View.GONE);
        }
    }

    /**
     * switch from estimated time to the arrived time
     */
    public void switchEstimatedTime() {
        currentActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                if (estimatedTimePanelVisible) {
                    showArrivingTime();
                } else {
                    showEstimatedTime();
                }
                estimatedTimePanelVisible = !estimatedTimePanelVisible;
            }

        });
    }

    /**
     * shows estimated time
     */
    public void showEstimatedTime() {
        hideViewIfVisible(arrivingTimePanel);
        if (estimatedTimeText != null) {
            estimatedTimeText.setText(SKToolsUtils.formatTime(timeToDestination));
        }
        showViewIfNotVisible(estimatedTimePanel);
    }

    /**
     * calculates and shows the arriving time
     */
    public void showArrivingTime() {
        Calendar currentTime = Calendar.getInstance();
        Calendar arrivingTime = (Calendar) currentTime.clone();
        int hours = timeToDestination / 3600;
        int minutes = (timeToDestination % 3600) / 60;
        arrivingTime.add(Calendar.MINUTE, minutes);
        TextView arrivingTimeAMPM = (TextView) arrivingTimePanel.findViewById(R.id.arriving_time_text_ampm);
        if (arrivingTimeText != null) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            arrivingTime.add(Calendar.HOUR_OF_DAY, hours);
            arrivingTimeAMPM.setText("");
            arrivingTimeText.setText(simpleDateFormat.format(arrivingTime.getTime()));
        }
        hideViewIfVisible(estimatedTimePanel);
        showViewIfNotVisible(arrivingTimePanel);
    }

    /**
     * Checks if the navigation is in free drive mode.
     *
     * @return
     */
    public boolean isFreeDriveMode() {
        return isFreeDrive;
    }

    /**
     * Sets the free drive mode.
     */
    public void setFreeDriveMode() {
        firstAdviceReceived = false;
        isFreeDrive = true;

        hideViewIfVisible(routeDistancePanel);
        hideViewIfVisible(arrivingETATimeGroupPanels);

        boolean isLandscape = currentActivity.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        setSettingsMenuForFreeDrive(isLandscape);
        cancelSpeedExceededThread();
    }

    /**
     * sets speed limit field and visibility
     *
     * @param countryCode
     * @param distanceUnitType
     */
    public void handleSpeedLimitAvailable(final String countryCode, SKMaps.SKDistanceUnitType distanceUnitType,
                                          int mapStyle) {

        if (speedPanel == null) {
            return;
        }

        TextView speedLimitText = (TextView) speedPanel.findViewById(R.id.speed_limit_value);
        ImageView speedLimitImage = (ImageView) speedPanel.findViewById(R.id.navigation_speed_sign_image);

        if (countryCode != null) {
            isUS = countryCode.equals("US");
            if (isUS) {
                isDefaultSpeedSign = false;
                if (speedExceededThread == null) {
                    if (speedLimitImage != null) {
                        speedLimitImage.setImageResource(R.drawable
                                .background_speed_sign_us);
                    }
                }
            } else {
                if (!isDefaultSpeedSign) {
                    if (speedLimitImage != null) {
                        speedLimitImage.setImageResource(R.drawable
                                .background_speed_sign);
                    }
                }
                isDefaultSpeedSign = true;
            }
        }

        // speed limit visibility
        if (currentNavigationMode == NavigationMode.FOLLOWER) {
            if (currentSpeedLimit != 0)//&& gpsIsWorking) {
                if (!speedLimitAvailable) {
                    currentActivity.findViewById(R.id.navigation_free_drive_speed_limit_panel).setVisibility(View
                            .VISIBLE);
                }
        }

        // set speed limit
        if (currentSpeedLimit != 0) {
            if (speedLimitText != null) {
                speedLimitText.setText(String.valueOf(SKToolsUtils
                        .getSpeedByUnit(currentSpeedLimit, distanceUnitType)));
                if (!speedLimitExceeded) {
                    speedLimitText.setVisibility(View.VISIBLE);
                    speedLimitImage.setVisibility(View.VISIBLE);
                }
            }
            if (!speedLimitAvailable) {
                setCurrentSpeedPanelBackgroundAndTextColour(mapStyle);
            }
            speedLimitAvailable = true;
        } else {
            if (speedLimitAvailable) {
                currentActivity.findViewById(R.id.navigation_free_drive_speed_limit_panel).setVisibility(View.GONE);

                setCurrentSpeedPanelBackgroundAndTextColour(mapStyle);

            }
            speedLimitAvailable = false;
        }

    }

    /**
     * cancels the thread that deals with the speed exceeded flow
     */
    private void cancelSpeedExceededThread() {
        if (speedExceededThread != null && speedExceededThread.isAlive()) {
            speedExceededThread.cancel();
        }
    }

    /**
     * sets current speed panel background color when speed limit is available.
     *
     * @param currentMapStyle
     */
    public void setCurrentSpeedPanelBackgroundAndTextColour(int currentMapStyle) {
        RelativeLayout currentSpeedPanel = (RelativeLayout) currentActivity.findViewById
                (R.id.navigation_free_drive_speed_limit_panel);
        if (currentMapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
            if (currentSpeedPanel != null) {
                currentSpeedPanel.setBackgroundColor(currentActivity.getResources().getColor(R.color
                        .gray));
            }
            currentActivity.findViewById(R.id.free_drive_current_speed_linear_layout).setBackgroundColor
                    (currentActivity.getResources().getColor(R.color.gray));
            if (currentSpeedText != null) {
                currentSpeedText.setTextColor(currentActivity.getResources().getColor(R.color.black));
            }
            if (currentSpeedTextValue != null) {
                currentSpeedTextValue.setTextColor(currentActivity.getResources().getColor(R.color
                        .black));
            }
        } else if (currentMapStyle == SKToolsMapOperationsManager.NIGHT_STYLE) {
            if (currentSpeedPanel != null) {
                currentSpeedPanel.setBackgroundColor(currentActivity.getResources().getColor(R.color
                        .speed_panel_night_background));
            }
            currentActivity.findViewById(R.id.free_drive_current_speed_linear_layout).setBackgroundColor
                    (currentActivity.getResources().getColor(R.color.speed_panel_night_background));
            if (currentSpeedText != null) {
                currentSpeedText.setTextColor(currentActivity.getResources().getColor(R.color
                        .gray));
            }
            if (currentSpeedTextValue != null) {
                currentSpeedTextValue.setTextColor(currentActivity.getResources().getColor(R.color
                        .gray));
            }
        }
    }

    /**
     * Handles the free drive updates.
     *
     * @param countryCode
     * @param streetName
     * @param currentFreeDriveSpeed
     * @param speedLimit
     * @param distanceUnitType
     * @param mapStyle
     */
    public void handleFreeDriveUpdated(final String countryCode, final String streetName,
                                       final double currentFreeDriveSpeed, final double speedLimit,
                                       final SKMaps.SKDistanceUnitType distanceUnitType,
                                       final int mapStyle) {
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (isFreeDrive) {

                    if (currentSpeed == 0 || currentSpeed != currentFreeDriveSpeed) {
                        currentSpeed = currentFreeDriveSpeed;
                        currentSpeedString = String.valueOf(SKToolsUtils.getSpeedByUnit(currentSpeed,
                                distanceUnitType));
                        currentSpeedText.setText(currentSpeedString);
                        currentSpeedTextValue.setText(SKToolsUtils.getSpeedTextByUnit(currentActivity,
                                distanceUnitType));
                    }

                    if (currentSpeedLimit != speedLimit) {
                        currentSpeedLimit = speedLimit;
                        handleSpeedLimitAvailable(countryCode, distanceUnitType, mapStyle);
                    }

                    setTopPanelsBackgroundColour(mapStyle, false, false);

                    if (streetName != null && !streetName.equals("")) {
                        currentStreetNameFreeDriveString = streetName;
                        ((TextView) freeDriveCurrentStreetPanel.findViewById(R.id.free_drive_current_street_text))
                                .setText(streetName);
                    } else {
                        currentStreetNameFreeDriveString = null;
                    }
                    if (currentNavigationMode == NavigationMode.FOLLOWER && currentStreetNameFreeDriveString != null) {
                        freeDriveCurrentStreetPanel.setVisibility(View.VISIBLE);
                        if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
                            showViewIfNotVisible(compassViewPanel);
                        } else {
                            showViewIfNotVisible(speedPanel);
                        }
                    }
                }

            }
        });
    }

    /**
     * Handles orientation change.
     *
     * @param mapStyle
     * @param displayMode
     */
    public void handleOrientationChanged(final int mapStyle, final SKMapSettings.SKMapDisplayMode displayMode) {
        if (currentNavigationMode != NavigationMode.PRE_NAVIGATION || currentNavigationMode != NavigationMode
                .POST_NAVIGATION) {
            currentActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (settingsPanel != null) {
                        rootLayout.removeView(settingsPanel);
                    }
                    rootLayout.removeView(speedPanel);
                    rootLayout.removeView(routeDistancePanel);
                    rootLayout.removeView(arrivingETATimeGroupPanels);
                    rootLayout.removeView(compassViewPanel);

                    inflateSettingsMenu();
                    initialiseVolumeSeekBar();
                    inflateBottomPanels();

                    setAudioViewsFromSettings();
                    switchMapMode(displayMode);
                    switchDayNightStyle(mapStyle);

                    boolean isLandscape = currentActivity.getResources().getConfiguration().orientation ==
                            Configuration.ORIENTATION_LANDSCAPE;
                    if (isFreeDrive) {
                        setSettingsMenuForFreeDrive(isLandscape);
                    }

                    if (currentNavigationMode == NavigationMode.SETTINGS) {
                        showViewIfNotVisible(settingsPanel);
                    } else if (currentNavigationMode == NavigationMode.FOLLOWER) {
                        if (routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN) {
                            showViewIfNotVisible(compassViewPanel);
                        } else {
                            showViewIfNotVisible(speedPanel);
                        }
                        if (!isFreeDrive) {
                            showViewIfNotVisible(routeDistancePanel);
                            showViewIfNotVisible(arrivingETATimeGroupPanels);
                        }
                    }

                    setAdvicesFields();
                    changePanelsBackgroundAndTextViewsColour(mapStyle);

                }
            });
        }
    }

    /**
     * Handles the speed exceeded.
     *
     * @param speedExceeded
     */
    public void handleSpeedExceeded(boolean speedExceeded) {
        speedLimitExceeded = speedExceeded;
        if (currentNavigationMode == NavigationMode.FOLLOWER) {
            changeSpeedSigns();
        }
    }


    /**
     * sets the current and next advice panels dimensions
     */
    private void setAdvicesFields() {
        if (!isFreeDrive) {
            setRouteDistanceFields();
            setETAFields();
        } else {
            TextView freeDriveCurrentStreetText =
                    (TextView) freeDriveCurrentStreetPanel.findViewById(R.id.free_drive_current_street_text);
            if (freeDriveCurrentStreetText != null) {
                freeDriveCurrentStreetText.setText(currentStreetNameFreeDriveString);
            }
        }
        setCurrentSpeedFields();
        setSpeedLimitFields();
    }

    /**
     * set speed limit fields
     */
    private void setSpeedLimitFields() {
        TextView speedLimitText = (TextView) speedPanel.findViewById(R.id.speed_limit_value);
        if (speedLimitAvailable && speedLimitText != null && currentSpeedLimit != 0) {
            speedLimitText.setText(String.valueOf(String.valueOf(SKToolsUtils
                    .getSpeedByUnit(currentSpeedLimit, distanceUnitType))));
            changeSpeedSigns();

            currentActivity.findViewById(R.id.navigation_free_drive_speed_limit_panel).setVisibility(View.VISIBLE);
        }
    }

    /**
     * changes the speed exceeded sign
     */
    private void changeSpeedSigns() {
        TextView speedLimitText = (TextView) speedPanel.findViewById(R.id.speed_limit_value);
        ImageView speedLimitImage = (ImageView) speedPanel.findViewById(R.id.navigation_speed_sign_image);

        if (speedLimitExceeded) {
            if (speedExceededThread == null || !speedExceededThread.isAlive()) {
                speedExceededThread = new SpeedExceededThread(true);
                speedExceededThread.start();
            }
        } else {
            if (speedExceededThread != null) {
                if (speedLimitImage != null) {
                    if (isUS) {
                        speedLimitImage.setImageResource(R.drawable.background_speed_sign_us);
                    } else {
                        speedLimitImage.setImageResource(R.drawable.background_speed_sign);
                    }
                }
                if (speedLimitText != null) {
                    speedLimitText.setVisibility(View.VISIBLE);
                }
                speedExceededThread.cancel();
                speedExceededThread = null;
            }
        }
    }

    /**
     * set current speed fields
     */
    private void setCurrentSpeedFields() {
        if (currentSpeedText != null) {
            currentSpeedText.setText(currentSpeedString);
        }
        if (currentSpeedTextValue != null) {
            currentSpeedTextValue.setText(SKToolsUtils.getSpeedTextByUnit(currentActivity, distanceUnitType));
        }
    }

    /**
     * set eta fields
     */
    private void setETAFields() {
        if (estimatedTimePanelVisible) {
            showEstimatedTime();
        } else {
            showArrivingTime();
        }
    }


    /**
     * sets route distance fields.
     */
    protected void setRouteDistanceFields() {
        if (routeDistanceText != null) {
            routeDistanceText.setText(routeDistanceString);
        }
        if (routeDistanceTextValue != null) {
            routeDistanceTextValue.setText(routeDistanceValueString);
        }
    }

    /**
     * sets next advice street name visibility
     */
    private void setNextAdviceStreetNameVisibility() {
        if (topNextNavigationPanel != null && nextAdviceStreetNameTextView != null &&
                nextAdviceStreetNamePanel != null) {
            if (nextVisualAdviceStreetName != null) {
                nextAdviceStreetNamePanel.setVisibility(View.VISIBLE);
            } else {
                nextAdviceStreetNamePanel.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Shows the next advice.
     */
    private void showNextAdvice() {
        topNextNavigationPanel.setVisibility(View.VISIBLE);
        topNextNavigationPanel.bringToFront();
    }

    /**
     * removes the next advice
     */
    private void disableNextAdvice() {
        isNextAdviceVisible = false;
        if (topNextNavigationPanel != null) {
            topNextNavigationPanel.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the via point panel for 2 seconds.
     */
    public void showViaPointPanel() {

        Runnable runnable;
        Handler handler = new Handler();

        runnable = new Runnable() {

            @Override
            public void run() {
                hideViewIfVisible(viaPointPanel);
            }
        };

        hideViewIfVisible(topCurrentNavigationPanel);
        hideViewIfVisible(topNextNavigationPanel);
        showViewIfNotVisible(viaPointPanel);

        TextView viaPointText =
                (TextView) viaPointPanel.findViewById(R.id.via_point_text_view);
        viaPointText.setText(currentActivity.getResources().getString(R.string.via_point_reached));
        handler.postDelayed(runnable, 2000);

    }

    /**
     * Initialises the volume bar.
     */
    public void initialiseVolumeSeekBar() {
        int currentVolume = SKToolsAdvicePlayer.getCurrentDeviceVolume(currentActivity);
        int maxVolume = SKToolsAdvicePlayer.getMaximAudioLevel(currentActivity);
        SeekBar volumeBar = (SeekBar) settingsPanel.findViewById(R.id
                .navigation_settings_volume);
        volumeBar.setMax(maxVolume);
        volumeBar.setProgress(currentVolume);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final AudioManager audioManager =
                        (AudioManager) currentActivity.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, progress,
                        AudioManager.FLAG_SHOW_UI);
            }
        });

    }

    /**
     * Changes audio settings menu item panels.
     */
    public void loadAudioSettings() {
        if (!(routeType == SKRouteSettings.SKRouteMode.PEDESTRIAN)) {
            TextView audioText = ((TextView) settingsPanel.findViewById(R.id
                    .navigation_settings_audio_text));
            Integer audioImageTag = (Integer) audioText.getTag();
            audioImageTag = audioImageTag == null ? 0 : audioImageTag;

            Resources res = currentActivity.getResources();
            if (audioImageTag == R.drawable.ic_audio_on) {
                SKToolsAdvicePlayer.getInstance().disableMute();
                SKToolsLogicManager.getInstance().playLastAdvice();
                audioText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_audio_off, 0, 0);
                audioText.setTag(R.drawable.ic_audio_off);
                audioText.setText(res.getString(R.string.navigate_settings_audio_off));
            } else if (audioImageTag == 0 || audioImageTag == R.drawable.ic_audio_off) {
                SKToolsAdvicePlayer.getInstance().stop();
                SKToolsAdvicePlayer.getInstance().enableMute();
                audioText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_audio_on, 0, 0);
                audioText.setTag(R.drawable.ic_audio_on);
                audioText.setText(res.getString(R.string.navigate_settings_audio_on));
            }
        }
    }

    /**
     * sets the image view and the text view for audio button in settings
     * screen, depending on the progress value of the volume bar (set
     * previously) and the current device volume.
     */
    protected void setAudioViewsFromSettings() {
        if (settingsPanel != null) {
            if (SKToolsAdvicePlayer.getInstance().isMuted()) {
                TextView audioText = ((TextView) settingsPanel.findViewById(R.id.navigation_settings_audio_text));
                audioText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_audio_on, 0, 0);
                audioText.setTag(R.drawable.ic_audio_on);
                audioText.setText(currentActivity.getResources().getString(R.string.navigate_settings_audio_on));
            } else {
                TextView audioText = ((TextView) settingsPanel.findViewById(R.id.navigation_settings_audio_text));
                audioText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_audio_off, 0, 0);
                audioText.setTag(R.drawable.ic_audio_off);
                audioText.setText(currentActivity.getResources().getString(R.string.navigate_settings_audio_off));
            }
        }
    }

    /**
     * Changes settings menu item, map style panels.
     *
     * @param mapStyle
     */
    public void switchDayNightStyle(int mapStyle) {
        if (settingsPanel != null) {

            TextView dayNightText = ((TextView) settingsPanel.findViewById(R.id
                    .navigation_settings_day_night_mode_text));
            if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
                dayNightText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_nightmode, 0, 0);
                dayNightText.setText(currentActivity.getResources().getString(R.string
                        .navigate_settings_nightmode));
            } else {
                dayNightText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_daymode, 0, 0);
                dayNightText.setText(currentActivity.getResources().getString(R.string
                        .navigate_settings_daymode));
            }

            isDefaultTopPanelBackgroundColor = false;
            changePanelsBackgroundAndTextViewsColour(mapStyle);
            setTopPanelsBackgroundColour(mapStyle, false, false);

        }
    }

    /**
     * Changes settings menu item, map mode panels.
     *
     * @param displayMode
     */
    public void switchMapMode(SKMapSettings.SKMapDisplayMode displayMode) {
        TextView dayNightText = ((TextView) settingsPanel.findViewById(R.id
                .navigation_settings_view_mode_text));
        if (displayMode == SKMapSettings.SKMapDisplayMode.MODE_3D) {
            dayNightText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_2d, 0, 0);
            dayNightText.setText(currentActivity.getResources().getString(R.string
                    .navigate_settings_2d_view));
        } else {
            dayNightText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_3d, 0, 0);
            dayNightText.setText(currentActivity.getResources().getString(R.string
                    .navigate_settings_3d_view));
        }
    }

    /**
     * sets the background drawable for all views
     *
     * @param currentMapStyle
     */
    public void changePanelsBackgroundAndTextViewsColour(int currentMapStyle) {

        if (currentNavigationMode == NavigationMode.PRE_NAVIGATION) {
            setPanelBackgroundAndTextColour(preNavigationPanel.findViewById(R.id.alternative_routes_layout), null,
                    currentMapStyle);
            setPanelBackgroundAndTextColour(preNavigationPanel.findViewById(R.id.start_navigation_button), null,
                    currentMapStyle);
        } else {

            if (!isFreeDrive) {
                setEtaTimeGroupPanelsBackgroundAndTextViewColour(currentMapStyle);

                setPanelBackgroundAndTextColour(routeDistancePanel.findViewById(R.id.route_distance_linear_layout),
                        routeDistanceText, currentMapStyle);
                setPanelBackgroundAndTextColour(null, routeDistanceTextValue, currentMapStyle);
            } else {
                setPanelBackgroundAndTextColour(freeDriveCurrentStreetPanel, null, currentMapStyle);
            }

            setPanelBackgroundAndTextColour(speedPanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(compassViewPanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(topCurrentNavigationPanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(nextAdviceStreetNamePanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(nextAdviceImageDistancePanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(backButtonPanel.findViewById(R.id.navigation_top_back_button), null,
                    currentMapStyle);
            setPanelBackgroundAndTextColour(roadBlockPanel.findViewById(R.id.road_block_relative_layout), null,
                    currentMapStyle);

            ViewGroup routeOverviewPanel = (ViewGroup) currentActivity.findViewById(R.id
                    .navigation_route_overview_linear_layout);
            setPanelBackgroundAndTextColour(routeOverviewPanel, null, currentMapStyle);
            setPanelBackgroundAndTextColour(viaPointPanel, null, currentMapStyle);

            setCurrentSpeedPanelBackgroundAndTextColour(currentMapStyle);
            setSettingsPanelBackground(currentMapStyle);
        }
    }

    /**
     * Sets the settings panel background depending on a map style.
     *
     * @param currentMapStyle
     */
    private void setSettingsPanelBackground(int currentMapStyle) {
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_audio_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_day_night_mode_button),
                null, currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_overview_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_route_info_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_roadblock_info_button),
                null, currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_panning_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_view_mode_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_quit_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_back_button), null,
                currentMapStyle);
        setPanelBackgroundAndTextColour(settingsPanel.findViewById(R.id.navigation_settings_seek_bar_layout), null,
                currentMapStyle);
    }


    /**
     * sets the background and text view colours for eta group panels
     *
     * @param currentMapStyle
     */
    public void setEtaTimeGroupPanelsBackgroundAndTextViewColour(int currentMapStyle) {
        TextView arrivingTimeAmPm = ((TextView) arrivingTimePanel.findViewById(R.id.arriving_time_text_ampm));
        TextView estimatedTimeTextValue = ((TextView) estimatedTimePanel.findViewById(R.id
                .estimated_navigation_time_text_value));

        setPanelBackgroundAndTextColour(arrivingTimePanel, arrivingTimeText,
                currentMapStyle);
        setPanelBackgroundAndTextColour(null, arrivingTimeAmPm, currentMapStyle);
        arrivingTimeAmPm.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bullet_gray, 0, R.drawable.bullet_green, 0);

        setPanelBackgroundAndTextColour(estimatedTimePanel, estimatedTimeText, currentMapStyle);
        setPanelBackgroundAndTextColour(null, estimatedTimeTextValue, currentMapStyle);
        estimatedTimeTextValue.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bullet_green, 0,
                R.drawable.bullet_gray, 0);
    }

    /**
     * sets the background for top panel and the text color for the texts inside it
     */
    private void setPanelBackgroundAndTextColour(View panel, TextView textView, int currentMapStyle) {

        if (currentMapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
            if (panel != null) {
                panel.setBackgroundColor(currentActivity.getResources().getColor(R.color
                        .navigation_style_day));
            }
            if (textView != null) {
                textView.setTextColor(currentActivity.getResources().getColor(R.color.white));
            }
        } else if (currentMapStyle == SKToolsMapOperationsManager.NIGHT_STYLE) {
            if (panel != null) {
                panel.setBackgroundColor(currentActivity.getResources().getColor(R.color
                        .navigation_style_night));
            }
            if (textView != null) {
                textView.setTextColor(currentActivity.getResources().getColor(R.color
                        .gray));
            }
        }

    }


    /**
     * removes views for pre navigation
     */
    protected void removePreNavigationViews() {
        if (preNavigationPanel != null) {
            rootLayout.removeView(preNavigationPanel);
            preNavigationPanel = null;
        }
    }

    /**
     * remove views with different UI for portrait and landscape
     */
    protected void removeNavigationViews() {
        cancelSpeedExceededThread();
        isFreeDrive = false;

        if (topCurrentNavigationPanel != null) {
            rootLayout.removeView(topCurrentNavigationPanel);
            topCurrentNavigationPanel = null;
        }
        if (topNextNavigationPanel != null) {
            rootLayout.removeView(topNextNavigationPanel);
            topNextNavigationPanel = null;
        }
        if (menuOptions != null) {
            rootLayout.removeView(menuOptions);
            menuOptions = null;
        }
        if (routeDistancePanel != null) {
            rootLayout.removeView(routeDistancePanel);
            routeDistancePanel = null;
        }
        if (speedPanel != null) {
            rootLayout.removeView(speedPanel);
            speedLimitAvailable = false;
            speedPanel = null;
        }
        if (compassViewPanel != null) {
            rootLayout.removeView(compassViewPanel);
            compassViewPanel = null;
        }
        if (arrivingETATimeGroupPanels != null) {
            rootLayout.removeView(arrivingETATimeGroupPanels);
            arrivingETATimeGroupPanels = null;
        }
        if (reRoutingPanel != null) {
            rootLayout.removeView(reRoutingPanel);
            reRoutingPanel = null;
        }
        if (freeDriveCurrentStreetPanel != null) {
            rootLayout.removeView(freeDriveCurrentStreetPanel);
            freeDriveCurrentStreetPanel = null;
        }
        if (settingsPanel != null) {
            rootLayout.removeView(settingsPanel);
            settingsPanel = null;
        }
        if (navigationSimulationPanel != null) {
            rootLayout.removeView(navigationSimulationPanel);
            navigationSimulationPanel = null;
        }
        if (viaPointPanel != null) {
            rootLayout.removeView(viaPointPanel);
            viaPointPanel = null;
        }
        if (routeOverviewPanel != null) {
            rootLayout.removeView(routeOverviewPanel);
            routeOverviewPanel = null;
        }
        if (roadBlockPanel != null) {
            rootLayout.removeView(roadBlockPanel);
            roadBlockPanel = null;
        }
        if (backButtonPanel != null) {
            rootLayout.removeView(backButtonPanel);
            backButtonPanel = null;
        }
    }


    /**
     * speed exceeded thread
     */
    private class SpeedExceededThread extends Thread {

        private boolean speedExceeded;

        public SpeedExceededThread(boolean speedExceeded) {
            this.speedExceeded = speedExceeded;
        }

        public void run() {

            final ImageView speedLimitImage = (ImageView) speedPanel.findViewById(R.id.navigation_speed_sign_image);
            final ImageView speedAlertImage = (ImageView) speedPanel.findViewById(R.id
                    .navigation_alert_sign_image);
            final TextView speedLimitText = (TextView) speedPanel.findViewById(R.id.speed_limit_value);

            while (speedExceeded) {
                currentActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (speedLimitText != null) {
                            speedLimitText.setVisibility(View.GONE);
                        }
                        if (speedAlertImage != null) {
                            speedAlertImage.setVisibility(View.VISIBLE);
                        }
                        if (speedLimitImage != null) {
                            if (isUS) {
                                speedLimitImage.setImageResource(R.drawable.background_alert_sign_us);
                            } else {
                                speedLimitImage.setImageResource(R.drawable.background_alert_sign);
                            }
                            speedLimitImage.setBackgroundDrawable(null);

                            Animation fadeOut = new AlphaAnimation(1, 0);
                            fadeOut.setInterpolator(new AccelerateInterpolator());
                            fadeOut.setDuration(800);
                            speedLimitImage.setAnimation(fadeOut);
                            speedLimitImage.clearAnimation();
                        }

                    }
                });
                synchronized (this) {
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                currentActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (speedLimitImage != null) {
                            if (isUS) {
                                speedLimitImage.setImageResource(R.drawable.background_speed_sign_us);
                            } else {
                                speedLimitImage.setImageResource(R.drawable.background_speed_sign);
                            }
                            Animation fadeIn = new AlphaAnimation(0, 1);
                            fadeIn.setInterpolator(new DecelerateInterpolator());
                            fadeIn.setDuration(800);
                            speedLimitImage.setAnimation(fadeIn);
                            speedLimitImage.clearAnimation();
                        }
                        if (speedAlertImage != null) {
                            speedAlertImage.setVisibility(View.GONE);
                        }
                        if (speedLimitText != null) {
                            speedLimitText.setVisibility(View.VISIBLE);
                        }

                    }
                });
                synchronized (this) {
                    try {
                        wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * cancels the thread
         */
        public void cancel() {
            this.speedExceeded = false;
        }
    }

}
