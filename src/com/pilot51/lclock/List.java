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
	private int src, calAccuracy;
	private TextView txtTimer;
	private ListView lv;
	private ListAdapter adapter;
	private ArrayList<Event> adapterList;
	protected static ArrayList<Event> listNasa, listSfn;
	/** {Attempted loading both, NASA cached, SpaceflightNow cached} */
	private static boolean[] isCached = {false, false, false};
	private SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
	private TimerTask timer;
	protected static final String EXTRA_SOURCE = "source";
	protected static final int
		SRC_NASA = 1,
		SRC_SFN = 2,
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
		src = getIntent().getIntExtra(EXTRA_SOURCE, 0);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.list);
		loadCache();
		buildListView();
		refreshList();
		common.ad();
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
		if (!isCached[0]) {
			listNasa = Database.getEvents(Database.TBL_NASA);
			if (!listNasa.isEmpty()) isCached[SRC_NASA] = true;
			listSfn = Database.getEvents(Database.TBL_SFN);
			if (!listSfn.isEmpty()) isCached[SRC_SFN] = true;
			isCached[0] = true;
		}
	}
	
	private void buildListView() {
		adapterList = new ArrayList<Event>(getList());
		adapter = new ListAdapter(this, adapterList);
		lv = (ListView) findViewById(R.id.list);
		txtTimer = (TextView) findViewById(R.id.txtTime);
		if (src == SRC_SFN) ((TextView)findViewById(R.id.header_mission)).setText(getString(R.string.payload));
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
						adapterList.clear();
						adapterList.addAll(getList());
						adapter.notifyDataSetChanged();
						for (int i = 0; i < getList().size(); i++) {
							Event event = getList().get(i);
							if (event.getCal().getTimeInMillis() > System.currentTimeMillis()) {
								new LTimer(adapterList.get(i));
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
		return src == SRC_NASA ? listNasa : listSfn;
	}
	private void setList(ArrayList<Event> list) {
		if (src == SRC_NASA) listNasa = list;
		else listSfn = list;
	}

	private Calendar eventCal(Event event) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		try {
			boolean hasTime = true, hasDay = true, hasMonth = true;
			int year = event.getYear();
			String date = event.getDay().replace("Sept.", "Sep").replaceAll("\\?|[0-9]{1,2}/|NET|\\.", "").trim(),
					time = event.getTime();
			if ((src == SRC_NASA & time.contentEquals("")) | (src == SRC_SFN & time.contentEquals("TBD")))
				hasTime = false;
			if (date.matches("[A-Za-z \\-]+"))
				hasDay = false;
			if (date.contentEquals("TBD"))
				hasMonth = false;
			if (src == SRC_NASA) {
				time = time.replaceAll("( [ap]m)? (–|\\-|and|to) [0-9:]+| */ *[0-9a-zA-Z: \\-]+$", "");
				if (time.matches("[0-9]{1,2}:[0-9]{2}:[0-9]{2} [ap]m [A-Z]+")) {
					sdf.applyPattern("h:mm:ss a z MMM d yyyy");
					cal.setTime(sdf.parse(time + " " + date + " " + year));
					calAccuracy = ACC_SECOND;
				} else if (hasTime) {
					sdf.applyPattern("h:mm a z MMM d yyyy");
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
					if (year == Calendar.getInstance().get(Calendar.YEAR))
						cal.set(year, Calendar.DECEMBER, 31);
					else cal.set(Calendar.YEAR, year);
					calAccuracy = ACC_YEAR;
				}
			} else if (src == SRC_SFN) {
				time = time.replaceAll("(\\-| and )[0-9]{4}(:[0-9]{2})?| on [0-9]{1,2}(st|nd|rd|th)| \\([0-9a-zA-Z:; \\-]*\\)", "");
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
					if (year == Calendar.getInstance().get(Calendar.YEAR))
						cal.set(year, Calendar.DECEMBER, 31);
					else cal.set(Calendar.YEAR, year);
					calAccuracy = ACC_YEAR;
				}
				Calendar cal2 = Calendar.getInstance();
				cal2.set(Calendar.MONTH, cal2.get(Calendar.MONTH) - 1);
				if (cal.before(cal2))
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
			if (src == SRC_NASA)
				data = downloadFile("http://www.nasa.gov/missions/highlights/schedule.html");
			else if (src == SRC_SFN)
				data = downloadFile("http://spaceflightnow.com/tracking/index.html");
		}
		if (data == null) {
			setList(Database.getEvents(Database.getSrcTable(src)));
			if (getList().isEmpty())
				toast(getString(R.string.toast_norcv_nocache));
			else {
				// Tell user situation if cache successfully loaded
				toast(getString(R.string.toast_norcv_loadcache));
			}
		} else {
			try {
				if (src == SRC_NASA)
					parseNASA(data);
				else if (src == SRC_SFN)
					parseSfn(data);
				new AlertBuilder(this);
				// Save cache if new data downloaded
				Database.setEvents(Database.getSrcTable(src), getList());
			} catch (Exception e) {
				setList(Database.getEvents(Database.getSrcTable(src)));
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

	private void parseNASA(String data) {
		data = data.replaceAll("<!--(.|\\s)*?-->|<[aA] [^>]*?>|</[aA]>|<font[^>]*?>|</(font|strong)>|</?b>|\n|\t", "");
		String year = null;
		while (data.contains("Date:")) {
			Event event = new Event();
			
			// Year
			int tmp = data.indexOf("<strong>20");
			if (tmp != -1 & tmp < data.indexOf("Date:")) {
				data = data.substring(tmp + 8, data.length());
				year = data.substring(0, data.indexOf(" "));
			}
			event.setYear(Integer.parseInt(year));
			
			// Isolate event from the rest of the HTML
			tmp = data.indexOf("<br /><br />", data.indexOf("Date:"));
			if (tmp == -1)
				tmp = data.indexOf("</div>", data.indexOf("Date:"));
			String data2 = data.substring(data.indexOf("Date:"), tmp + 6);

			data = data.substring(tmp, data.length());
			
			// Date
			data2 = data2.substring(data2.indexOf("Date:") + 6, data2.length());
			event.setDay(data2.substring(0, data2.indexOf("<")).replaceAll("[\\*\\+(\\(U/R\\))]*", "").trim());
			event.setDate(event.getDay() + ", " + year);

			// Mission
			if (data2.contains("Mission:")) {
				data2 = data2.substring(data2.indexOf("Mission:") + 9, data2.length());
				event.setMission(data2.substring(0, data2.indexOf("<br")).trim());
			} else event.setMission("");

			// Vehicle
			if (data2.contains("Vehicle:")) {
				data2 = data2.substring(data2.indexOf("Vehicle:") + 9, data2.length());
				event.setVehicle(data2.substring(0, data2.indexOf("<br")).trim());
			} else event.setVehicle("");

			// Location
			if (data2.contains("Site:")) {
				data2 = data2.substring(data2.indexOf("Site:") + 5, data2.length());
				event.setLocation(data2.substring(0, data2.indexOf("<br")).trim());
			} else event.setLocation("");

			// Time
			if (data2.contains("Time:") | data2.contains("Window:") | data2.contains("Times:")) {
				if (data2.contains("Time:"))
					tmp = data2.indexOf("Time:") + 5;
				else if (data2.contains("Window:"))
					tmp = data2.indexOf("Window:") + 7;
				else if (data2.contains("Times:"))
					tmp = data2.indexOf("Times:") + 6;
				data2 = data2.substring(tmp, data2.length());
				event.setTime(data2.substring(0, data2.indexOf("<br")).replaceAll("[\\.\\*\\+]*", "").replaceAll(" {2,}", " ").trim());
			} else event.setTime("");

			// Description
			if (data2.contains("Description:")) {
				data2 = data2.substring(data2.indexOf("Description:") + 13, data2.length());
				tmp = data2.indexOf("<br />");
				if (tmp == -1) tmp = data2.indexOf("</div>");
				event.setDescription(data2.substring(0, tmp).trim());
			} else event.setDescription("");

			// Calendar
			event.setCal(eventCal(event));
			event.setCalAccuracy(calAccuracy);
			
			listNasa.add(event);
		}
	}

	private void parseSfn(String data) {
		data = data.replaceAll("<![ \n\t]*(--([^-]|[\n]|-[^-])*--[ \n\t]*)>|<FONT[^>]*?>|</[\nFONT]{4,5}>|</?[BU]>|<[aA]\\s[^>]*?>|</[aA]>", "");
		int tmp = 0;
		int year = Calendar.getInstance().get(Calendar.YEAR);
		while (data.contains("CC0000")) {
			Event event = new Event();
			
			// Isolate event from the rest of the HTML
			String data2 = data.substring(data.indexOf("CC0000") + 8, data.indexOf("6\"></TD"));
			data = data.substring(data.indexOf("6\"></TD") + 8, data.length());

			// Date
			event.setDay(data2.substring(0, data2.indexOf("<")).replaceAll("\n", " ").trim());
			event.setYear(year);
			event.setDate(event.getDay()/* + ", " + map.get("year")*/);

			// Vehicle
			data2 = data2.substring(data2.indexOf(">&nbsp;") + 7, data2.length());
			event.setVehicle(data2.substring(0, data2.indexOf("&nbsp")).replaceAll("\n", " ").trim());
			
			// Payload
			data2 = data2.substring(data2.indexOf("&#149;") + 18, data2.length());
			event.setMission(data2.substring(0, data2.indexOf("</TD")).replaceAll("\n|<BR>", " ").trim());

			// Time
			if (data2.indexOf("time:") != -1)
				tmp = data2.indexOf("time:") + 5;
			else if (data2.indexOf("window:") != -1)
				tmp = data2.indexOf("window:") + 7;
			else if (data2.indexOf("times:") != -1)
				tmp = data2.indexOf("times:") + 6;
			data2 = data2.substring(tmp, data2.length());
			event.setTime(data2.substring(0, data2.indexOf("<")).replaceAll("\\.", "").replaceAll("\n", " ").trim());

			// Location
			data2 = data2.substring(data2.indexOf("site:") + 5, data2.length());
			event.setLocation(data2.substring(0, data2.indexOf("<")).replaceAll("\n", " ").trim());

			// Description
			data2 = data2.substring(data2.indexOf("><BR>") + 5, data2.length());
			event.setDescription(data2.substring(0, data2.indexOf("</TD")).replaceAll("\n", " ").trim());
			
			// Calendar
			event.setCal(eventCal(event));
			event.setCalAccuracy(calAccuracy);
			
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
			info = src == SRC_NASA ? event.getMission().replaceAll("</a>|^[0-9a-zA-Z \\-]+\\(|\\)$", "") : event.getVehicle();
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