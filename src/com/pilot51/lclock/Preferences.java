package com.pilot51.lclock;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

public class Preferences extends PreferenceActivity {
	private int result = 1;
	private String alertTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		alertTime = Common.prefs.getString("prefAlert", null);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (!Common.prefs.getString("alertTime", null).equals(alertTime))
				result *= 2;
			setResult(result);
		}
		return super.onKeyDown(keyCode, event);
	}
}
