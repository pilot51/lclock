/*
 * Copyright 2013 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	static final String EXTRA_NAME = "name", EXTRA_TIME = "time";
	private String name, msg;
	private int alerttime;
	private Bundle bundle;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		bundle = intent.getExtras();
		alerttime = bundle.getInt(EXTRA_TIME);
		name = bundle.getString(EXTRA_NAME);
		msg = MessageFormat.format(context.getString(R.string.alert_msg), alerttime/60000);
		notificate(2, msg, name, context);
		Toast.makeText(context, msg + "\n" + name, Toast.LENGTH_LONG).show();
	}
	
	private void notificate(int id, String title, String text, Context context) {
		Notification notification = new Notification(R.drawable.icon, title, (System.currentTimeMillis() + alerttime));
		try {
			notification.sound = Uri.parse(Common.prefs.getString("ring", null));
		} catch (Exception e) {
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		if (Common.prefs.getBoolean("vibrate", false)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(context, Common.TAG, text,
				PendingIntent.getActivity(context, 0, new Intent(context, ListActivity.class), 0));
		((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, notification);
	}
}