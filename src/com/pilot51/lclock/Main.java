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
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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
		btnNASA = (Button)findViewById(R.id.btnNASA);
		btnNASA.setOnClickListener(this);
		btnSfN = (Button)findViewById(R.id.btnSfN);
		btnSfN.setOnClickListener(this);
		new Thread(new Runnable() {
			public void run() {
				List.loadCache();
			}
		}).start();
		new AlertBuilder(this);
		common.ad();
	}

	@Override
	public void onClick(View src) {
		Intent i = new Intent(this, List.class);
		switch (src.getId()) {
			case R.id.btnNASA:
				startActivity(i.putExtra(List.EXTRA_SOURCE, List.SRC_NASA));
				break;
			case R.id.btnSfN:
				startActivity(i.putExtra(List.EXTRA_SOURCE, List.SRC_SFN));
				break;
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_prefs:
				startActivity(new Intent(this, Preferences.class));
				break;
		}
		return true;
	}
}