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
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.adwhirl.AdWhirlLayout;

public class Common {
	protected Activity activity;
	protected Context context;
	public String TAG;
	protected Common(Activity a) {
		activity = a;
		context = a;
		setClassVars();
	}
	protected Common(Context c) {
		context = c;
		setClassVars();
	}
	private void setClassVars() {
		TAG = context.getString(R.string.app_name);
	}
	protected Intent intentList() {
		return new Intent(context, List.class);
	}

	void ad() {
		LinearLayout layout = (LinearLayout) activity.findViewById(R.id.layoutAd);
		// Disable ads based on preference or availability
		if (layout == null) {
			//Log.e("AdWhirl", "Layout is null!");
			return;
		}
		float density = context.getResources().getDisplayMetrics().density;
		// int width = (int) (320 * density);
		int height = (int) (52 * density);
		/*
		AdWhirlTargeting.setAge(23);
		AdWhirlTargeting.setGender(AdWhirlTargeting.Gender.MALE);
		AdWhirlTargeting.setKeywords("online games gaming");
		AdWhirlTargeting.setPostalCode("94123");
		AdWhirlTargeting.setTestMode(false);
		AdWhirlAdapter.setGoogleAdSenseAppName("PrediSat");
		AdWhirlAdapter.setGoogleAdSenseCompanyName("Pilot_51");
		*/
		// layout.setLayoutParams(new LinearLayout.LayoutParams(width, height));
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, height);
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(new AdWhirlLayout(activity, "08a2a4f33a2e465eb5d6f899fcc000a8"), lp);
		layout.invalidate();
	}
	
	void saveCache(final int src, final ArrayList<HashMap<String, Object>> list) {
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
	ArrayList<HashMap<String, Object>> readCache(final int src) {
		String sourceName = src == 1 ? "nasa" : "sfn";
		File file = new File(context.getCacheDir() + "/cache_" + sourceName);
		if (!file.exists()) return null;
		ArrayList<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
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