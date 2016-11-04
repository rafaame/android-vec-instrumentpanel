package me.rafaa.vecinstrumentpanel.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.inject.Inject;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationConfiguration;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationListener;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationManager;

import java.util.List;

import me.rafaa.vecinstrumentpanel.R;
import me.rafaa.vecinstrumentpanel.io.OBDPacket;
import me.rafaa.vecinstrumentpanel.io.ObdGatewayService;
import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

import static me.rafaa.vecinstrumentpanel.activity.ConfigActivity.getGpsDistanceUpdatePeriod;
import static me.rafaa.vecinstrumentpanel.activity.ConfigActivity.getGpsUpdatePeriod;

@ContentView(R.layout.activity_dashboard)
public class DashboardActivity extends RoboFragmentActivity implements
        LocationListener,
        GpsStatus.Listener,
        SKToolsNavigationListener,
        SKMapSurfaceListener {

    private static final String TAG = DashboardActivity.class.getName();

    @InjectView(R.id.main_content)
    private View mainContentView;

    @InjectView(R.id.panel_left)
    private View panelLeftView;

    @InjectView(R.id.panel_right)
    private View panelRightView;

    @InjectView(R.id.panel_status)
    private View panelStatusView;

    @InjectView(R.id.menu_left)
    private View leftMenuView;

    @InjectView(R.id.menu_button_settings)
    private View menuButtonSettingsView;

    @InjectView(R.id.menu_button_console)
    private View menuButtonConsoleView;

    @InjectView(R.id.status_wifi)
    private ImageView iconStatusWifiView;

    @InjectView(R.id.indicator_obd_latency)
    private TextView indicatorObdLatency;

    @InjectView(R.id.indicator_speed)
    private TextView indicatorSpeedView;

    @InjectView(R.id.indicator_rpm)
    private TextView indicatorRpmView;

    @InjectView(R.id.chart_efficiency)
    private LineChart chartEfficiency;

    //private SKMapFragment mapFragment = null;
    private SKMapSurfaceView mapView;
    private SKMapViewHolder mapHolder;

    private boolean isObdServiceBound;
    private PowerManager.WakeLock wakeLock = null;
    private ObdGatewayService service;

    boolean mGpsIsStarted = false;
    private LocationManager locationService;
    private LocationProvider mLocProvider;
    private Location mLastLocation;

    @Inject
    private SensorManager sensorManager;

    @Inject
    private PowerManager powerManager;

    @Inject
    private SharedPreferences prefs;

    private Sensor orientationSensor = null;
    private final SensorEventListener orientationListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            String orientation = "";

            if (x >= 337.5 || x < 22.5)
                orientation = "N";
            else if (x >= 22.5 && x < 67.5)
                orientation = "NE";
            else if (x >= 67.5 && x < 112.5)
                orientation = "E";
            else if (x >= 112.5 && x < 157.5)
                orientation = "SE";
            else if (x >= 157.5 && x < 202.5)
                orientation = "S";
            else if (x >= 202.5 && x < 247.5)
                orientation = "SW";
            else if (x >= 247.5 && x < 292.5)
                orientation = "W";
            else if (x >= 292.5 && x < 337.5)
                orientation = "NW";

            //updateTextView(compass, dir);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            isObdServiceBound = true;

            service = ((ObdGatewayService.ObdGatewayServiceBinder) binder).getService();
            service.setContext(DashboardActivity.this);
            service.startService();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isObdServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            isObdServiceBound = false;
        }
    };

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private boolean isFullscreen;
    private final Handler fullscreenHandler = new Handler();
    private final Runnable enableFullscreenRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mainContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable disableFullscreenRunnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            //ActionBar actionBar = getSupportActionBar();
            //if(actionBar != null)
            //    actionBar.enableFullscreen();

            leftMenuView.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable delayedFullscreenRunnable = new Runnable() {
        @Override
        public void run() {
            enableFullscreen();
        }
    };

    private final View.OnTouchListener mDelayFullscreenListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedFullscreen(AUTO_HIDE_DELAY_MILLIS);
            }

            return false;
        }
    };

    private void initOrientationSensor() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0) {
            orientationSensor = sensors.get(0);
            sensorManager.registerListener(orientationListener, orientationSensor, SensorManager.SENSOR_DELAY_UI);

            Log.d(TAG, "Orientation sensor started.");
        } else {
            Log.d(TAG, "Unable to init orientation sensor. Car orientation will not work.");
        }
    }

    private void initViews() {
        isFullscreen = false;

        panelRightView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullscreen();
            }
        });

        menuButtonSettingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchConfigActivity();
            }
        });

        leftMenuView.setOnTouchListener(mDelayFullscreenListener);

        initCharts();
    }

    private void initLineChart(LineChart chart) {
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(ContextCompat.getColor(this, R.color.black_overlay));
        chart.setDescription(null);

        LineData data = new LineData();
        data.setDrawValues(false);

        chart.setData(data);

        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaxValue(100f);
        leftAxis.setAxisMinValue(0f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setEnabled(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setLineWidth(2f);
        set.setColor(ContextCompat.getColor(this, R.color.chart_orange_line));
        //set.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_fill_background));

        data.addDataSet(set);
    }

    private void initEfficiencyChart() {
        initLineChart(chartEfficiency);
    }

    private void initCharts() {
        initEfficiencyChart();
    }

    private void addEntry() {

        LineData data = chartEfficiency.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);

            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 40) + 30f), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chartEfficiency.notifyDataSetChanged();

            // limit the number of visible entries
            chartEfficiency.setVisibleXRangeMaximum(120);
            // chartEfficiency.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chartEfficiency.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart1.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private void initMapFragment() {
        mapHolder = (SKMapViewHolder) findViewById(R.id.mapFragment);
        mapHolder.setMapSurfaceListener(DashboardActivity.this);
    }

    private void wakeLock() {
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");
    }

    private void updateSystemStatusIcons() {
        updateWifiStatusIcon(service != null && service.isConnected());
    }

    private void init() {
        initViews();
        initOrientationSensor();
        initGps();

        initMapFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        init();
        delayedFullscreen(100);
    }

    protected void onResume() {
        super.onResume();
        mapHolder.onResume();
        Log.d(TAG, "Resuming..");

        wakeLock();
        updateSystemStatusIcons();

        startLiveData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapHolder.onPause();
        Log.d(TAG, "Pausing..");

        releaseWakeLock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationService != null) {
            locationService.removeGpsStatusListener(this);

            try {
                locationService.removeUpdates(this);
            } catch (SecurityException e) {
                throw e;
            }
        }

        releaseWakeLock();

        if (isObdServiceBound) {
            unbindObdService();
        }
    }

    private void launchConfigActivity() {
        startActivity(new Intent(this, ConfigActivity.class));
    }

    private void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen) {
            disableFullscreen();
        } else {
            enableFullscreen();
        }
    }

    private void disableFullscreen() {
        // Show the system bar
        mainContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        isFullscreen = false;

        // Schedule a runnable to display UI elements after a delay
        fullscreenHandler.removeCallbacks(enableFullscreenRunnable);
        fullscreenHandler.postDelayed(disableFullscreenRunnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void enableFullscreen() {
        leftMenuView.setVisibility(View.GONE);
        isFullscreen = true;

        // Schedule a runnable to remove the status and navigation bar after a delay
        fullscreenHandler.removeCallbacks(disableFullscreenRunnable);
        fullscreenHandler.postDelayed(enableFullscreenRunnable, UI_ANIMATION_DELAY);
    }

    private void delayedFullscreen(int delayMillis) {
        fullscreenHandler.removeCallbacks(delayedFullscreenRunnable);
        fullscreenHandler.postDelayed(delayedFullscreenRunnable, delayMillis);
    }

    private void doBindService() {
        if (!isObdServiceBound) {
            Log.d(TAG, "Binding OBD service..");

            Intent serviceIntent = new Intent(this, ObdGatewayService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindObdService() {
        if (isObdServiceBound) {
            if (service.isConnected()) {
                service.stopService();
            }

            Log.d(TAG, "Unbinding OBD service..");

            unbindService(serviceConnection);
            isObdServiceBound = false;

            updateWifiStatusIcon(false);
        }
    }

    private void startLiveData() {
        Log.d(TAG, "Starting live data..");

        doBindService();
        gpsStart();
        wakeLock.acquire();
    }

    private void stopLiveData() {
        Log.d(TAG, "Stopping live data..");

        gpsStop();
        unbindObdService();
        releaseWakeLock();
    }

    public void notifyObdPacket(OBDPacket packet) {
        addEntry();
        updateWifiStatusIcon(service.isConnected());

        updateObdLatencyIndicator(packet.latency);
        updateEngineRpmIndicator(packet.engineRpm);
        updateSpeedIndicator(packet.speed);
    }

    private void updateObdLatencyIndicator(Integer value) {
        if (value > 0) {
            indicatorObdLatency.setText(String.valueOf(value) + "ms");
        } else {
            indicatorObdLatency.setText("");
        }
    }

    private void updateEngineRpmIndicator(Integer value) {
        if (value > 0) {
            indicatorRpmView.setText(String.valueOf(value / 1000.0).substring(0, 3));
        } else {
            indicatorRpmView.setText("-");
        }
    }

    private void updateSpeedIndicator(Integer value) {
        if (value > 0) {
            indicatorSpeedView.setText(String.valueOf(value));
        } else {
            indicatorSpeedView.setText("-");
        }
    }

    private boolean initGps() {
        locationService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationService != null) {
            mLocProvider = locationService.getProvider(LocationManager.GPS_PROVIDER);
            if (mLocProvider != null) {
                try {
                    locationService.addGpsStatusListener(this);
                    if (locationService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        //gpsStatusTextView.setText(getString(R.string.status_gps_ready));
                        return true;
                    }
                } catch (SecurityException e) {
                    throw e;
                }
            }
        }

        Log.e(TAG, "Unable to get GPS PROVIDER");
        return false;
    }

    private synchronized void gpsStart() {
        try {
            if (!mGpsIsStarted && mLocProvider != null && locationService != null && locationService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationService.requestLocationUpdates(mLocProvider.getName(), getGpsUpdatePeriod(prefs), getGpsDistanceUpdatePeriod(prefs), this);
                mGpsIsStarted = true;
            } else {
                //gpsStatusTextView.setText(getString(R.string.status_gps_no_support));
            }
        } catch (SecurityException e) {
            throw e;
        }
    }

    private synchronized void gpsStop() {
        try {
            if (mGpsIsStarted) {
                locationService.removeUpdates(this);
                mGpsIsStarted = false;
            }
        } catch (SecurityException e) {
            throw e;
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

    private void updateWifiStatusIcon(boolean status) {
        int src = status ? R.drawable.icon_wifi_on : R.drawable.icon_wifi_off;
        iconStatusWifiView.setImageResource(src);

        if (!status) {
            updateObdLatencyIndicator(0);
        }
    }

    @Override
    public void onActionPan() {

    }

    @Override
    public void onActionZoom() {

    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder skMapViewHolder) {
        final String path = getFilesDir().getPath() + "/maps/";

        SKToolsNavigationConfiguration configuration = new SKToolsNavigationConfiguration();
        configuration.setDistanceUnitType(SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS);
        configuration.setSpeedWarningThresholdInCity(5.0);
        configuration.setSpeedWarningThresholdOutsideCity(5.0);
        configuration.setAutomaticDayNight(false);
        configuration.setNavigationType(SKNavigationSettings.SKNavigationType.REAL);
        configuration.setDayStyle(new SKMapViewStyle(path + ".DayStyle/", "daystyle.json"));
        configuration.setNightStyle(new SKMapViewStyle(path + ".NightStyle/", "nightstyle.json"));

        SKToolsNavigationManager navigationManager = new SKToolsNavigationManager(this, R.id.panel_left);
        navigationManager.setNavigationListener(this);
        navigationManager.startFreeDriveWithConfiguration(configuration, mapHolder);
    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onDoubleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onRotateMap() {

    }

    @Override
    public void onLongPress(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onInternetConnectionNeeded() {

    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {

    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {

    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {

    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI) {

    }

    @Override
    public void onCompassSelected() {

    }

    @Override
    public void onCurrentPositionSelected() {

    }

    @Override
    public void onObjectSelected(int i) {

    }

    @Override
    public void onInternationalisationCalled(int i) {

    }

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    @Override
    public void onNavigationStarted() {

    }

    @Override
    public void onNavigationEnded() {

    }

    @Override
    public void onRouteCalculationStarted() {

    }

    @Override
    public void onRouteCalculationCompleted() {

    }

    @Override
    public void onRouteCalculationCanceled() {

    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Dashboard Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }
}
