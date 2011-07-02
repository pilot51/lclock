package com.pilot51.lclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Main extends Activity implements OnClickListener {
	private Common common;
	private Button btnNASA, btnSfN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		common = new Common(this);
		setContentView(R.layout.main);
		btnNASA = (Button) findViewById(R.id.btnNASA);
		btnNASA.setOnClickListener(this);
		btnSfN = (Button) findViewById(R.id.btnSfN);
		btnSfN.setOnClickListener(this);
		new Thread(new Runnable() {
			public void run() {
				List.loadCache();
		}}).start();
		common.newAlertBuilder();
		common.ad();
	}

	@Override
	public void onClick(View src) {
		Intent i = common.intentList();
		switch (src.getId()) {
			case R.id.btnNASA:
				i.putExtra("source", List.SRC_NASA);
				startActivity(i);
				break;
			case R.id.btnSfN:
				i.putExtra("source", List.SRC_SFN);
				startActivity(i);
				break;
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_prefs:
				startActivityForResult(common.intentPreferences(), 1);
				break;
		}
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.d(TAG, "requestCode: " + requestCode + " | resultCode: " + resultCode);
		if (requestCode == 1)
			if (resultCode % 2 == 0)
				common.newAlertBuilder(); // Alert time preference changed
	}

	public void finish() {
		super.finish();
		Process.killProcess(Process.myPid());
	}
}