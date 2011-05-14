package com.pilot51.lclock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.adwhirl.AdWhirlLayout;

public class Common {
	protected Activity activity;
	protected Context context;
	protected String TAG;
	protected SharedPreferences prefs;
	protected Common(Activity a) {
		activity = a;
		context = a;
		a.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
		setClassVars();
	}
	protected Common(Context c) {
		context = c;
		setClassVars();
	}
	private void setClassVars() {
		TAG = context.getString(R.string.app_name);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
	protected Intent intentPreferences() {
		return new Intent(context, Preferences.class);
	}
	protected Intent intentList() {
		return new Intent(context, List.class);
	}
	protected AlertBuilder newAlertBuilder() {
		return new AlertBuilder(activity);
	}
	protected Intent intentAlarmReceiver() {
    	return new Intent(context, AlarmReceiver.class);
    }
	
	protected boolean isOnline() {
		NetworkInfo netInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) return true;
		return false;
	}

	protected void ad() {
		if (!isOnline()) return;
		LinearLayout layout = (LinearLayout) activity.findViewById(R.id.layoutAd);
		if (layout == null) {
			//Log.e("AdWhirl", "Layout is null!");
			return;
		}
		float density = context.getResources().getDisplayMetrics().density;
		AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "08a2a4f33a2e465eb5d6f899fcc000a8");
		adWhirlLayout.setMaxWidth((int) (320 * density));
		adWhirlLayout.setMaxHeight((int) (52 * density));
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(adWhirlLayout, layout.getLayoutParams());
		layout.invalidate();
	}
	
	protected void saveCache(final int src, final ArrayList<HashMap<String, Object>> list) {
		String sourceName = src == 1 ? "nasa" : "sfn";
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(context.getCacheDir() + "/cache_" + sourceName)));
			out.writeObject(list);
			out.close();
		} catch (IOException e) {
			Log.e(TAG, "Error saving cache file");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	protected ArrayList<HashMap<String, Object>> readCache(final int src) {
		String sourceName = src == 1 ? "nasa" : "sfn";
		File file = new File(context.getCacheDir() + "/cache_" + sourceName);
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		if (!file.exists()) return list;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
			try {
				list = (ArrayList<HashMap<String, Object>>) in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			in.close();
		} catch (IOException e) {
			Log.e(TAG, "Error reading cache file");
			e.printStackTrace();
		}
		return list;
	}
}