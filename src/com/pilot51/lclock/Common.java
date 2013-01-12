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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.adwhirl.AdWhirlLayout;

public class Common {
	protected static String TAG;
	protected static SharedPreferences prefs;
	protected Activity activity;
	protected Context context;
	
	protected Common(Activity a) {
		this(a.getBaseContext());
		activity = a;
		a.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
	}
	
	protected Common(Context c) {
		context = c;
		init();
	}
	
	private void init() {
		if (TAG == null) {
			TAG = context.getString(R.string.app_name);
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
			PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
		}
		if (!Database.initialized) new Database(context);
	}
	
	protected boolean isOnline() {
		NetworkInfo netInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) return true;
		return false;
	}

	protected void ad() {
		if (!isOnline()) return;
		LinearLayout layout = (LinearLayout) activity.findViewById(R.id.layoutAd);
		if (layout == null) return;
		float density = context.getResources().getDisplayMetrics().density;
		AdWhirlLayout adWhirlLayout = new AdWhirlLayout(activity, "08a2a4f33a2e465eb5d6f899fcc000a8");
		adWhirlLayout.setMaxWidth((int) (320 * density));
		adWhirlLayout.setMaxHeight((int) (52 * density));
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(adWhirlLayout, layout.getLayoutParams());
		layout.invalidate();
	}
}