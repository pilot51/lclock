package com.pilot51.lclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

public class Preferences extends PreferenceActivity {
	private Common common;
	private SharedPreferences prefs;
	private int result = 1;
	private String alertTime;
	
	protected Common newCommon() {
		return new Common(this);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		common = newCommon();
		prefs = common.prefs;
		alertTime = prefs.getString("prefAlert", null);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (!prefs.getString("alertTime", null).equals(alertTime)) {
				result *= 2;
			}
			setResult(result);
		}
		return super.onKeyDown(keyCode, event);
	}
}
