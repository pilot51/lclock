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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListAdapter extends BaseAdapter {
	private final ArrayList<Event> list = new ArrayList<Event>();
	private LayoutInflater mInflater;

	public ListAdapter(Context c, ArrayList<Event> list) {
		mInflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setData(list);
	}
	
	protected void setData(ArrayList<Event> list) {
		this.list.clear();
		this.list.addAll(list);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (convertView == null) view = mInflater.inflate(R.layout.grid, parent, false);
		((TextView)view.findViewById(R.id.mission)).setText(list.get(position).getMission());
		((TextView)view.findViewById(R.id.vehicle)).setText(list.get(position).getVehicle());
		((TextView)view.findViewById(R.id.location)).setText(list.get(position).getLocation());
		((TextView)view.findViewById(R.id.date)).setText(list.get(position).getDate());
		((TextView)view.findViewById(R.id.time)).setText(list.get(position).getTime());
		((TextView)view.findViewById(R.id.description)).setText(list.get(position).getDescription());
		return view;
	}
}
