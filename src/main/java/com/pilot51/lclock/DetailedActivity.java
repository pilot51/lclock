package com.pilot51.lclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.Calendar;

/**
 * Created by jacob_000 on 7/23/2015.
 */
public class DetailedActivity extends AppCompatActivity {
    private Launch launch;
    private CountDownTimer countDownTimer;
    private String imgFalcon9 = "http://i.imgur.com/xYXarSa.jpg",
            imgSoyuz = "http://i.imgur.com/dPKLDG1.jpg",
            imgH2B = "http://i.imgur.com/bar7eAS.jpg",
            imgLongMarch = "http://i.imgur.com/ouTHTvc.jpg",
            imgDelta4 = "http://i.imgur.com/OkWUZvb.jpg",
            imgPSLV = "http://i.imgur.com/tJQguXE.jpg",
            imgProton = "http://i.imgur.com/LVZh3te.jpg",
            imgGSLV = "http://i.imgur.com/wN5kC9U.jpg",
            imgAtlas5 = "http://i.imgur.com/GJt4xBO.jpg",
            imgAriane5 = "http://i.imgur.com/sWDV4kh.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed);
        Intent i = getIntent();
        launch = i.getParcelableExtra("LAUNCH_OBJ");
        i.removeExtra("LAUNCH_OBJ");
        fillView();

    }

    public void fillView() {
        TextView title = (TextView) findViewById(R.id.dcard_title);
        TextView subtitle = (TextView) findViewById(R.id.dcard_subtitle);
        ImageView imageView = (ImageView) findViewById(R.id.dcard_img);
        TextView details = (TextView) findViewById(R.id.dcard_details);
        TextView countdown = (TextView) findViewById(R.id.dcard_countdown);
        final Button reminderButt = (Button) findViewById(R.id.dcard_reminderButt);

        if (!launch.hasCal()) {
            reminderButt.setVisibility(View.GONE);
            countdown.setVisibility(View.GONE);
        } else if (launch.getCal().getTimeInMillis() > System.currentTimeMillis()) {
            setCountdown(countdown);
        }

        reminderButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reminderPopup();
            }
        });

        String detailsStr = "<b>When: </b>" + launch.getDate() + " at " + launch.getTime()
                + "<br><b>Location: </b>" + launch.getLocation() + "</br>"
                + "<br></br><br>" + launch.getDescription() + "</br>";
        details.setText(Html.fromHtml(detailsStr));
        title.setText(launch.getMission());
        subtitle.setText(launch.getVehicle());

        String r = launch.getVehicle();
        if (r.contains("Falcon 9"))
            Glide.with(this).load(imgFalcon9).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Soyuz"))
            Glide.with(this).load(imgSoyuz).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("H-2"))
            Glide.with(this).load(imgH2B).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Long March"))
            Glide.with(this).load(imgLongMarch).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Delta 4"))
            Glide.with(this).load(imgDelta4).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("PSLV"))
            Glide.with(this).load(imgPSLV).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Proton"))
            Glide.with(this).load(imgProton).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("GSLV"))
            Glide.with(this).load(imgGSLV).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Atlas 5"))
            Glide.with(this).load(imgAtlas5).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else if (r.contains("Ariane 5"))
            Glide.with(this).load(imgAriane5).placeholder(R.drawable.placeholder).error(R.drawable.defaultimg).crossFade().into(imageView);
        else
            Glide.with(this).load(R.drawable.defaultimg).placeholder(R.drawable.placeholder).crossFade().into(imageView);
    }

    public void reminderPopup() {
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.dcard_reminderButt));
        popupMenu.inflate(R.menu.reminder_popup);
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                long futureInMillis = launch.getCal().getTimeInMillis() - System.currentTimeMillis() + SystemClock.elapsedRealtime();
                if (item.getTitle().toString().contains("5")) futureInMillis -= 1000 * 60 * 5;
                if (item.getTitle().toString().contains("10")) futureInMillis -= 1000 * 60 * 10;
                if (item.getTitle().toString().contains("20")) futureInMillis -= 1000 * 60 * 20;
                if (item.getTitle().toString().contains("30")) futureInMillis -= 1000 * 60 * 30;
                if (item.getTitle().toString().contains("1 Hour")) futureInMillis -= 1000 * 60 * 60;
                int notificationId = launch.getCal().get(Calendar.DAY_OF_YEAR) + launch.getCal().get(Calendar.HOUR) + launch.getCal().get(Calendar.MINUTE);
                setNotification(futureInMillis, notificationId, item.getTitle().toString());
                Toast.makeText(getApplicationContext(), "Reminder set for " + item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

    }

    public void setCountdown(final TextView countdown) {
        final DecimalFormat df = new DecimalFormat("00");
        final long millisToLaunch = launch.getCal().getTimeInMillis() - System.currentTimeMillis();
        countDownTimer = new CountDownTimer(millisToLaunch, 1000) {
            public void onTick(long millisUntilFinished) {
                int days = (int) (millisUntilFinished / 1000 / 60 / 60 / 24);
                millisUntilFinished %= 1000 * 60 * 60 * 24;
                int hours = (int) (millisUntilFinished / 1000 / 60 / 60);
                millisUntilFinished %= 1000 * 60 * 60;
                int minutes = (int) (millisUntilFinished / 1000 / 60);
                millisUntilFinished %= 1000 * 60;
                int seconds = (int) (millisUntilFinished / 1000);
                String timeRemaining = "";
                if (days > 0) timeRemaining += df.format(days) + ":";
                timeRemaining += df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
                countdown.setText(Html.fromHtml("<b>T-</b>" + timeRemaining));
            }

            @Override
            public void onFinish() {
                countdown.setVisibility(View.GONE);
            }
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (launch.hasCal())
            countDownTimer.cancel();
    }

    public void setNotification(long futureInMillis, int id, String when) {
        boolean vibrate = MainActivity.prefs.getBoolean("pref_vibrate", true);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentTitle("Upcoming launch");
        builder.setContentText(launch.getMission() + " will launch in " + when);
        builder.setSmallIcon(R.drawable.icon);
        if (vibrate)
            builder.setVibrate(new long[]{0, 250, 250, 250});
        builder.setWhen(System.currentTimeMillis());
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0));
        Intent notificationIntent = new Intent(getApplicationContext(), NotificationPublisher.class);
        notificationIntent.putExtra("notification", builder.build());
        notificationIntent.putExtra("id", id);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detailed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_map:
                startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + launch.getLocation())));
                return true;
        }
        return false;
    }
}