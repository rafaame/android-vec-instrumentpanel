package me.rafaa.vecinstrumentpanel.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.google.common.io.ByteStreams;
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
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationConfiguration;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationListener;
import com.skobbler.ngx.sdktools.navigationui.SKToolsNavigationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import me.rafaa.vecinstrumentpanel.R;
import me.rafaa.vecinstrumentpanel.config.ObdConfig;
import me.rafaa.vecinstrumentpanel.io.AbstractGatewayService;
import me.rafaa.vecinstrumentpanel.io.MockObdGatewayService;
import me.rafaa.vecinstrumentpanel.io.ObdCommandJob;
import me.rafaa.vecinstrumentpanel.io.ObdGatewayService;
import me.rafaa.vecinstrumentpanel.io.ObdProgressListener;
import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

import static me.rafaa.vecinstrumentpanel.activity.ConfigActivity.getGpsDistanceUpdatePeriod;
import static me.rafaa.vecinstrumentpanel.activity.ConfigActivity.getGpsUpdatePeriod;

@ContentView(R.layout.activity_dashboard)
public class DashboardActivity extends RoboFragmentActivity implements ObdProgressListener, LocationListener, GpsStatus.Listener, SKToolsNavigationListener, SKMapSurfaceListener
{

    private static final String TAG = "DashboardActivity";

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

    @InjectView(R.id.status_bluetooth)
    private ImageView iconStatusBluetoothView;

    @InjectView(R.id.status_obd)
    private ImageView iconStatusObdView;

    //private SKMapFragment mapFragment = null;
	private SKMapSurfaceView mapView;
	private SKMapViewHolder mapHolder;

    private boolean isObdServiceBound;
    private static boolean bluetoothDefaultIsEnable = false;
    private PowerManager.WakeLock wakeLock = null;
    private AbstractGatewayService service;

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
    private final SensorEventListener orientationListener = new SensorEventListener()
    {

        public void onSensorChanged(SensorEvent event)
        {

            float x = event.values[0];
            String orientation = "";

            if(x >= 337.5 || x < 22.5)
                orientation = "N";
            else if(x >= 22.5 && x < 67.5)
                orientation = "NE";
            else if(x >= 67.5 && x < 112.5)
                orientation = "E";
            else if(x >= 112.5 && x < 157.5)
                orientation = "SE";
            else if(x >= 157.5 && x < 202.5)
                orientation = "S";
            else if(x >= 202.5 && x < 247.5)
                orientation = "SW";
            else if(x >= 247.5 && x < 292.5)
                orientation = "W";
            else if(x >= 292.5 && x < 337.5)
                orientation = "NW";

            //updateTextView(compass, dir);

        }

        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {

            // do nothing

        }

    };

    private ServiceConnection serviceConn = new ServiceConnection()
    {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder)
        {

            Log.d(TAG, className.toString() + " service is bound");

            isObdServiceBound = true;

            service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            service.setContext(DashboardActivity.this);

            Log.d(TAG, "Starting service...");

            try
            {

                service.startService();

            }
            catch(IOException e)
            {

                Log.e(TAG, "Failure starting service...");

                unbindObdService();

            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException
        {

            return super.clone();

        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isObdServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className)
        {

            //Log.d(TAG, className.toString() + " service is unbound");
            isObdServiceBound = false;

        }

    };

    public java.util.Map<String, String> commandResult = new HashMap<String, String>();
    private final Runnable mQueueCommands = new Runnable()
    {

        public void run()
        {

            if (service != null && service.isRunning() && service.queueEmpty())
            {

                queueCommands();

                double lat = 0;
                double lon = 0;
                double alt = 0;
                final int posLen = 7;

                if(mGpsIsStarted && mLastLocation != null)
                {

                    lat = mLastLocation.getLatitude();
                    lon = mLastLocation.getLongitude();
                    alt = mLastLocation.getAltitude();

                        /*StringBuilder sb = new StringBuilder();
                        sb.append("Lat: ");
                        sb.append(String.valueOf(mLastLocation.getLatitude()).substring(0, posLen));
                        sb.append(" Lon: ");
                        sb.append(String.valueOf(mLastLocation.getLongitude()).substring(0, posLen));
                        sb.append(" Alt: ");
                        sb.append(String.valueOf(mLastLocation.getAltitude()));
                        gpsStatusTextView.setText(sb.toString());*/

                }

                    /*if (prefs.getBoolean(ConfigActivity.UPLOAD_DATA_KEY, false)) {
                        // Upload the current reading by http
                        final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                        Map<String, String> temp = new HashMap<String, String>();
                        temp.putAll(commandResult);
                        ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);
                        new UploadAsyncTask().execute(reading);

                    } else if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {
                        // Write the current reading to CSV
                        final String vin = prefs.getString(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                        Map<String, String> temp = new HashMap<String, String>();
                        temp.putAll(commandResult);
                        ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);
                        if(reading != null) myCSVWriter.writeLineCSV(reading);
                    }*/
                commandResult.clear();

            }
            // run again in period defined in preferences
            new Handler().postDelayed(mQueueCommands, ConfigActivity.getObdUpdatePeriod(prefs));

        }

    };

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private boolean isFullscreen;
    private final Handler fullscreenHandler = new Handler();
    private final Runnable enableFullscreenRunnable = new Runnable()
    {

        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {

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

    private final Runnable disableFullscreenRunnable = new Runnable()
    {

        @Override
        public void run()
        {

            // Delayed display of UI elements
            //ActionBar actionBar = getSupportActionBar();
            //if(actionBar != null)
            //    actionBar.enableFullscreen();

            leftMenuView.setVisibility(View.VISIBLE);

        }

    };

    private final Runnable delayedFullscreenRunnable = new Runnable()
    {

        @Override
        public void run()
        {

            enableFullscreen();

        }

    };

    private final View.OnTouchListener mDelayFullscreenListener = new View.OnTouchListener()
    {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {

            if(AUTO_HIDE)
                delayedFullscreen(AUTO_HIDE_DELAY_MILLIS);

            return false;

        }

    };

    private void initOrientationSensor()
    {

        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0)
        {

            orientationSensor = sensors.get(0);
            sensorManager.registerListener(orientationListener, orientationSensor, SensorManager.SENSOR_DELAY_UI);

            Log.d(TAG, "Orientation sensor started.");

        }
        else
            Log.d(TAG, "Unable to init orientation sensor. Car orientation will not work.");

    }

    private void initViews()
    {

        isFullscreen = false;

        panelRightView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view)
            {

                toggleFullscreen();

            }

        });

        menuButtonSettingsView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view)
            {

                launchConfigActivity();

            }

        });

        leftMenuView.setOnTouchListener(mDelayFullscreenListener);

    }

    private void initMapFragment()
    {

        final String path = getFilesDir().getPath() + "/vec-instrument-panel-maps/";

        /*mapFragment = (SKMapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.initialise();

		SKNavigationSettings navigationSettings = new SKNavigationSettings();
		navigationSettings.setNavigationType(SKNavigationSettings.SKNavigationType.REAL);
		navigationSettings.setNavigationMode(SKNavigationSettings.SKNavigationMode.CAR);
		navigationSettings.setCcpAsCurrentPosition(true);
		navigationSettings.setFcdEnabled(true);

		SKNavigationManager navigationManager = SKNavigationManager.getInstance();
		navigationManager.setMapView();
		navigationManager.setNavigationListener(this);
		navigationManager.startNavigation(navigationSettings);*/

		mapHolder = (SKMapViewHolder) findViewById(R.id.mapFragment);
		mapHolder.setMapSurfaceListener(DashboardActivity.this);

    }

    private void wakeLock()
    {

        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "ObdReader");

    }

    private void updateSystemStatusIcons()
    {

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        updateBluetoothStatusIcon(bluetoothAdapter != null && bluetoothAdapter.isEnabled());

    }

    private void init()
    {

        initViews();
        initOrientationSensor();
        initGps();

		initMapFragment();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);

        init();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {

        super.onPostCreate(savedInstanceState);

        delayedFullscreen(100);

    }

    protected void onResume()
    {

        super.onResume();
		mapHolder.onResume();
        Log.d(TAG, "Resuming..");

        wakeLock();
        updateSystemStatusIcons();

        startLiveData();

    }

    @Override
    protected void onPause()
    {

        super.onPause();
		mapHolder.onPause();
        Log.d(TAG, "Pausing..");

        releaseWakeLock();

    }

    @Override
    protected void onDestroy()
    {

        super.onDestroy();

        if (locationService != null)
        {

            locationService.removeGpsStatusListener(this);

            try
            {

                locationService.removeUpdates(this);

            }
            catch(SecurityException e)
            {

                throw e;

            }

        }

        releaseWakeLock();

        if (isObdServiceBound)
            unbindObdService();

    }

    private void launchConfigActivity()
    {

        startActivity(new Intent(this, ConfigActivity.class));

    }

    private void releaseWakeLock()
    {

        if(wakeLock.isHeld())
            wakeLock.release();

    }

    private void toggleFullscreen()
    {

        if (isFullscreen)
            disableFullscreen();
        else
            enableFullscreen();

    }

    private void disableFullscreen()
    {

        // Show the system bar
        mainContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        isFullscreen = false;

        // Schedule a runnable to display UI elements after a delay
        fullscreenHandler.removeCallbacks(enableFullscreenRunnable);
        fullscreenHandler.postDelayed(disableFullscreenRunnable, UI_ANIMATION_DELAY);

    }

    @SuppressLint("InlinedApi")
    private void enableFullscreen()
    {

        leftMenuView.setVisibility(View.GONE);
        isFullscreen = true;

        // Schedule a runnable to remove the status and navigation bar after a delay
        fullscreenHandler.removeCallbacks(disableFullscreenRunnable);
        fullscreenHandler.postDelayed(enableFullscreenRunnable, UI_ANIMATION_DELAY);

    }

    private void delayedFullscreen(int delayMillis)
    {

        fullscreenHandler.removeCallbacks(delayedFullscreenRunnable);
        fullscreenHandler.postDelayed(delayedFullscreenRunnable, delayMillis);

    }

    private void doBindService()
    {

        if (!isObdServiceBound)
        {

            Log.d(TAG, "Binding OBD service..");

            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if(bluetoothAdapter != null && bluetoothAdapter.isEnabled())
            {

                Log.d(TAG, "Using bluetooth...");

                //btStatusTextView.setText(getString(R.string.status_bluetooth_connecting));
                Intent serviceIntent = new Intent(this, ObdGatewayService.class);
                bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);

            }
            else
            {

                Log.d(TAG, "Using mock...");

                //btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
                Intent serviceIntent = new Intent(this, MockObdGatewayService.class);
                bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);

            }
        }
    }

    private void unbindObdService()
    {

        if(isObdServiceBound)
        {

            if(service.isRunning())
                service.stopService();

            Log.d(TAG, "Unbinding OBD service..");

            unbindService(serviceConn);
            isObdServiceBound = false;

            updateObdStatusIcon(false);

        }

    }

    public static String LookUpCommand(String txt)
    {

        for (AvailableCommandNames item : AvailableCommandNames.values())
            if (item.getValue().equals(txt))
                return item.name();

        return txt;

    }

    private void queueCommands()
    {

        if(isObdServiceBound)
        {

            for(ObdCommand Command : ObdConfig.getCommands())
            {

                if(prefs.getBoolean(Command.getName(), true))
                    service.queueJob(new ObdCommandJob(Command));

            }

        }

    }

    private void startLiveData()
    {

        Log.d(TAG, "Starting live data..");

        doBindService();
        new Handler().post(mQueueCommands);
        gpsStart();
        wakeLock.acquire();

        /*if (prefs.getBoolean(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {

            // Create the CSV Logger
            long mils = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("_dd_MM_yyyy_HH_mm_ss");

            try {
                myCSVWriter = new LogCSVWriter("Log" + sdf.format(new Date(mils)).toString() + ".csv",
                        prefs.getString(ConfigActivity.DIRECTORY_FULL_LOGGING_KEY,
                                getString(R.string.default_dirname_full_logging))
                );
            } catch (FileNotFoundException | RuntimeException e) {
                Log.e(TAG, "Can't enable logging to file.", e);
            }
        }*/
    }

    private void stopLiveData()
    {

        Log.d(TAG, "Stopping live data..");

        gpsStop();
        unbindObdService();
        releaseWakeLock();

        /*final String devemail = prefs.getString(ConfigActivity.DEV_EMAIL_KEY, null);
        if (devemail != null && !devemail.isEmpty()) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ObdGatewayService.saveLogcatToFile(getApplicationContext(), devemail);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Where there issues?\nThen please send us the logs.\nSend Logs?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).enableFullscreen();
        }

       *if (myCSVWriter != null) {
            myCSVWriter.closeLogCSVWriter();
        }*/

    }

    public void stateUpdate(final ObdCommandJob job)
    {

        final String cmdName = job.getCommand().getName();
        final String cmdID = LookUpCommand(cmdName);
        String cmdResult = "";

        if(job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR))
        {

            cmdResult = job.getCommand().getResult();

            if(cmdResult != null && isObdServiceBound)
            {

                updateObdStatusIcon(true);

            }

        }
        else if(job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED))
        {

            cmdResult = getString(R.string.status_obd_no_support);

        }
        else
        {

            cmdResult = job.getCommand().getFormattedResult();

            if(isObdServiceBound)
                updateObdStatusIcon(true);

        }

        /*if (vv.findViewWithTag(cmdID) != null) {
            TextView existingTV = (TextView) vv.findViewWithTag(cmdID);
            existingTV.setText(cmdResult);
        } else addTableRow(cmdID, cmdName, cmdResult);
        commandResult.put(cmdID, cmdResult);
        updateTripStatistic(job, cmdID);*/

    }

    private boolean initGps()
    {

        locationService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(locationService != null)
        {

            mLocProvider = locationService.getProvider(LocationManager.GPS_PROVIDER);

            if(mLocProvider != null)
            {

                try
                {

                    locationService.addGpsStatusListener(this);

                    if(locationService.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    {

                        //gpsStatusTextView.setText(getString(R.string.status_gps_ready));
                        return true;

                    }

                }
                catch(SecurityException e)
                {

                    throw e;

                }

            }

        }

        Log.e(TAG, "Unable to get GPS PROVIDER");

        return false;

    }

    private synchronized void gpsStart()
    {

        try
        {

            if (!mGpsIsStarted && mLocProvider != null && locationService != null && locationService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                locationService.requestLocationUpdates(mLocProvider.getName(), getGpsUpdatePeriod(prefs), getGpsDistanceUpdatePeriod(prefs), this);
                mGpsIsStarted = true;

            } else {

                //gpsStatusTextView.setText(getString(R.string.status_gps_no_support));

            }

        }
        catch(SecurityException e)
        {

            throw e;

        }

    }

    private synchronized void gpsStop()
    {

        try
        {

            if (mGpsIsStarted)
            {

                locationService.removeUpdates(this);
                mGpsIsStarted = false;

            }

        }
        catch(SecurityException e)
        {

            throw e;

        }

    }

    @Override
    public void onGpsStatusChanged(int event)
    {



    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {



    }

    public void onLocationChanged(Location location)
    {

        mLastLocation = location;

    }

    public void onProviderEnabled(String provider)
    {



    }

    public void onProviderDisabled(String provider)
    {



    }

    private void updateBluetoothStatusIcon(boolean status)
    {

        int src = status ? R.mipmap.icon_bluetooth : R.mipmap.ic_launcher;

        iconStatusBluetoothView.setImageResource(src);

    }

    private void updateObdStatusIcon(boolean status)
    {

        int src = status ? R.mipmap.ic_btcar : R.mipmap.ic_launcher;

        iconStatusObdView.setImageResource(src);

    }

    public static void copyAssetsToFolder(AssetManager assetManager, String sourceFolder, String destinationFolder) throws IOException
    {

        final String[] assets = assetManager.list(sourceFolder);
        final File destFolderFile = new File(destinationFolder);

        if(!destFolderFile.exists())
            destFolderFile.mkdirs();

        copyAsset(assetManager, sourceFolder, destinationFolder, assets);

    }

    public static void copyAsset(AssetManager assetManager, String sourceFolder, String destinationFolder, String... assetsNames) throws IOException
    {

        for(String assetName : assetsNames)
        {

            OutputStream destinationStream = new FileOutputStream(new File(destinationFolder + "/" + assetName));
            String[] files = assetManager.list(sourceFolder + "/" + assetName);

            if(files == null || files.length == 0)
            {

                InputStream asset = assetManager.open(sourceFolder + "/" + assetName);

                try
                {

                    ByteStreams.copy(asset, destinationStream);

                }
                finally
                {

                    asset.close();
                    destinationStream.close();

                }

            }

        }

    }

	@Override
	public void onActionPan()
	{

	}

	@Override
	public void onActionZoom()
	{

	}

	@Override
	public void onSurfaceCreated(SKMapViewHolder skMapViewHolder)
	{

		final String path = getFilesDir().getPath() + "/vec-instrument-panel-maps/";

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

		/*SKNavigationSettings navigationSettings = new SKNavigationSettings();
		navigationSettings.setNavigationType(SKNavigationSettings.SKNavigationType.REAL);
		navigationSettings.setNavigationMode(SKNavigationSettings.SKNavigationMode.CAR);
		navigationSettings.setCcpAsCurrentPosition(true);
		navigationSettings.setFcdEnabled(true);

		SKNavigationManager navigationManager = SKNavigationManager.getInstance();
		navigationManager.setMapView(mapHolder.getMapSurfaceView());
		navigationManager.setNavigationListener(this);
		navigationManager.startNavigation(navigationSettings);*/

	}

	@Override
	public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion)
	{

	}

	@Override
	public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion)
	{

	}

	@Override
	public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion)
	{

	}

	@Override
	public void onDoubleTap(SKScreenPoint skScreenPoint)
	{

	}

	@Override
	public void onSingleTap(SKScreenPoint skScreenPoint)
	{

	}

	@Override
	public void onRotateMap()
	{

	}

	@Override
	public void onLongPress(SKScreenPoint skScreenPoint)
	{

	}

	@Override
	public void onInternetConnectionNeeded()
	{

	}

	@Override
	public void onMapActionDown(SKScreenPoint skScreenPoint)
	{

	}

	@Override
	public void onMapActionUp(SKScreenPoint skScreenPoint)
	{

	}

	@Override
	public void onPOIClusterSelected(SKPOICluster skpoiCluster)
	{

	}

	@Override
	public void onMapPOISelected(SKMapPOI skMapPOI)
	{

	}

	@Override
	public void onAnnotationSelected(SKAnnotation skAnnotation)
	{

	}

	@Override
	public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI)
	{

	}

	@Override
	public void onCompassSelected()
	{

	}

	@Override
	public void onCurrentPositionSelected()
	{

	}

	@Override
	public void onObjectSelected(int i)
	{

	}

	@Override
	public void onInternationalisationCalled(int i)
	{

	}

	@Override
	public void onBoundingBoxImageRendered(int i)
	{

	}

	@Override
	public void onGLInitializationError(String s)
	{

	}

	@Override
	public void onNavigationStarted()
	{

	}

	@Override
	public void onNavigationEnded()
	{

	}

	@Override
	public void onRouteCalculationStarted()
	{

	}

	@Override
	public void onRouteCalculationCompleted()
	{

	}

	@Override
	public void onRouteCalculationCanceled()
	{

	}
}
