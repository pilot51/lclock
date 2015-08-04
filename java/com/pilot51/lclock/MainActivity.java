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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements Serializable {

    private RecyclerView rv;
    private CustomAdapter adapter;
    private List<Launch> launchList;
    private ProgressBar progressBar;
    public static Context context;
    public static SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        MainActivity.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        MainActivity.context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.refresh_spinner);
        launchList = new ArrayList<>();
        buildListView();
        loadData();
        new DownloadFile().execute("http://spaceflightnow.com/launch-schedule/");
        /*if(BuildConfig.DEBUG){
			Launch test = new Launch();
			test.setMission("Test Launch");
			test.setTime("SOMETHING");
			test.setHasCal(true);
			test.setDate("today");
			test.setDescription("BLAH LOREM IPSUM");
			test.setLocation("earth");
			test.setVehicle("Falco");
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE)+21);
			test.setCal(cal);
			launchList.add(0, test);
			adapter.setList(launchList);
			(rv.getAdapter()).notifyDataSetChanged();
		} */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.force_refresh:
                new DownloadFile().execute("http://spaceflightnow.com/launch-schedule/");
                return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            FileOutputStream fileOut = new FileOutputStream(new File(this.getFilesDir(), "Launch Data"));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(launchList);
            out.close();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        try {
            FileInputStream fileIn = new FileInputStream(new File(this.getFilesDir(), "Launch Data"));
            ObjectInputStream in = new ObjectInputStream(fileIn);
            launchList = (ArrayList<Launch>) in.readObject();
            in.close();
            fileIn.close();
            adapter.setList(launchList);
            (rv.getAdapter()).notifyDataSetChanged();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void toast(final String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private void buildListView() {
        adapter = new CustomAdapter(launchList, MainActivity.this);
        rv = (RecyclerView) findViewById(R.id.list);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);
        rv.setAdapter(adapter);
    }

    public class DownloadFile extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream input;
            String strdata = null;
            StringBuilder stringBuilder = new StringBuilder();
            int count;
            try {
                URL url = new URL(params[0]);
                // download the file
                input = new BufferedInputStream(url.openStream());
                byte data[] = new byte[1024];
                while ((count = input.read(data)) != -1) {
                    stringBuilder.append(new String(data, 0, count));
                }
                strdata = stringBuilder.toString();
                strdata = strdata.replaceAll("\r\n", "\n");
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return strdata;
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {

        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    launchList.clear();
                    launchList = DataParser.parseData(result);
                    adapter.setList(launchList);
                    (rv.getAdapter()).notifyDataSetChanged();
                } catch (Exception e) {
                    toast("Error parsing file");
                    loadData();

                    e.printStackTrace();
                }
            } else if (launchList.isEmpty())
                toast("Error downloading file");
            else
                toast("Could not refresh");
            progressBar.setVisibility(View.GONE);
            super.onPostExecute(result);
        }
    }
}