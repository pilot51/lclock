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

import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertBuilder {
	private final Context context;
	private final Intent intent;
	private final AlarmManager am;
	private int n, alertTime;

	AlertBuilder(Context c) {
		context = c;
		am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
		intent = new Intent(c, AlarmReceiver.class);
		new Thread(new Runnable() {
			public void run() {
				cancelAlerts();
				ListActivity.loadCache();
				readAlertPref();
			}
		}).start();
	}

	private void readAlertPref() {
		try {
			alertTime = Integer.parseInt(Common.prefs.getString("alertTime", null));
			alertTime = alertTime * 60000;
		} catch (NumberFormatException e) {
			alertTime = -1;
		}
		if (alertTime != -1) {
			buildAlerts();
		}
		Common.extraPrefs.edit().putInt("nAlerts", n).commit();
	}

	private void cancelAlerts() {
		int n = Common.extraPrefs.getInt("nAlerts", 0);
		while (n > 0) {
			n--;
			PendingIntent pi = PendingIntent.getBroadcast(context, n, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			pi.cancel();
		}
	}

	private void buildAlerts() {
		List<Event> list = ListActivity.listSfn;
		for (Event event : list) {
			try {
				if ((Integer) event.getCalAccuracy() >= ListActivity.ACC_MINUTE) {
					createAlarm(n, event.getCal().getTimeInMillis(), event.getVehicle());
					n++;
				}
			} catch (NullPointerException e) {
				Log.w(Common.TAG,
					"Time accuracy not available (likely because loaded cache from v0.6.0), skipping alert creation.");
				break;
			}
		}
	}

	private void createAlarm(int id, long time, String name) {
		intent.putExtra(AlarmReceiver.EXTRA_TIME, alertTime);
		intent.putExtra(AlarmReceiver.EXTRA_NAME, name);
		PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (System.currentTimeMillis() < time - alertTime) {
			am.set(AlarmManager.RTC_WAKEUP, time - alertTime, pi);
		}
	}
}