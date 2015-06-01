package xizz.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
	private static final String TAG = "NotificationReceiver";

	@Override
	public void onReceive(Context c, Intent i) {
		Log.d(TAG, "received result: " + getResultCode());

		// a foreground activity cancelled the broadcast
		if (getResultCode() != Activity.RESULT_OK)
			return;

		int requestCode = i.getIntExtra("REQUEST_CODE", 0);
		Notification notification = i.getParcelableExtra("NOTIFICATION");

		NotificationManager notificationManager = (NotificationManager)
				c.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(requestCode, notification);
	}
}