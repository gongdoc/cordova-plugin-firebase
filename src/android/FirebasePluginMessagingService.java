package org.apache.cordova.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.app.Notification;
import android.text.TextUtils;
import android.content.ContentResolver;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    private static String lastId;

    /**
     * Get a string from resources without importing the .R package
     *
     * @param name Resource Name
     * @return Resource
     */
    private String getStringResource(String name) {
        return this.getString(
                this.getResources().getIdentifier(
                        name, "string", this.getPackageName()
                )
        );
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Pass the message to the receiver manager so any registered receivers can decide to handle it
        boolean wasHandled = FirebasePluginMessageReceiverManager.onMessageReceived(remoteMessage);
        if (wasHandled) {
            Log.d(TAG, "Message was handled by a registered receiver");

            // Don't process the message in this method.
            return;
        }

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        String flagWakeUp = "";
        String flagPush = "";
        String title = "";
        String text = "";
        String id = "";
        String wakeUp = "";
        String sound = "";
        String lights = "";
        Map<String, String> data = remoteMessage.getData();

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            text = remoteMessage.getNotification().getBody();
            id = remoteMessage.getMessageId();
        } else if (data != null) {
            flagWakeUp = data.get("flagWakeUp");
            flagPush = data.get("flagPush");
            title = data.get("title");
            text = data.get("text");
            id = data.get("id");
            wakeUp = data.get("wakeUp");
            sound = data.get("sound");
            lights = data.get("lights"); //String containing hex ARGB color, miliseconds on, miliseconds off, example: '#FFFF00FF,1000,3000'

            if (TextUtils.isEmpty(text)) {
                text = data.get("body");
            }
        }

        if (TextUtils.isEmpty(id)) {
            Random rand = new Random();
            int n = rand.nextInt(50) + 1;
            id = Integer.toString(n);
        }

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message flagWakeUp: " + flagWakeUp);
        Log.d(TAG, "Notification Message flagPush: " + flagPush);
        Log.d(TAG, "Notification Message id: " + id);
        Log.d(TAG, "Notification Message Title: " + title);
        Log.d(TAG, "Notification Message Body/Text: " + text);
        Log.d(TAG, "Notification Message WakeUp: " + wakeUp);
        Log.d(TAG, "Notification Message Sound: " + sound);
        Log.d(TAG, "Notification Message Lights: " + lights);

        // TODO: Add option to developer to configure if show notification when app on foreground
        Context context = this.getApplicationContext();

        if (flagWakeUp.equals("X")) {
            if (id.equals(FirebasePluginMessagingService.lastId)) {
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setClass(context, OverlayActivity.class);

                Bundle bundle = new Bundle();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    bundle.putString(entry.getKey(), entry.getValue());
                }
                intent.putExtras(bundle);

                startActivity(intent);

                FirebasePluginMessagingService.lastId = "";
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(id.hashCode());
            }

            return;
        }

        if (flagWakeUp.equals("Y") && wakeUp != null && wakeUp.equals("Y")) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            if (!notificationManagerCompat.areNotificationsEnabled()) return;

            boolean showNotification = (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(title));
            if (!showNotification) return;

            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setClass(context, OverlayActivity.class);

            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }

            PowerManager powerManager = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isInteractive()) {
                bundle.putString("screen", "on");
            } else {
                bundle.putString("screen", "off");
            }

            intent.putExtras(bundle);

            if (flagPush.equals("N")) {
                try {
                    final AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        int ringerMode = audioManager.getRingerMode();
                        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                            Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/gongdoc.mp3");

                            final int maxVolumeMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            final int volumeMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            int maxVolumeNotification = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                            int volumeNotification = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

                            int volume = volumeNotification * maxVolumeMusic / maxVolumeNotification;
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

                            final MediaPlayer mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(getApplicationContext(), soundPath);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                public void onCompletion(MediaPlayer mp) {
                                    mediaPlayer.release();
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeMusic, 0);
                                }
                            });
                        }

                        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                            long[] defaultVibration = new long[] { 0, 280, 250, 280, 250 };
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if (vibrator != null && vibrator.hasVibrator()) {
                                if (android.os.Build.VERSION.SDK_INT >= 26) {
                                    vibrator.vibrate(VibrationEffect.createWaveform(defaultVibration, -1));
                                } else {
                                    vibrator.vibrate(defaultVibration, -1);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "Sound file load failed");
                }
            }

            startActivity(intent);

/*
            Intent intent = new Intent(context, OverlayService.class);
            intent.setAction(Intent.ACTION_SCREEN_ON);
            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
            intent.putExtras(bundle);
            context.startService(intent);
*/

            // save id
            FirebasePluginMessagingService.lastId = id;
        }

        if (flagPush.equals("Y") && (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(title) || !data.isEmpty())) {
            PushWakeLock.acquireWakeLock(getApplicationContext());

            boolean showNotification = (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback()) && (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(title));
            sendNotification(id, title, text, data, showNotification, sound, lights);

            PushWakeLock.releaseWakeLock();
        }
    }

    private void sendNotification(String id, String title, String messageBody, Map<String, String> data, boolean showNotification, String sound, String lights) {
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            bundle.putString(key, data.get(key));
        }

        if (showNotification) {
            Intent intent = new Intent(this, OnNotificationOpenReceiver.class);
            intent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            String groupId = getPackageName() + ".NOTIFICATIONS";

            String channelId = this.getStringResource("default_notification_channel_id");
            String channelName = this.getStringResource("default_notification_channel_name");
            // Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Uri defaultSoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/gongdoc.mp3");

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);

            int contentViewId = getResources().getIdentifier("notification", "layout", getPackageName());
            RemoteViews contentView = new RemoteViews(getPackageName(), contentViewId);

            int bigContentViewId = getResources().getIdentifier("notification_expanded", "layout", getPackageName());
            RemoteViews bigContentView = new RemoteViews(getPackageName(), bigContentViewId);

            int titleId = getResources().getIdentifier("notificationTitle", "id", getPackageName());
            int contentId = getResources().getIdentifier("notificationContent", "id", getPackageName());

            if (bundle.getString("type").equals("register")) {
                String titleMessage = bundle.getString("workAddress");
                String contentMessage = bundle.getString("workType") + "(" + bundle.getString("workEquipments") + ") - " + bundle.getString("workDate");
                contentView.setTextViewText(titleId, titleMessage);
                contentView.setTextViewText(contentId, contentMessage);
            } else {
                contentView.setTextViewText(titleId, title);
                contentView.setTextViewText(contentId, messageBody);
            }

            bigContentView.setTextViewText(titleId, title);
            bigContentView.setTextViewText(contentId, messageBody);

            notificationBuilder
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setGroup(groupId)
                    .setGroupSummary(true)
                    .setCustomContentView(contentView)
                    .setCustomBigContentView(bigContentView)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(defaultVibration)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX);

            int resID = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
            if (resID != 0) {
                notificationBuilder.setSmallIcon(resID);
            } else {
                notificationBuilder.setSmallIcon(getApplicationInfo().icon);
            }

            if (lights != null) {
                try {
                    String[] lightsComponents = lights.replaceAll("\\s", "").split(",");
                    if (lightsComponents.length == 3) {
                        int lightArgb = Color.parseColor(lightsComponents[0]);
                        int lightOnMs = Integer.parseInt(lightsComponents[1]);
                        int lightOffMs = Integer.parseInt(lightsComponents[2]);

                        notificationBuilder.setLights(lightArgb, lightOnMs, lightOffMs);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Lights set failed");
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int accentID = getResources().getIdentifier("primary", "color", getPackageName());
                notificationBuilder.setColor(getResources().getColor(accentID, null));
            }

            Uri soundPath = defaultSoundUri;
            if (sound != null) {
                // soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                notificationBuilder.setSound(soundPath);
            } else {
                Log.d(TAG, "Sound was null ");
                // notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
                notificationBuilder.setSound(soundPath);
            }

            long[] defaultVibration = new long[] { 0, 280, 250, 280, 250 };
            AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int ringerMode = audioManager.getRingerMode();
                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    notificationBuilder.setVibrate(defaultVibration);
                }
            }

            Notification notification = notificationBuilder.build();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int iconID = android.R.id.icon;
                int notiID = getResources().getIdentifier("ic_notification", "drawable", getPackageName());
                if (notification.contentView != null) {
                    notification.contentView.setImageViewResource(iconID, notiID);
                }
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Since android Oreo notification channel is needed.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);

                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    channel.setSound(soundPath, attributes);
                    // if (sound != null) {
                    //     channel.setSound(soundPath, attributes);
                    // } else {
                    //     // Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    //     Uri uri= Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/gongdoc.mp3");
                    //     channel.setSound(uri, attributes);
                    // }

                    notificationManager.createNotificationChannel(channel);
                }

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    if (audioManager != null) {
                        int ringerMode = audioManager.getRingerMode();
                        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                            if (channel.shouldVibrate()) {
                                channel.setVibrationPattern(defaultVibration);
                            }
                        }
                    }
                }

                notificationManager.notify(id.hashCode(), notification);
            }
        } else {
            bundle.putBoolean("tap", false);
            bundle.putString("title", title);
            bundle.putString("body", messageBody);
            FirebasePlugin.sendNotification(bundle, this.getApplicationContext());
        }
    }
}