package com.pilot51.lclock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

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
		//Log.d(Common.TAG, "Alerts canceled");
		int n = Common.prefsExtra.getInt("nAlerts", 0);
		//Log.d(Common.TAG, "cancelAlerts: " + Integer.toString(n));
		if (n > 0) {
			//Log.d(Common.TAG, "nAlerts > 0");
			do {
				n--;
				//Log.d(Common.TAG, "Canceled alert id: " + n);
				PendingIntent pi = PendingIntent.getBroadcast(common.activity, n, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
				pi.cancel();
				//am.cancel(pi);
			} while (n > 0);
		}
	}

	private void buildAlerts(int src) {
		ArrayList<HashMap<String, Object>> list = src == List.SRC_NASA ? List.listNasa : List.listSfn;
		String nameKey = src == List.SRC_NASA ? "mission" : "vehicle";
		if (!list.isEmpty()) {
			int i = 0;
			HashMap<String, Object> event;
			do {
				event = list.get(i);
				try {
					if ((Integer) event.get("calAccuracy") >= List.ACC_MINUTE) {
						createAlarm(n, ((Calendar) event.get("cal")).getTimeInMillis(),
							(String) event.get(nameKey), src);
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
		//Log.d(Common.TAG, "Creating alert id: " + id);
		intent.putExtra("time", alertTime);
		intent.putExtra("name", name);
		intent.putExtra("src", src);
		PendingIntent pi = PendingIntent.getBroadcast(common.activity, id, intent,
			PendingIntent.FLAG_UPDATE_CURRENT);
		//Log.d(Common.TAG, "Current time: " + new SimpleDateFormat("yyyy-MMMM-dd HH:mm:ss zzz").format(Calendar.getInstance().getTime()));
		//Log.d(Common.TAG, "Current time in milliseconds: " + System.currentTimeMillis());
		//Calendar c = Calendar.getInstance();
		//c.setTimeInMillis(time - alertTime);
		//Log.d(Common.TAG, "Assigned alert time: " + new SimpleDateFormat("yyyy-MMMM-dd HH:mm:ss zzz").format(c.getTime()));
		//Log.d(Common.TAG, "Assigned alert time in milliseconds: " + (time - alertTime));
		if (System.currentTimeMillis() < time - alertTime)
			am.set(AlarmManager.RTC_WAKEUP, time - alertTime, pi);
	}
}