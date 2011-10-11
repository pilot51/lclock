package com.pilot51.lclock;

import java.text.MessageFormat;

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
	private String name, msg;
	private int alerttime;
	private Bundle bundle;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		common = new Common(context);
		bundle = intent.getExtras();
		alerttime = bundle.getInt("time");
		name = bundle.getString("name");
		msg = MessageFormat.format(context.getString(R.string.alert_msg), alerttime/60000);
		notificate(2, msg, name, context);
		Toast.makeText(context, msg + "\n" + name, Toast.LENGTH_LONG).show();
	}
	
	// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
	private void notificate(int id, String title, String text, Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, title, (System.currentTimeMillis() + alerttime));
		try {
			notification.sound = Uri.parse(Common.prefs.getString("ring", null));
		} catch (Exception e) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (Common.prefs.getBoolean("vibrate", false))
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		Intent intent = common.intentList();
		intent.putExtra("source", bundle.getInt("src"));
		Intent notificationIntent = intent;
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, Common.TAG, text, contentIntent);
		mNotificationManager.notify(id, notification);
	}
}