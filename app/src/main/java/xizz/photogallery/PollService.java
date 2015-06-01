package xizz.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

public class PollService extends IntentService {
	public static final String PREF_IS_ALARM_ON = "isAlarmOn";
	public static final String ACTION_SHOW_NOTIFICATION = "xizz.photogallery.SHOW_NOTIFICATION";
	public static final String PERM_PRIVATE = "xizz.photogallery.PRIVATE";

	private static final String TAG = "PollService";
	private static final int POLL_INTERVAL = 1000 * 60;

	public PollService() {
		super(TAG);
	}

	public static void setServiceAlarm(Context context, boolean isOn) {
		Intent i = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (isOn) {
			alarmManager.setRepeating(AlarmManager.RTC,
					System.currentTimeMillis(), POLL_INTERVAL, pi);
		} else {
			alarmManager.cancel(pi);
			pi.cancel();
		}

		PreferenceManager.getDefaultSharedPreferences(context)
				.edit()
				.putBoolean(PollService.PREF_IS_ALARM_ON, isOn)
				.commit();
	}

	public static boolean isServiceAlarmOn(Context context) {
		Intent i = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(
				context, 0, i, PendingIntent.FLAG_NO_CREATE);
		return pi != null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "Received an intent: " + intent);

		ConnectivityManager connectivityManager =
				(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager.getActiveNetworkInfo() == null)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);

		List<GalleryItem> items;

		if (query != null) {
			items = new FlickrFetchr().search(query);
		} else {
			items = new FlickrFetchr().fetchItems();
		}

		if (items.size() == 0)
			return;

		String resultId = items.get(0).id;

		if (!resultId.equals(lastResultId)) {
			Log.d(TAG, "Got a new result: " + resultId);

			Resources r = getResources();
			PendingIntent pi = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoGalleryActivity.class), 0);

			Notification notification = new Notification.Builder(this)
					.setTicker(r.getString(R.string.new_pictures_title))
					.setSmallIcon(android.R.drawable.ic_menu_report_image)
					.setContentTitle(r.getString(R.string.new_pictures_title))
					.setContentText(r.getString(R.string.new_pictures_text))
					.setContentIntent(pi)
					.setAutoCancel(true)
					.build();

			showBackgroundNotification(0, notification);

			prefs.edit().putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId).apply();
		} else
			Log.d(TAG, "Got a old result");
	}

	private void showBackgroundNotification(int requestCode, Notification notification) {
		Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
		i.putExtra("REQUEST_CODE", requestCode);
		i.putExtra("NOTIFICATION", notification);

		sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
	}
}
