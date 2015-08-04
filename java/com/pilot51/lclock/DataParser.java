package com.pilot51.lclock;

import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by jacob_000 on 7/25/2015.
 */
public class DataParser {
    static List<Launch> parseData(String data) {

        List<Launch> list = new ArrayList<Launch>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        // Remove data before and after launch activity_main, remove unwanted tags, consolidate launch time identifiers
        data = data.substring(data.indexOf("<div class=\"datename"), data.indexOf("</div>", data.lastIndexOf("missdescrip")) + 6)
                .replaceAll("</?span( [a-z]+=\"(?!launchdate|mission)[^\"]+\")?>|</?[BU]>|</?[aA][^>]*?>", "")
                .replaceAll("&#8217;", "'").replaceAll("&#038;", "+").replaceAll("&amp;", "+")
                .replaceAll("</p>", "</div>")
                .replaceAll("Launch (times?|window|period):", "Launch time:");


        while (data.contains("\"datename\"")) {
            Launch launch = new Launch();

            // Isolate launch from the rest of the HTML
            String eventData = data.substring(data.indexOf("<div class=\"datename"), data.indexOf("</div>", data.indexOf("missdescrip")) + 6);
            data = data.substring(data.indexOf("</div>", data.indexOf("missdescrip")) + 6, data.length());

            // Date
            int tmpIndex = eventData.indexOf("launchdate");
            launch.setDay(eventData.substring(tmpIndex + 12, eventData.indexOf("<", tmpIndex)).replaceAll("Sept", "Sep"));
            launch.setYear(year);
            launch.setDate(launch.getDay());

            // Vehicle
            tmpIndex = eventData.indexOf("mission");
            launch.setVehicle(eventData.substring(tmpIndex + 9, eventData.indexOf(" •", tmpIndex)));
            // Mission / Payload
            launch.setMission(eventData.substring(eventData.indexOf("• ", tmpIndex) + 2, eventData.indexOf("<", tmpIndex)));

            // Time / Window
            tmpIndex = eventData.indexOf("missiondata");
            if (eventData.indexOf("time:", tmpIndex) != -1) {
                tmpIndex = eventData.indexOf("time:", tmpIndex) + 5;
                String time = eventData.substring(tmpIndex, eventData.indexOf("<br", tmpIndex)).replaceAll("\\.m\\.", "m").trim();
                if(!time.contains("TBD")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("D HHmm z", Locale.ENGLISH);
                    boolean hr24 = DateFormat.is24HourFormat(MainActivity.context);
                    time = time.replaceAll("Approx. (:[0-9]{2})?| on [0-9]{1,2}(st|nd|rd|th)| \\([^)]*\\)", "");
                    time = time.replaceAll("(:[0-9]{2})", "");
                    if (time.contains("-")) {
                        try {
                            Date d1 = sdf.parse(sdf.getCalendar().get(Calendar.DAY_OF_YEAR)+" "+ time.substring(0, time.indexOf("-")) + " GMT");
                            Date d2 = sdf.parse(sdf.getCalendar().get(Calendar.DAY_OF_YEAR)+" "+ time.substring(time.indexOf("-") + 1, time.length()));
                            if (hr24) sdf.applyPattern("H:mm");
                            else sdf.applyPattern("h:mm a");
                            time = sdf.format(d1);
                            if (hr24) sdf.applyPattern("H:mm z");
                            else sdf.applyPattern("h:mm a z");
                            time += "-" + sdf.format(d2);
                        } catch (Exception e) { e.printStackTrace();}
                    }
                    else if (time.contains("GMT"))
                        try {
                            Date d1 = sdf.parse(sdf.getCalendar().get(Calendar.DAY_OF_YEAR)+" "+time);
                            if (hr24) sdf.applyPattern("H:mm z");
                            else sdf.applyPattern("h:mm a z");
                            time = sdf.format(d1);
                        } catch (Exception e){ e.printStackTrace(); }
                    launch.setHasCal(true);
                }
                else
                    launch.setHasCal(false);
                launch.setTime(time);

            }

            // Location
            tmpIndex = eventData.indexOf("missiondata");
            launch.setLocation(eventData.substring(eventData.indexOf("site:", tmpIndex) + 5, eventData.indexOf("</div", tmpIndex)));

            // Description
            tmpIndex = eventData.indexOf("missdescrip");
            launch.setDescription(eventData.substring(eventData.indexOf(">", tmpIndex) + 1, eventData.indexOf("</div", tmpIndex)));

            // Calendar
            Calendar cal = CalendarCreator(launch);
            launch.setCal(cal);
            if (cal.get(Calendar.YEAR) > year) {
                launch.setYear(year = cal.get(Calendar.YEAR));
            }
            list.add(launch);
        }
        return list;
    }

    /**
     * Creates a {@link Calendar} from string-based times in an {@link Launch} object.
     * @param launch The launch from which to create the calendar. It does not get modified.
     * @return A {@link } containing the calendar and the accuracy value for the calendar.
     */
    private static Calendar CalendarCreator(Launch launch) {
        final SimpleDateFormat sdf = new SimpleDateFormat("", Locale.ENGLISH);
        if (DateFormat.is24HourFormat(MainActivity.context)) sdf.applyPattern("H:mm zzz MMM d yyyy");
        else sdf.applyPattern("h:mm a zzz MMM d yyyy");
        sdf.setTimeZone(TimeZone.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.clear();
        try {
            boolean hasCal = launch.hasCal();
            int year = launch.getYear();
            String date = launch.getDay().replaceAll("\\?|[0-9]{1,2}/|NET|\\.", "").trim();
            String time = launch.getTime();
            if(hasCal) {
                if (time.contains("-"))
                    time = time.substring(0, time.indexOf("-"))+" "+time.substring(time.length()-3, time.length());
                cal.setTime(sdf.parse(time + " " + date + " " + year));
            }
            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.MONTH, -1);
            if (cal.before(cal2)) {
                cal.add(Calendar.YEAR, 1);
            }
        } catch (ParseException e) {
            cal.clear();
            e.printStackTrace();
        }
        return cal;
    }
}
