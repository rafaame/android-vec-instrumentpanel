package me.rafaa.vecinstrumentpanel.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import me.rafaa.vecinstrumentpanel.R;
import me.rafaa.vecinstrumentpanel.activity.ConfigActivity;
import me.rafaa.vecinstrumentpanel.activity.DashboardActivity;
import me.rafaa.vecinstrumentpanel.io.ObdCommandJob.ObdCommandJobState;
import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ObdGatewayService extends AbstractGatewayService
{

    private static final String TAG = ObdGatewayService.class.getName();

    @Inject
    SharedPreferences prefs;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;

    public void startService() throws IOException
    {

        Log.d(TAG, "Starting service..");

        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String remoteDevice = prefs.getString(ConfigActivity.BLUETOOTH_LIST_KEY, null);

        if (pairedDevices.size() > 0)
        {

            for (BluetoothDevice device : pairedDevices)
            {

                if(device.getName() == "Nexus 5")
                    remoteDevice = device.getAddress();

            }

        }

        remoteDevice = "CC:FA:00:1E:4E:E9";

        if (remoteDevice == null || "".equals(remoteDevice))
        {

            Toast.makeText(ctx, getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();

            Log.e(TAG, "No Bluetooth device has been selected.");

            stopService();
            throw new IOException();

        }
        else
        {

            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            dev = btAdapter.getRemoteDevice(remoteDevice);


            /*
             * Establish Bluetooth connection
             *
             * Because discovery is a heavyweight procedure for the Bluetooth adapter,
             * this method should always be called before attempting to connect to a
             * remote device with connect(). Discovery is not managed by the Activity,
             * but is run as a system service, so an application should always call
             * cancel discovery even if it did not directly request a discovery, just to
             * be sure. If Bluetooth state is not STATE_ON, this API will return false.
             *
             * see
             * http://developer.android.com/reference/android/bluetooth/BluetoothAdapter
             * .html#cancelDiscovery()
             */
            Log.d(TAG, "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();

            showNotification(getString(R.string.notification_action), getString(R.string.service_starting), R.mipmap.ic_btcar, true, true, false);

            try
            {

                startObdConnection();

            }
            catch(Exception e)
            {

                Log.e(TAG, "There was an error while establishing connection. -> " + e.getMessage());

                stopService();
                throw new IOException();

            }

            showNotification(getString(R.string.notification_action), getString(R.string.service_started), R.mipmap.ic_btcar, true, true, false);

        }

    }

    /**
     * Start and configure the connection to the OBD interface.
     * <p/>
     * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
     *
     * @throws IOException
     */
    private void startObdConnection() throws IOException
    {

        Log.d(TAG, "Starting OBD connection..");
        isRunning = true;

        try
        {

            sock = BluetoothManager.connect(dev);

        }
        catch(Exception e)
        {

            Log.e(TAG, "There was an error while establishing Bluetooth connection. Stopping app..", e);

            stopService();
            throw new IOException();

        }

        Log.d(TAG, "Queueing jobs for connection configuration..");
        queueJob(new ObdCommandJob(new ObdResetCommand()));
        
        //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
        try
        {

            Thread.sleep(500);

        }
        catch(InterruptedException e)
        {

            e.printStackTrace();

        }
        
        queueJob(new ObdCommandJob(new EchoOffCommand()));

        /*
         * Will send second-time based on tests.
         *
         * TODO this can be done w/o having to queue jobs by just issuing
         * command.run(), command.getResult() and validate the result.
         */
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(62)));

        // Get protocol from preferences
        final String protocol = prefs.getString(ConfigActivity.PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));

        // Job for returning dummy data
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

        queueCounter = 0L;
        Log.d(TAG, "Initialization jobs queued.");

    }

    @Override
    public void queueJob(ObdCommandJob job)
    {

        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(ConfigActivity.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);

    }

    protected void executeQueue() throws InterruptedException
    {

        Log.d(TAG, "Executing queue..");

        while(!Thread.currentThread().isInterrupted())
        {

            ObdCommandJob job = null;

            try
            {

                job = jobsQueue.take();

                Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

                if(job.getState().equals(ObdCommandJobState.NEW))
                {

                    Log.d(TAG, "Job state is NEW. Run it..");

                    job.setState(ObdCommandJobState.RUNNING);

                    if(sock.isConnected())
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                    else
                    {

                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, "Can't run command on a closed socket.");

                    }

                }
                else
                    Log.e(TAG, "Job state was not new, so it shouldn't be in queue. BUG ALERT!");

            }
            catch(InterruptedException i)
            {

                Thread.currentThread().interrupt();

            }
            catch(UnsupportedCommandException u)
            {

                if(job != null)
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);

                Log.d(TAG, "Command not supported. -> " + u.getMessage());

            }
            catch(Exception e)
            {

                if(job != null)
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);

                Log.e(TAG, "Failed to run command. -> " + e.getMessage());

            }

            if(job != null)
            {

                final ObdCommandJob job2 = job;

                ((DashboardActivity) ctx).runOnUiThread(new Runnable()
                {

                    @Override
                    public void run()
                    {

                        ((DashboardActivity) ctx).stateUpdate(job2);

                    }

                });

            }

        }

    }

    public void stopService()
    {

        Log.d(TAG, "Stopping service..");

        notificationManager.cancel(NOTIFICATION_ID);
        jobsQueue.clear();
        isRunning = false;

        if(sock != null)
            try
            {

                sock.close();

            }
            catch(IOException e)
            {

                Log.e(TAG, e.getMessage());

            }

        stopSelf();

    }

    public boolean isRunning()
    {

        return isRunning;

    }

    public static void saveLogcatToFile(Context context, String devemail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{devemail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "OBD2 Reader Debug Logs");

        StringBuilder sb = new StringBuilder();
        sb.append("\nManufacturer: ").append(Build.MANUFACTURER);
        sb.append("\nModel: ").append(Build.MODEL);
        sb.append("\nRelease: ").append(Build.VERSION.RELEASE);

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        String fileName = "OBDReader_logcat_" + System.currentTimeMillis() + ".txt";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + "OBD2Logs");
        if (dir.mkdirs()) {
            File outputFile = new File(dir, fileName);
            Uri uri = Uri.fromFile(outputFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            Log.d("savingFile", "Going to save logcat to " + outputFile);
            //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            try {
                @SuppressWarnings("unused")
                Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
