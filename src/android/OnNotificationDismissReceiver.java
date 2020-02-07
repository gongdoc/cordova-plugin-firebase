package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class OnNotificationDismissReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    int notificationId = intent.getIntExtra("notificationId", 0);
    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancel(notificationId);
  }
}