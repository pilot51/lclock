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

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertBuilder {
	private Common common;
	private int n, alertTime;
	private Intent intent;
	private AlarmManager am;

	protected AlertBuilder(final Activity activity) {
		new Thread(new Runnable() {
			public void run() {
				common = new Common(activity);
				am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
				intent = common.intentAlarmReceiver();
				cancelAlerts();
				List.loadCache();
				readAlertPref();
			}
		}).start();
	}

	protected void readAlertPref() {
		try {
			alertTime = Integer.parseInt(Common.prefs.getString("alertTime", null));
			alertTime = alertTime * 60000;
		} catch (NumberFormatException e) {
			alertTime = -1;
		}
		if (alertTime != -1) {
			buildAlerts(List.SRC_NASA);
			buildAlerts(List.SRC_SFN);
		}
		Common.prefsExtra.edit().putInt("nAlerts", n).commit();
	}

	private void cancelAlerts() {
		int n = Common.prefsExtra.getInt("nAlerts", 0);
		if (n > 0) {
			do {
				n--;
				PendingIntent pi = PendingIntent.getBroadcast(common.activity, n, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
				pi.cancel();
			} while (n > 0);
		}
	}

	private void buildAlerts(int src) {
		ArrayList<Event> list = src == List.SRC_NASA ? List.listNasa : List.listSfn;
		if (!list.isEmpty()) {
			int i = 0;
			do {
				Event event = list.get(i);
				try {
					if ((Integer) event.getCalAccuracy() >= List.ACC_MINUTE) {
						createAlarm(n, event.getCal().getTimeInMillis(),
								src == List.SRC_NASA ? event.getMission() : event.getVehicle(), src);
						n++;
					}
				} catch (NullPointerException e) {
					Log.w(Common.TAG,
						"Time accuracy not available (likely because loaded cache from v0.6.0), skipping alert creation.");
					break;
				}
				i++;
			} while (i < list.size());
		}
	}

	private void createAlarm(int id, long time, String name, int src) {
		intent.putExtra("time", alertTime);
		intent.putExtra("name", name);
		intent.putExtra("src", src);
		PendingIntent pi = PendingIntent.getBroadcast(common.activity, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (System.currentTimeMillis() < time - alertTime) {
			am.set(AlarmManager.RTC_WAKEUP, time - alertTime, pi);
		}
	}
}