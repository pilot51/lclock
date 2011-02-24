package com.pilot51.lclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
	private Common common;
	private String TAG, name, timeStr = "Rocket launch";
	private int alerttime;
	private Context context;
	private Bundle bundle;
	
	protected Common newCommon() {
		return new Common(context);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		common = newCommon();
		TAG = common.TAG;
		bundle = intent.getExtras();
		alerttime = bundle.getInt("time");
		name = bundle.getString("name");
//		Log.d(TAG, "Alarm triggered | name = " + name + " | type = " + type + " | alerttime = " + Integer.toString(alerttime));
		if(alerttime == 60000) {
			timeStr += " in 1 minute! ";
		} else if (alerttime == 0) {
			timeStr += " NOW! ";
		} else {
			timeStr += " in " + alerttime/60000 + " minutes! ";
		}
		notificate(2, timeStr, name, context);
		Toast.makeText(context, timeStr + "\n" + name, Toast.LENGTH_LONG).show();
	}
	
	// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
	private void notificate(int id, String title, String text, Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, title, (System.currentTimeMillis() + alerttime));
		try {
			notification.sound = Uri.parse(common.prefs.getString("ring", null));
		} catch (Exception e) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (common.prefs.getBoolean("vibrate", false)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		Intent intent = common.intentList();
		intent.putExtra("source", bundle.getInt("src"));
		Intent notificationIntent = intent;
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, TAG, text, contentIntent);
		mNotificationManager.notify(id, notification);
	}
}