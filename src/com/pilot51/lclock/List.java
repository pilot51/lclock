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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
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

public class List extends Activity {
	protected Common common;
	private int calAccuracy;
	private TextView txtTimer;
	private ListView lv;
	private ListAdapter adapter;
	protected static ArrayList<Event> listSfn;
	private static boolean attemptedLoadingCache = false;
	private SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
	private TimerTask timer;
	protected enum DataSource {
//		NASA, // Broken indefinitely
		SFN;
		
		private boolean isCached;
	}
	protected static final int
		ACC_ERROR = -1,
		ACC_NONE = 0,
		ACC_YEAR = 1,
		ACC_MONTH = 2,
		ACC_DAY = 3,
		ACC_HOUR = 4,
		ACC_MINUTE = 5,
		ACC_SECOND = 6;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		common = new Common(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.list);
		loadCache();
		buildListView();
		refreshList();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
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
	
	public void finish() {
		if (timer != null)
			timer.cancel();
		super.finish();
	}
	
	private void toast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(List.this, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	protected static synchronized void loadCache() {
		if (!attemptedLoadingCache) {
			listSfn = Database.getEvents(Database.TBL_SFN);
			if (!listSfn.isEmpty()) {
				DataSource.SFN.isCached = true;
			}
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
		setProgressBarIndeterminateVisibility(true);
		new Thread(new Runnable() {
			public void run() {
				loadList();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.setData(getList());
						for (Event event : getList()) {
							if (event.getCal().getTimeInMillis() > System.currentTimeMillis()) {
								new LTimer(event);
								break;
							}
						}
						setProgressBarIndeterminateVisibility(false);
					}
				});
			}
		}).start();
	}
	
	private ArrayList<Event> getList() {
		return listSfn;
	}
	private void setList(ArrayList<Event> list) {
		listSfn = list;
	}

	private Calendar eventCal(Event event) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		try {
			boolean hasTime = true, hasDay = true, hasMonth = true;
			int year = event.getYear();
			String date = event.getDay().replace("Sept.", "Sep").replaceAll("\\?|[0-9]{1,2}/|NET|\\.", "").trim(),
					time = event.getTime();
			if (time.contentEquals("TBD")) {
				hasTime = false;
			}
			if (date.matches("[A-Za-z \\-]+"))
				hasDay = false;
			if (date.contentEquals("TBD"))
				hasMonth = false;
			time = time.replaceAll("Approx. |(\\-| and )[0-9]{4}(:[0-9]{2})?| on [0-9]{1,2}(st|nd|rd|th)| \\([^)]*\\)", "");
			if (time.matches("[0-9]{4}:[0-9]{2} [A-Z]+")) {
				sdf.applyPattern("HHmm:ss z MMM d yyyy");
				cal.setTime(sdf.parse(time + " " + date + " " + year));
				calAccuracy = ACC_SECOND;
			} else if (hasTime) {
				sdf.applyPattern("HHmm z MMM d yyyy");
				cal.setTime(sdf.parse(time + " " + date + " " + year));
				calAccuracy = ACC_MINUTE;
			} else if (hasDay) {
				sdf.applyPattern("MMM d yyyy");
				cal.setTime(sdf.parse(date + " " + year));
				calAccuracy = ACC_DAY;
			} else if (hasMonth) {
				sdf.applyPattern("MMM yyyy");
				cal.setTime(sdf.parse(date + " " + year));
				calAccuracy = ACC_MONTH;
			} else {
				if (year == Calendar.getInstance().get(Calendar.YEAR)) {
					cal.set(year, Calendar.DECEMBER, 31);
				} else cal.set(Calendar.YEAR, year);
				calAccuracy = ACC_YEAR;
			}
			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.MONTH, -1);
			if (cal.before(cal2)) {
				cal.add(Calendar.YEAR, 1);
			}
		} catch (ParseException e) {
			cal.clear();
			calAccuracy = ACC_ERROR;
			e.printStackTrace();
		}
		return cal;
	}

	private boolean loadList() {
		String data = null;
		if (common.isOnline()) {
			getList().clear();
			data = downloadFile("http://spaceflightnow.com/launch-schedule/");
		}
		if (data == null) {
			setList(Database.getEvents(Database.TBL_SFN));
			if (getList().isEmpty())
				toast(getString(R.string.toast_norcv_nocache));
			else {
				// Tell user situation if cache successfully loaded
				toast(getString(R.string.toast_norcv_loadcache));
			}
		} else {
			try {
				parseSfn(data);
				new AlertBuilder(this);
				// Save cache if new data downloaded
				Database.setEvents(Database.TBL_SFN, getList());
			} catch (Exception e) {
				setList(Database.getEvents(Database.TBL_SFN));
				if (getList().isEmpty())
					toast(getString(R.string.toast_errparse_nocache));
				else {
					// Tell user situation if cache successfully loaded
					toast(getString(R.string.toast_errparse_loadcache));
				}
				toast(getString(R.string.toast_pls_contact_if_persist));
				e.printStackTrace();
			}
		}
		if (getList().isEmpty()) {
			// Finish List activity if no data loaded
			finish();
			return false;
		}
		return true;
	}

	private String downloadFile(String url) {
		InputStream input = null;
		String strdata = null;
		StringBuffer strbuff = new StringBuffer();
		int count;
		try {
			URL url2 = new URL(url);
			// download the file
			input = new BufferedInputStream(url2.openStream());
			byte data[] = new byte[1024];
			while ((count = input.read(data)) != -1) {
				strbuff.append(new String(data, 0, count));
			}
			strdata = strbuff.toString();
			strdata = strdata.replaceAll("\r\n", "\n");
			input.close();
		} catch (Exception e) {
			Log.e(Common.TAG, "Error downloading file!");
			e.printStackTrace();
		}
		return strdata;
	}

	private void parseSfn(String data) {
		// Remove data before and after launch list and remove unwanted tags
		data = data.substring(data.indexOf("<div class=\"datename"), data.indexOf("</div>", data.lastIndexOf("missdescrip")) + 6)
				.replaceAll("</?span( [a-z]+=\"(?!launchdate|mission)[a-z]+\")?>|</?[BU]>|</?[aA][^>]*?>", "");
		int year = Calendar.getInstance().get(Calendar.YEAR);
		
		while (data.contains("\"datename\"")) {
			Event event = new Event();
			
			// Isolate event from the rest of the HTML
			String eventData = data.substring(data.indexOf("<div class=\"datename"), data.indexOf("</div>", data.indexOf("missdescrip")) + 6);
			data = data.substring(data.indexOf("</div>", data.indexOf("missdescrip")) + 6, data.length());

			// Date
			int tmpIndex = eventData.indexOf("launchdate");
			event.setDay(eventData.substring(tmpIndex + 12, eventData.indexOf("<", tmpIndex)));
			event.setYear(year);
			event.setDate(event.getDay());

			// Vehicle
			tmpIndex = eventData.indexOf("mission");
			event.setVehicle(eventData.substring(tmpIndex + 9, eventData.indexOf(" •", tmpIndex)));
			
			// Mission / Payload
			event.setMission(eventData.substring(eventData.indexOf("• ", tmpIndex) + 2, eventData.indexOf("<", tmpIndex)));

			// Time
			tmpIndex = eventData.indexOf("missiondata");
			if (eventData.indexOf("time:", tmpIndex) != -1) {
				tmpIndex = eventData.indexOf("time:", tmpIndex) + 5;
			} else if (eventData.indexOf("window:", tmpIndex) != -1) {
				tmpIndex = eventData.indexOf("window:", tmpIndex) + 7;
			} else if (eventData.indexOf("times:", tmpIndex) != -1) {
				tmpIndex = eventData.indexOf("times:", tmpIndex) + 6;
			} else {
				tmpIndex = -1;
			}
			if (tmpIndex > 0) {
				event.setTime(eventData.substring(tmpIndex, eventData.indexOf("<br", tmpIndex)).replaceAll("\\.m\\.", "m").trim());
			}

			// Location
			tmpIndex = eventData.indexOf("missiondata");
			event.setLocation(eventData.substring(eventData.indexOf("site:", tmpIndex) + 5, eventData.indexOf("</div", tmpIndex)));

			// Description
			tmpIndex = eventData.indexOf("missdescrip");
			event.setDescription(eventData.substring(eventData.indexOf(">", tmpIndex) + 1, eventData.indexOf("</div", tmpIndex)));
			
			// Calendar
			Calendar cal = eventCal(event);
			event.setCal(cal);
			event.setCalAccuracy(calAccuracy);
			
			if (cal.get(Calendar.YEAR) > year) {
				event.setYear(year = cal.get(Calendar.YEAR));
			}
			
			listSfn.add(event);
		}
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater =	getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}
	
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
	
	private class LTimer extends TimerTask {
		private int days, hours, minutes, seconds, accuracy;
		private long tLaunch, millisToLaunch;
		private String info, dirL = "-";
		private StringBuilder s = new StringBuilder();
		private DecimalFormat df = new DecimalFormat("00");
		
		private LTimer(Event event) {
			if (timer != null)
				timer.cancel();
			timer = this;
			tLaunch = event.getCal().getTimeInMillis();
			info = event.getVehicle();
			try {
				accuracy = event.getCalAccuracy();
			} catch (NullPointerException e) {
				accuracy = ACC_NONE;
				Log.w(Common.TAG, "Time accuracy not available (likely because loaded cache from v0.6.0), using full time format.");
			}
			if (accuracy == ACC_ERROR | (accuracy == ACC_NONE & tLaunch == 0)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						txtTimer.setText(Html.fromHtml(info + "<br /><font color='#FF0000'>" + getString(R.string.toast_errparse_time) + "</font>"));
					}
				});
				return;
			}
			if (accuracy == ACC_SECOND | accuracy == ACC_NONE)
				sdf.applyPattern("yyyy-MM-dd h:mm:ss a zzz");
			else if (accuracy == ACC_MINUTE)
				sdf.applyPattern("yyyy-MM-dd h:mm a zzz");
			else if (accuracy == ACC_HOUR)
				sdf.applyPattern("yyyy-MM-dd h a zzz");
			else if (accuracy == ACC_DAY)
				sdf.applyPattern("yyyy-MM-dd");
			else if (accuracy == ACC_MONTH)
				sdf.applyPattern("yyyy-MM");
			else if (accuracy == ACC_YEAR)
				sdf.applyPattern("yyyy");
			new Timer().schedule(this, 0, 500);
		}
		
		@Override
		public void run() {
			millisToLaunch = tLaunch - System.currentTimeMillis();
			if (millisToLaunch < 0) dirL = "+";
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
			if (accuracy < ACC_DAY)
				s.append("<font color='#FF0000'>");
			s.append(day);
			if (accuracy == ACC_DAY)
				s.append("<font color='#FF0000'>");
			s.append(":" + df.format(hour));
			if (accuracy == ACC_HOUR)
				s.append("<font color='#FF0000'>");
			s.append(":" + df.format(minute));
			if (accuracy == ACC_MINUTE)
				s.append("<font color='#FF0000'>");
			s.append(":" + df.format(second));
			if (accuracy != ACC_SECOND)
				s.append("</font>");
			return s.toString();
		}
	}
}