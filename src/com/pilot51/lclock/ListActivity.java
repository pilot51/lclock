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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListActivity extends Activity {
	private Common common;
	private TextView txtTimer;
	private ListView lv;
	private ListAdapter adapter;
	static List<Event> listSfn;
	private static boolean attemptedLoadingCache = false;
	private boolean isRefreshing = false;
	private TimerTask timer;
	static final int
		ACC_ERROR = -1,
		ACC_NONE = 0,
		ACC_YEAR = 1,
		ACC_MONTH = 2,
		ACC_DAY = 3,
		ACC_HOUR = 4,
		ACC_MINUTE = 5,
		ACC_SECOND = 6;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		common = new Common(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.list);
		loadCache();
		buildListView();
		showNextLaunchTimer();
		// Refresh if it has been at least 12 hours since the last successful refresh.
		if (System.currentTimeMillis() - Common.extraPrefs.getLong("lastRefresh", 0) > 1000 * 60 * 60 * 12) {
			refreshList();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				if (!isRefreshing) {
					refreshList();
				}
				break;
			case R.id.menu_prefs:
				startActivity(new Intent(this, Preferences.class));
				break;
		}
		return true;
	}
	
	@Override
	protected void onDestroy() {
		if (timer != null) {
			timer.cancel();
		}
		super.onDestroy();
	}

	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ListActivity.this, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	static synchronized void loadCache() {
		if (!attemptedLoadingCache) {
			listSfn = Database.getEvents(Database.TBL_SFN);
			attemptedLoadingCache = true;
		}
	}
	
	private void buildListView() {
		adapter = new ListAdapter(this, getList());
		lv = (ListView) findViewById(R.id.list);
		txtTimer = (TextView) findViewById(R.id.txtTime);
		((TextView)findViewById(R.id.header_mission)).setText(getString(R.string.payload));
		registerForContextMenu(lv);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				new LTimer((Event)lv.getItemAtPosition(position));
			}
		});
	}
	
	private synchronized void refreshList() {
		if (!common.isOnline()) {
			toast(getString(R.string.toast_noconn));
			return;
		}
		isRefreshing = true;
		setProgressBarIndeterminateVisibility(true);
		new Thread(new Runnable() {
			public void run() {
				loadList();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.setData(getList());
						showNextLaunchTimer();
						setProgressBarIndeterminateVisibility(false);
						isRefreshing = false;
					}
				});
			}
		}).start();
	}
	
	private List<Event> getList() {
		return listSfn;
	}
	private void setList(List<Event> list) {
		listSfn = list;
	}

	private boolean loadList() {
		String data = null;
		if (common.isOnline()) {
			getList().clear();
			data = DataFetcher.downloadFile("http://spaceflightnow.com/launch-schedule/");
		}
		if (data == null) {
			setList(Database.getEvents(Database.TBL_SFN));
			if (getList().isEmpty()) {
				toast(getString(R.string.toast_norcv_nocache));
			} else {
				// Tell user situation if cache successfully loaded
				toast(getString(R.string.toast_norcv_loadcache));
			}
		} else {
			try {
				setList(DataFetcher.parseSfn(data));
				new AlertBuilder(this);
				// Save cache if new data downloaded
				Database.setEvents(Database.TBL_SFN, getList());
				Common.extraPrefs.edit().putLong("lastRefresh", System.currentTimeMillis()).commit();
			} catch (Exception e) {
				setList(Database.getEvents(Database.TBL_SFN));
				if (getList().isEmpty()) {
					toast(getString(R.string.toast_errparse_nocache));
				} else {
					// Tell user situation if cache successfully loaded
					toast(getString(R.string.toast_errparse_loadcache));
				}
				toast(getString(R.string.toast_pls_contact_if_persist));
				e.printStackTrace();
			}
		}
		if (getList().isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater =	getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Event event = (Event)lv.getItemAtPosition(info.position);
		switch (item.getItemId()) {
			case R.id.ctxtMap:
				startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + event.getLocation())));
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	private void showNextLaunchTimer() {
		for (Event event : getList()) {
			if (event.getCal().getTimeInMillis() > System.currentTimeMillis()) {
				new LTimer(event);
				break;
			}
		}
	}

	private class LTimer extends TimerTask {
		private int days, hours, minutes, seconds, accuracy;
		private long tLaunch, millisToLaunch;
		private String info, dirL = "-";
		private StringBuilder s = new StringBuilder();
		private DecimalFormat df = new DecimalFormat("00");
		private final SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
		
		private LTimer(Event event) {
			if (timer != null) {
				timer.cancel();
			}
			timer = this;
			tLaunch = event.getCal().getTimeInMillis();
			info = event.getVehicle();
			try {
				accuracy = event.getCalAccuracy();
			} catch (NullPointerException e) {
				accuracy = ACC_NONE;
				Log.w(Common.TAG, "Time accuracy not available (likely because loaded cache from v0.6.0), using full time format.");
			}
			if (accuracy == ACC_ERROR || (accuracy == ACC_NONE && tLaunch == 0)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtTimer.setText(Html.fromHtml(info + "<br /><font color='#FF0000'>" + getString(R.string.toast_errparse_time) + "</font>"));
					}
				});
				return;
			}
			boolean is24HourFormat = DateFormat.is24HourFormat(getApplicationContext());
			if (accuracy == ACC_SECOND || accuracy == ACC_NONE) {
				if (is24HourFormat) {
					sdf.applyPattern("yyyy-MM-dd H:mm:ss zzz");
				} else {
					sdf.applyPattern("yyyy-MM-dd h:mm:ss a zzz");
				}
			} else if (accuracy == ACC_MINUTE) {
				if (is24HourFormat) {
					sdf.applyPattern("yyyy-MM-dd H:mm zzz");
				} else {
					sdf.applyPattern("yyyy-MM-dd h:mm a zzz");
				}
			} else if (accuracy == ACC_HOUR) {
				if (is24HourFormat) {
					sdf.applyPattern("yyyy-MM-dd H zzz");
				} else {
					sdf.applyPattern("yyyy-MM-dd h a zzz");
				}
			} else if (accuracy == ACC_DAY) {
				sdf.applyPattern("yyyy-MM-dd");
			} else if (accuracy == ACC_MONTH) {
				sdf.applyPattern("yyyy-MM");
			} else if (accuracy == ACC_YEAR) {
				sdf.applyPattern("yyyy");
			}
			new Timer().schedule(this, 0, 500);
		}
		
		@Override
		public void run() {
			millisToLaunch = tLaunch - System.currentTimeMillis();
			if (millisToLaunch < 0) {
				dirL = "+";
			}
			days = (int) (millisToLaunch / 1000 / 60 / 60 / 24);
			millisToLaunch %= 1000 * 60 * 60 * 24;
			hours = (int) (millisToLaunch / 1000 / 60 / 60);
			millisToLaunch %= 1000 * 60 * 60;
			minutes = (int) (millisToLaunch / 1000 / 60);
			millisToLaunch %= 1000 * 60;
			seconds = (int) (millisToLaunch / 1000);
			runOnUiThread(new Runnable() {
				@Override
				public void run(){
					txtTimer.setText(Html.fromHtml(info + "<br />" + sdf.format(tLaunch)
							+ "<br />L " + dirL + " "
							+ clockColor(Math.abs(days), Math.abs(hours), Math.abs(minutes), Math.abs(seconds))
					));
			}});
		}
		
		private String clockColor(int day, int hour, int minute, int second) {
			s.delete(0, s.length());
			if (accuracy < ACC_DAY) {
				s.append("<font color='#FF0000'>");
			}
			s.append(day);
			if (accuracy == ACC_DAY) {
				s.append("<font color='#FF0000'>");
			}
			s.append(":" + df.format(hour));
			if (accuracy == ACC_HOUR) {
				s.append("<font color='#FF0000'>");
			}
			s.append(":" + df.format(minute));
			if (accuracy == ACC_MINUTE) {
				s.append("<font color='#FF0000'>");
			}
			s.append(":" + df.format(second));
			if (accuracy != ACC_SECOND) {
				s.append("</font>");
			}
			return s.toString();
		}
	}
}