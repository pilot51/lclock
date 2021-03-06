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

public class Common {
	static String TAG;
	static SharedPreferences prefs, extraPrefs;
	private Context context;
	
	protected Common(Activity a) {
		this(a.getBaseContext());
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
			extraPrefs = context.getSharedPreferences("extraPref", Context.MODE_PRIVATE);
		}
		if (!Database.initialized) {
			new Database(context);
		}
	}
	
	boolean isOnline() {
		NetworkInfo netInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnected();
	}
}