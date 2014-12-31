package com.pilot51.lclock;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.util.Log;

/**
 * Class for downloading and parsing data.
 */
public class DataFetcher {
	static String downloadFile(String url) {
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
		} catch (IOException e) {
			Log.e(Common.TAG, "Error downloading file!");
			e.printStackTrace();
		}
		return strdata;
	}

	static List<Event> parseSfn(String data) {
		List<Event> list = new ArrayList<Event>();
		int year = Calendar.getInstance().get(Calendar.YEAR);

		// Remove data before and after launch list and remove unwanted tags
		data = data.substring(data.indexOf("<div class=\"datename"), data.indexOf("</div>", data.lastIndexOf("missdescrip")) + 6)
				.replaceAll("</?span( [a-z]+=\"(?!launchdate|mission)[^\"]+\")?>|</?[BU]>|</?[aA][^>]*?>", "")
				.replaceAll("&#8217;", "'").replaceAll("&amp;", "&");

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
			CalendarAccuracyPair calPair = eventCal(event);
			Calendar cal = calPair.calendar;
			event.setCal(cal);
			event.setCalAccuracy(calPair.accuracy);

			if (cal.get(Calendar.YEAR) > year) {
				event.setYear(year = cal.get(Calendar.YEAR));
			}

			list.add(event);
		}
		return list;
	}

	/**
	 * Creates a {@link Calendar} from string-based times in an {@link Event} object.
	 * @param event The event from which to create the calendar. It does not get modified.
	 * @return A {@link CalendarAccuracyPair} containing the calendar and the accuracy value for the calendar.
	 */
	private static CalendarAccuracyPair eventCal(Event event) {
		final SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
		int calAccuracy;
		Calendar cal = Calendar.getInstance();
		cal.clear();
		try {
			boolean hasTime = true, hasDay = true, hasMonth = true;
			int year = event.getYear();
			String date = event.getDay().replace("Sept.", "Sep").replaceAll("\\?|[0-9]{1,2}/|NET|\\.", "").trim();
			String time = event.getTime();
			if (time.contentEquals("TBD")) {
				hasTime = false;
			}
			if (date.matches("[A-Za-z \\-]+")) {
				hasDay = false;
			}
			if (date.contentEquals("TBD")) {
				hasMonth = false;
			}
			time = time.replaceAll("Approx. |(\\-| and )[0-9]{4}(:[0-9]{2})?| on [0-9]{1,2}(st|nd|rd|th)| \\([^)]*\\)", "");
			if (time.matches("[0-9]{4}:[0-9]{2} [A-Z]+")) {
				sdf.applyPattern("HHmm:ss z MMM d yyyy");
				cal.setTime(sdf.parse(time + " " + date + " " + year));
				calAccuracy = ListActivity.ACC_SECOND;
			} else if (hasTime) {
				sdf.applyPattern("HHmm z MMM d yyyy");
				cal.setTime(sdf.parse(time + " " + date + " " + year));
				calAccuracy = ListActivity.ACC_MINUTE;
			} else if (hasDay) {
				sdf.applyPattern("MMM d yyyy");
				cal.setTime(sdf.parse(date + " " + year));
				calAccuracy = ListActivity.ACC_DAY;
			} else if (hasMonth) {
				sdf.applyPattern("MMM yyyy");
				cal.setTime(sdf.parse(date + " " + year));
				calAccuracy = ListActivity.ACC_MONTH;
			} else {
				if (year == Calendar.getInstance().get(Calendar.YEAR)) {
					cal.set(year, Calendar.DECEMBER, 31);
				} else {
					cal.set(Calendar.YEAR, year);
				}
				calAccuracy = ListActivity.ACC_YEAR;
			}
			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.MONTH, -1);
			if (cal.before(cal2)) {
				cal.add(Calendar.YEAR, 1);
			}
		} catch (ParseException e) {
			cal.clear();
			calAccuracy = ListActivity.ACC_ERROR;
			e.printStackTrace();
		}
		return new CalendarAccuracyPair(cal, calAccuracy);
	}

	private static class CalendarAccuracyPair {
		private Calendar calendar;
		private int accuracy;

		private CalendarAccuracyPair(Calendar calendar, int accuracy) {
			this.calendar = calendar;
			this.accuracy = accuracy;
		}
	}
}
