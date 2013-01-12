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

import java.util.ArrayList;
import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class Database extends SQLiteOpenHelper {
	private static Database database;
	private static final String DATABASE_NAME = "cache.db";
	private static final int DATABASE_VERSION = 1;
	protected static boolean initialized = false;

	protected Database(Context c) {
		super(c, DATABASE_NAME, null, DATABASE_VERSION);
		if (database == null) database = this;
		initialized = true;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TBL_NASA);
		db.execSQL(CREATE_TBL_SFN);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
		if (oldVer >= newVer) return;
	}
	
	protected static synchronized void closeDb() {
		if (database != null) database.close();
	}
	
	protected static synchronized void clearEvents(String table) {
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(table, null, null);
		db.close();
	}
	
	protected static synchronized ArrayList<Event> getEvents(String table) {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor c = db.query(table, null, null, null, null, null, null);
		ArrayList<Event> events = new ArrayList<Event>();
		while (c.moveToNext()) {
			Event event = new Event();
			event.setMission(c.getString(c.getColumnIndex(MISSION)));
			event.setVehicle(c.getString(c.getColumnIndex(VEHICLE)));
			event.setLocation(c.getString(c.getColumnIndex(LOCATION)));
			event.setDate(c.getString(c.getColumnIndex(DATE)));
			event.setTime(c.getString(c.getColumnIndex(TIME)));
			event.setDescription(c.getString(c.getColumnIndex(DESCRIPTION)));
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(c.getLong(c.getColumnIndex(CAL)));
			event.setCal(cal);
			event.setCalAccuracy(c.getInt(c.getColumnIndex(CAL_ACC)));
			events.add(event);
		}
		c.close();
		db.close();
		return events;
	}
	
	protected static synchronized void setEvents(String table, ArrayList<Event> events) {
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(table, null, null);
		ContentValues values;
		for (Event event : events) {
			values = new ContentValues();
			values.put(MISSION, event.getMission());
			values.put(VEHICLE, event.getVehicle());
			values.put(LOCATION, event.getLocation());
			values.put(DESCRIPTION, event.getDescription());
			values.put(DATE, event.getDate());
			values.put(TIME, event.getTime());
			values.put(CAL, event.getCal().getTimeInMillis());
			values.put(CAL_ACC, event.getCalAccuracy());
			db.insert(table, null, values);
		}
		db.close();
	}
	
	protected static String getSrcTable(Integer src) {
		return src == List.SRC_NASA ? TBL_NASA : TBL_SFN;
	}

	// Tables
	protected static final String
		TBL_NASA = "nasa",
		TBL_SFN = "spaceflightnow";

	// Columns
	private static final String
		MISSION = "mission",
		VEHICLE = "vehicle",
		LOCATION = "location",
		DESCRIPTION = "description",
		DATE = "date",
		TIME = "time",
		CAL = "calendar",
		CAL_ACC = "calendar_accuracy";
	
	// Commands
	private static final String
		columns =
			MISSION + " text not null, "
			+ VEHICLE + " text not null, "
			+ LOCATION + " text not null, "
			+ DESCRIPTION + " text not null, "
			+ DATE + " text not null, "
			+ TIME + " text not null, "
			+ CAL + " integer, "
			+ CAL_ACC + " integer",
		CREATE_TBL_NASA = "create table if not exists " + TBL_NASA + "(" + BaseColumns._ID
			+ " integer primary key autoincrement, " + columns + ");",
		CREATE_TBL_SFN = "create table if not exists " + TBL_SFN + "(" + BaseColumns._ID
			+ " integer primary key autoincrement, " + columns + ");";
}