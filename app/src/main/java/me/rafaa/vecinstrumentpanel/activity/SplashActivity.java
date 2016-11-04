package me.rafaa.vecinstrumentpanel.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.common.io.ByteStreams;
import com.skobbler.ngx.SKDeveloperKeyException;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitSettings;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.ngx.versioning.SKMapUpdateListener;
import com.skobbler.ngx.versioning.SKVersioningManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.rafaa.vecinstrumentpanel.R;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_splash)
public class SplashActivity extends RoboActivity implements SKPrepareMapTextureListener, SKMapUpdateListener
{
	private static final String TAG = SplashActivity.class.getName();

	private static final int PERMISSION_ACCESS_FINE_LOCATION = 1;
	private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 2;
	private static final int PERMISSION_READ_PHONE_STATE = 2;

	private void prepareSkEngine() {
		final String path = getFilesDir().getPath() + "/maps/";

		if(!new File(path).exists()) {

			new SKPrepareMapTextureThread(this, path, "SKMaps.zip", this).start();

		}
		else
		{

			Toast.makeText(SplashActivity.this, "Map resources copied in a previous run", Toast.LENGTH_SHORT).show();

			initSkEngine();

		}

	}

	private boolean initSkEngine()
	{

		final String path = getFilesDir().getPath() + "/maps/";

		SKLogging.enableLogs(true);

		// get object holding map initialization settings
		SKMapsInitSettings initMapSettings = new SKMapsInitSettings();

		// set path to map resources and initial map style
		initMapSettings.setMapResourcesPaths(path, new SKMapViewStyle(path + "daystyle/", "daystyle.json"));

		final SKAdvisorSettings advisorSettings = initMapSettings.getAdvisorSettings();
		advisorSettings.setAdvisorConfigPath(path +"/Advisor");
		advisorSettings.setResourcePath(path +"/Advisor/Languages");
		advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
		advisorSettings.setAdvisorVoice("en");
		initMapSettings.setAdvisorSettings(advisorSettings);

		// EXAMPLE OF ADDING PREINSTALLED MAPS
//         initMapSettings.setPreinstalledMapsPath(((DemoApplication)context.getApplicationContext()).getMapResourcesDirPath()
//         + "/PreinstalledMaps");
		// initMapSettings.setConnectivityMode(SKMaps.CONNECTIVITY_MODE_OFFLINE);

		// Example of setting light maps
		// initMapSettings.setMapDetailLevel(SKMapsInitSettings.SK_MAP_DETAIL_LIGHT);
		// initialize map using the settings object

		try
		{

			SKMaps.getInstance().initializeSKMaps(SplashActivity.this, initMapSettings);
			SKVersioningManager.getInstance().setMapUpdateListener(this);

		}
		catch(SKDeveloperKeyException exception)
		{

			exception.printStackTrace();

			return false;

		}

        launchDashboardActivity();

		return true;

	}

	@Override
	public void onNewVersionDetected(int newVersion)
	{

		// TODO Auto-generated method stub
		Log.e("", "new version " + newVersion);

	}

	@Override
	public void onMapVersionSet(int newVersion)
	{



	}

	@Override
	public void onVersionFileDownloadTimeout()
	{



	}

	@Override
	public void onNoNewVersionDetected()
	{



	}

	@Override
	public void onMapTexturesPrepared(boolean b)
	{

		SKVersioningManager.getInstance().setMapUpdateListener(this);
		Toast.makeText(SplashActivity.this, "Map resources were copied", Toast.LENGTH_SHORT).show();

		initSkEngine();

	}

	private boolean checkPermissions()
	{

		if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
		{

			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_PHONE_STATE);

			return false;

		}

		if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{

			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);

			return false;

		}

		if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{

			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ACCESS_FINE_LOCATION);

			return false;

		}

		return true;

	}

	private void init()
	{

		if(! checkPermissions())
			return;


		writeLogs();
		prepareSkEngine();

	}

	public boolean isExternalStorageWritable()
	{

		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state))
			return true;

		return false;

	}

	public void writeLogs()
	{

		if (isExternalStorageWritable())
		{

			File appDirectory = new File( Environment.getExternalStorageDirectory() + "/me.rafaa.vecinstrumentpanel" );
			File logDirectory = new File( appDirectory + "/log" );
			File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

			if(!appDirectory.exists())
				appDirectory.mkdir();

			if(!logDirectory.exists())
				logDirectory.mkdir();

			try
			{

				Process process = Runtime.getRuntime().exec("logcat -c");
				process = Runtime.getRuntime().exec("logcat -f " + logFile);

			}
			catch(IOException e)
			{

				e.printStackTrace();

			}

		}

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

	}

	protected void onResume()
	{

		super.onResume();

	}

	@Override
	protected void onPause()
	{

		super.onPause();

	}

	@Override
	protected void onDestroy()
	{

		super.onDestroy();

	}

	private void launchDashboardActivity()
	{

		startActivity(new Intent(this, DashboardActivity.class));

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{

		switch(requestCode)
		{

			case PERMISSION_ACCESS_FINE_LOCATION:
			{

				// If request is cancelled, the result arrays are empty.
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{

					init();

				}
				else
				{

					// permission denied, boo! Disable the
					// functionality that depends on this permission.

				}

				return;

			}

			case PERMISSION_WRITE_EXTERNAL_STORAGE:
			{

				// If request is cancelled, the result arrays are empty.
				if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
				{

					init();

				}
				else
				{

					// permission denied, boo! Disable the
					// functionality that depends on this permission.

				}

				return;

			}

			default:
			{

				super.onRequestPermissionsResult(requestCode, permissions, grantResults);

			}

			// other 'case' lines to check for other
			// permissions this app might request

		}

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

}
