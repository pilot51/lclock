package com.pilot51.lclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by jacob_000 on 7/27/2015.
 */
public class NotificationPublisher extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent){
        Log.e("PUBLISH CALLED","");
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra("notification");
        int id = intent.getIntExtra("id", 0);
        notificationManager.notify(id, notification);
    }
}
