package com.pilot51.lclock;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class List extends Activity {
	protected Common common;
	private int src, calAccuracy;
	private TextView txtTimer;
	private ListView lv;
	private SimpleAdapter adapter;
	private HashMap<String, Object> launchMap = new HashMap<String, Object>();
	private ArrayList<HashMap<String, Object>> listAdapter;
	protected static ArrayList<HashMap<String, Object>> listNasa, listSfn;
	private static boolean[] isCached = {false, false, false}; // {Attempted loading both, NASA not empty, SpaceflightNow not empty}
	private SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
	private TimerTask timer;
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
		src = getIntent().getIntExtra("source", 0);
		setContentView(R.layout.list);
		loadCache();
		if (getFeed()) {
			common.newAlertBuilder();
			listAdapter = new ArrayList<HashMap<String, Object>>(getCurrentList());
			adapter = new SimpleAdapter(this, listAdapter, R.layout.grid,
					new String[] { "mission", "vehicle", "location", "date", "time", "description" },
					new int[] { R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6 });
			createList();
			timer = new LTimer(listAdapter.get(0));
			lv.setOnItemClickListener(new OnItemClickListener() {
				@SuppressWarnings("unchecked")
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (timer != null)
						timer.cancel();
					timer = new LTimer((HashMap<String, Object>) lv.getItemAtPosition(position));
				}
			});
		}
	}
	
	public void finish() {
		if (timer != null)
			timer.cancel();
		super.finish();
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

	private void createList() {
		lv = (ListView) findViewById(R.id.list);
		common.ad();
		txtTimer = (TextView) findViewById(R.id.txtTime);
		TextView header1 = (TextView) findViewById(R.id.header1);
		if(src == SRC_SFN) header1.setText("Payload");
		registerForContextMenu(lv);
		lv.setAdapter(adapter);
	}
	
	protected ArrayList<HashMap<String, Object>> getCurrentList() {
		return src == SRC_NASA ? listNasa : listSfn;
	}
	private void setCurrentList(ArrayList<HashMap<String, Object>> list) {
		if (src == SRC_NASA) listNasa = list;
		else listSfn = list;
	}

	private Calendar eventCal(HashMap<String, Object> map) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		try {
			boolean hasTime = true, hasDay = true, hasMonth = true;
			int year = Integer.parseInt((String)map.get("year"));
			String date = ((String)map.get("day")).replace("Sept.", "Sep").replaceAll("\\?|/[0-9]+|\\.", ""),
					time = ((String)map.get("time"));
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
				time = time.replaceAll("(\\-| and )[0-9]{4}(:[0-9]{2})?| \\([0-9a-zA-Z:; \\-]*\\)", "");
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

	private boolean getFeed() {
		String data = null;
		if (common.isOnline()) {
			if (src == SRC_NASA)
				data = downloadFile("http://www.nasa.gov/missions/highlights/schedule.html");
			else if (src == SRC_SFN)
				data = downloadFile("http://spaceflightnow.com/tracking/index.html");
		}
		if (data == null) {
			setCurrentList(Database.getEvents(Database.getSrcTable(src)));
			if (getCurrentList().isEmpty())
				Toast.makeText(this, "Error: No data received and no cache.", Toast.LENGTH_LONG).show();
			else {
				// Tell user situation if cache successfully loaded
				Toast.makeText(this, "No data received, loaded from cache.", Toast.LENGTH_LONG).show();
			}
		} else {
			try {
				if (src == SRC_NASA)
					parseNASA(data);
				else if (src == SRC_SFN)
					parseSfn(data);
				// Save cache if new data downloaded
				Database.setEvents(Database.getSrcTable(src), getCurrentList());
			} catch (Exception e) {
				setCurrentList(Database.getEvents(Database.getSrcTable(src)));
				if (getCurrentList().isEmpty())
					Toast.makeText(this, "Error parsing received data,\nno cache to fall back to.", Toast.LENGTH_LONG).show();
				else {
					// Tell user situation if cache successfully loaded
					Toast.makeText(this, "Error parsing received data,\nloaded from cache.", Toast.LENGTH_LONG).show();
				}
				Toast.makeText(this, "Please contact developer if error persists.", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
		if (getCurrentList().isEmpty()) {
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
			long total = 0;
			while ((count = input.read(data)) != -1) {
				total += count;
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
		data = data.replaceAll("<[aA] [^>]*?>|</[aA]>|<font[^>]*?>|</font>|</?b>|\n|\t", "");
		int tmp;
		String year = null;
		for (int i = 0; data.contains("Description:"); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			
			// Isolate event from the rest of the HTML
			String data2 = data.substring(data.indexOf("Date:"), data.indexOf("<br /><br />", data.indexOf("Description:")) + 12);
			
			// Year
			tmp = data.indexOf("<center> 20");
			if (tmp != -1 & tmp < data.indexOf("Date:")) {
				data = data.substring(tmp + 9, data.length());
				year = data.substring(0, data.indexOf(" "));
			}
			map.put("year", year);
			
			data = data.substring(data.indexOf("<br /><br />", data.indexOf("Description:")) + 12, data.length());

			// Date
			data2 = data2.substring(data2.indexOf("Date:") + 6, data2.length());
			map.put("day", data2.substring(0, data2.indexOf("<")).replaceAll("[\\*\\+]*", "").trim());
			map.put("date", map.get("day") + ", " + year);

			// Mission
			data2 = data2.substring(data2.indexOf("Mission:") + 9, data2.length());
			map.put("mission", data2.substring(0, data2.indexOf("<br")).trim());

			// Vehicle
			data2 = data2.substring(data2.indexOf("Vehicle:") + 9, data2.length());
			map.put("vehicle", data2.substring(0, data2.indexOf("<br")).trim());

			// Location
			data2 = data2.substring(data2.indexOf("Site:") + 6, data2.length());
			map.put("location", data2.substring(0, data2.indexOf("<br")).trim());

			// Time
			if (data2.contains("Time:"))
				tmp = data2.indexOf("Time:") + 5;
			else if (data2.contains("Window:"))
				tmp = data2.indexOf("Window:") + 7;
			else if (data2.contains("Times:"))
				tmp = data2.indexOf("Times:") + 6;
			data2 = data2.substring(tmp, data2.length());
			map.put("time", data2.substring(0, data2.indexOf("<br")).replaceAll("[\\.\\*\\+]*", "").replaceAll(" {2,}", " ").trim());

			// Description
			data2 = data2.substring(data2.indexOf("Description:") + 13, data2.length());
			map.put("description", data2.substring(0, data2.indexOf("<br")).trim());

			// Calendar
			map.put("cal", eventCal(map));
			map.put("calAccuracy", calAccuracy);
			
			listNasa.add(map);
		}
	}

	private void parseSfn(String data) {
		data = data.replaceAll("<![ \n\t]*(--([^-]|[\n]|-[^-])*--[ \n\t]*)>|<FONT[^>]*?>|</[\nFONT]{4,5}>|</?B>|<[aA]\\s[^>]*?>|</[aA]>", "");
		int tmp = 0;
		int year = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = 0; data.contains("CC0000"); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			
			// Isolate event from the rest of the HTML
			String data2 = data.substring(data.indexOf("CC0000") + 8, data.indexOf("6\"></TD"));
			data = data.substring(data.indexOf("6\"></TD") + 8, data.length());

			// Date
			map.put("day", data2.substring(0, data2.indexOf("<")).replaceAll("\n", " ").trim());
			map.put("year", Integer.toString(year));
			map.put("date", map.get("day")/* + ", " + map.get("year")*/);

			// Vehicle
			data2 = data2.substring(data2.indexOf(">&nbsp;") + 7, data2.length());
			map.put("vehicle", data2.substring(0, data2.indexOf("&nbsp")).replaceAll("\n", " ").trim());
			
			// Payload
			data2 = data2.substring(data2.indexOf("&#149;") + 18, data2.length());
			map.put("mission", data2.substring(0, data2.indexOf("</TD")).replaceAll("\n|<BR>", " ").trim());

			// Time
			if (data2.indexOf("time:") != -1)
				tmp = data2.indexOf("time:") + 5;
			else if (data2.indexOf("window:") != -1)
				tmp = data2.indexOf("window:") + 7;
			else if (data2.indexOf("times:") != -1)
				tmp = data2.indexOf("times:") + 6;
			data2 = data2.substring(tmp, data2.length());
			map.put("time", data2.substring(0, data2.indexOf("<")).replaceAll("\\.", "").replaceAll("\n", " ").trim());

			// Location
			data2 = data2.substring(data2.indexOf("site:") + 5, data2.length());
			map.put("location", data2.substring(0, data2.indexOf("<")).replaceAll("\n", " ").trim());

			// Description
			data2 = data2.substring(data2.indexOf("><BR>") + 5, data2.length());
			map.put("description", data2.substring(0, data2.indexOf("</TD")).replaceAll("\n", " ").trim());
			
			// Calendar
			map.put("cal", eventCal(map));
			map.put("calAccuracy", calAccuracy);
			
			listSfn.add(map);
		}
	}

	@SuppressWarnings("unchecked")
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater =	getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
		launchMap = (HashMap<String, Object>)lv.getItemAtPosition(((AdapterContextMenuInfo)menuInfo).position);
	}
	
	@SuppressWarnings("unchecked")
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		launchMap = (HashMap<String, Object>)lv.getItemAtPosition(info.position);
		switch (item.getItemId()) {
			case R.id.ctxtMap:
				String location = (String)launchMap.get("location");
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
				Uri.parse("geo:0,0?q=" + location));
				startActivity(intent);
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
		
		private LTimer(HashMap<String, Object> map) {
			tLaunch = ((Calendar)map.get("cal")).getTimeInMillis();
			info = src == SRC_NASA ? ((String)map.get("mission")).replaceAll("</a>|^[0-9a-zA-Z \\-]+\\(|\\)$", "") : (String)map.get("vehicle");
			try {
				accuracy = (Integer)map.get("calAccuracy");
			} catch (NullPointerException e) {
				accuracy = ACC_NONE;
				Log.w(Common.TAG, "Time accuracy not available (likely because loaded cache from v0.6.0), using full time format.");
			}
			if (accuracy == ACC_ERROR | (accuracy == ACC_NONE & tLaunch == 0)) {
				txtTimer.setText(Html.fromHtml(info + "<br /><font color='#FF0000'>Error parsing launch time.</font>"));
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
			runOnUiThread(new Runnable() {
				@Override
				public void run(){
					millisToLaunch = tLaunch - System.currentTimeMillis();
					if (millisToLaunch < 0) dirL = "+";
					days = (int) (millisToLaunch / 1000 / 60 / 60 / 24);
					millisToLaunch %= 1000 * 60 * 60 * 24;
					hours = (int) (millisToLaunch / 1000 / 60 / 60);
					millisToLaunch %= 1000 * 60 * 60;
					minutes = (int) (millisToLaunch / 1000 / 60);
					millisToLaunch %= 1000 * 60;
					seconds = (int) (millisToLaunch / 1000);
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