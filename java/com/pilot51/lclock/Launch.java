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

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Calendar;

public class Launch implements Parcelable, Serializable{
	private static final long serialVersionUID = 1L;
	private String mission, vehicle, location, date, time, description, day, reminderamount;
	private Calendar cal;
	private int year;
	private boolean hasCal;

	public Launch(){ }

	public void writeToParcel(Parcel pc, int flags){
		pc.writeString(mission);
		pc.writeString(vehicle);
		pc.writeString(location);
		pc.writeString(date);
		pc.writeString(time);
		pc.writeString(description);
		pc.writeInt(hasCal ? 1 : 0);
		pc.writeLong(cal.getTimeInMillis());
	}
	public Launch(Parcel pc){
		mission=pc.readString();
		vehicle=pc.readString();
		location=pc.readString();
		date=pc.readString();
		time=pc.readString();
		description=pc.readString();
		hasCal=pc.readInt()!=0;
		Calendar tempCal = Calendar.getInstance();
		tempCal.setTimeInMillis(pc.readLong());
		cal=(Calendar)tempCal.clone();
	}

	public static final Parcelable.Creator<Launch> CREATOR = new Parcelable.Creator<Launch>() {
		public Launch createFromParcel(Parcel pc) {
			return new Launch(pc);
		}
		public Launch[] newArray(int size) {
			return new Launch[size];
		}
	};
	public int describeContents() {
		return 0;
	}
	
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

	boolean hasCal() { return hasCal; }

	void setHasCal(boolean hasCal){ this.hasCal=hasCal; }

}
