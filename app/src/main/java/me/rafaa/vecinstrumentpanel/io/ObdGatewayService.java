package me.rafaa.vecinstrumentpanel.io;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import me.rafaa.vecinstrumentpanel.activity.DashboardActivity;
import roboguice.service.RoboService;

import com.google.inject.Inject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ObdGatewayService extends RoboService
{

    private static final String TAG = ObdGatewayService.class.getName();

    @Inject
    SharedPreferences prefs;

	@Inject
	protected NotificationManager notificationManager;

	public static final int NOTIFICATION_ID = 1;
	private final IBinder binder = new ObdGatewayServiceBinder();

	protected Context context;
	protected boolean isConnected = false;

	public class ObdGatewayServiceBinder extends Binder {
		public ObdGatewayService getService() {
			return ObdGatewayService.this;
		}
	}

	Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				serviceThread();
			}
			catch (InterruptedException e) {
				t.interrupt();
			}
		}
	});

	public void setContext(Context c) {
		context = c;
	}

	public boolean isConnected() {
		return isConnected;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Creating service..");

		t.start();
		Log.d(TAG, "Service created.");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.d(TAG, "Destroying service...");
		notificationManager.cancel(NOTIFICATION_ID);

		t.interrupt();
		Log.d(TAG, "Service destroyed.");
	}

    public void startService() {
		isConnected = true;
        Log.d(TAG, "Starting service..");
    }

	public void stopService() {
		Log.d(TAG, "Stopping service..");

		notificationManager.cancel(NOTIFICATION_ID);
		isConnected = false;

		stopSelf();
	}

    protected void parseObdPacket(String data, int latency) throws InterruptedException {
		final OBDPacket packet = OBDPacket.createFromData(data, latency);

        if (context == null) {
            return;
        }

		((DashboardActivity) context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				((DashboardActivity) context).notifyObdPacket(packet);
			}
		});
    }

	public void serviceThread() throws InterruptedException {
        try {
            DatagramSocket socket = new DatagramSocket(27015);
            long lastPacketTime = System.currentTimeMillis();

            while (! Thread.currentThread().isInterrupted()) {
                byte[] buffer = new byte[2048];

                //DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                //socket.receive(packet);

                //String data = new String(buffer, 0, packet.getLength());
                String data = "0\n0\n0\n0\n0\n0\n0\n0\n0\n0\n0";
                Thread.sleep(100);
                //if (data != null) {
                    long elapsedTime = System.nanoTime() - lastPacketTime;
                    lastPacketTime = System.nanoTime();
					parseObdPacket(data, (int) elapsedTime / 1000000);
                //}
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	protected void showNotification(String contentTitle, String contentText, int icon, boolean ongoing, boolean notify, boolean vibrate)
	{

		final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, DashboardActivity.class), 0);
		final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);

		notificationBuilder
				.setContentTitle(contentTitle)
				.setContentText(contentText).setSmallIcon(icon)
				.setContentIntent(contentIntent)
				.setWhen(System.currentTimeMillis());

		// can cancel?
		if(ongoing)
			notificationBuilder.setOngoing(true);
		else
			notificationBuilder.setAutoCancel(true);

		if(vibrate)
			notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);

		if(notify)
			notificationManager.notify(NOTIFICATION_ID, notificationBuilder.getNotification());

	}

}
