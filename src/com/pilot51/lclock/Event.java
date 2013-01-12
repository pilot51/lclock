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

import java.util.Calendar;

public class Event {
	private String mission, vehicle, location, date, time, description, day;
	private Calendar cal;
	private int calAccuracy, year;
	
	String getMission() {
		return mission;
	}

	void setMission(String mission) {
		this.mission = mission;
	}

	String getVehicle() {
		return vehicle;
	}

	void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}

	String getLocation() {
		return location;
	}

	void setLocation(String location) {
		this.location = location;
	}

	String getDate() {
		return date;
	}

	void setDate(String date) {
		this.date = date;
	}

	String getTime() {
		return time;
	}

	void setTime(String time) {
		this.time = time;
	}

	String getDescription() {
		return description;
	}

	void setDescription(String description) {
		this.description = description;
	}

	int getYear() {
		return year;
	}

	void setYear(int year) {
		this.year = year;
	}

	String getDay() {
		return day;
	}

	void setDay(String day) {
		this.day = day;
	}

	Calendar getCal() {
		return cal;
	}

	void setCal(Calendar cal) {
		this.cal = cal;
	}

	int getCalAccuracy() {
		return calAccuracy;
	}

	void setCalAccuracy(int calAccuracy) {
		this.calAccuracy = calAccuracy;
	}
}
