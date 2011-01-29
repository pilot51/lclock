/*
 * Immediately before release:
 * TODO Test on all API's and screen sizes
 * TODO In all projects, set release date in strings and version in strings and manifest
 * TODO Clean before compiling
 * TODO Install old version from Market to make sure update from Market works properly
 * TODO Save sources along with binary for each release version
 * 
 * Research:
 * Online sources:
 * 		http://spaceflightnow.com/tracking/
 * 		http://nasa.gov/missions/highlights/schedule.html
 * 		http://www.space.com/missionlaunches/launches/launch_schedule.html
 * 		http://kennedyspacecenter.com/events.aspx (countdown for next rocket launch at top-left)
 * 
 * Problems:
 * FIXME Text in main screen in landscape mode
 * 
 * v1.0.0: -- Deadline: 1 week before scheduled launch of STS-133
 * TODO Look for major features in MissionClock to include, maybe get some style ideas too
 * TODO Countdown for end of launch window
 * TODO Don't say "Launch!" unless it can actually be confirmed
 * TODO Synchronized countdown for STS-133 at very minimum
 * 
 * Bin:
 * 
 */

package com.pilot51.lclock;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Process;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity implements OnClickListener {
	protected Common common = newCommon();
	String TAG;
	TextView txtTimer;
	Button btnNASA, btnSfN;
	HashMap<String, String> launchMap = new HashMap<String, String>();
	CountDownTimer timer;
	
	protected Common newCommon() {
		return new Common();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		TAG = getString(R.string.app_name);
		
		btnNASA = (Button) findViewById(R.id.btnNASA);
		btnNASA.setOnClickListener(this);
		btnSfN = (Button) findViewById(R.id.btnSfN);
		btnSfN.setOnClickListener(this);
		common.ad(this);
	}
	
	@Override
	public void onClick(View src) {
		Intent i = new Intent(this, List.class);
		switch (src.getId()) {
			case R.id.btnNASA:
				i.putExtra("source", 1);
				startActivity(i);
				break;
			case R.id.btnSfN:
				i.putExtra("source", 2);
				startActivity(i);
				break;
		}
	}
	
	public void finish() {
		super.finish();
		Process.killProcess(Process.myPid());
	}
}