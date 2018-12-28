package org.apache.cordova.firebase;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;


public class OverlayService extends Service {

    private static final String TAG = OverlayService.class.getSimpleName();

    WindowManager windowManager;
    View view;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showDialog(intent.getExtras());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerOverlayReceiver();
    }

    @Override
    public void onDestroy() {
        hideDialog();
        unregisterOverlayReceiver();

        super.onDestroy();
    }

    private void registerOverlayReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(overlayReceiver, filter);
    }

    private void unregisterOverlayReceiver() {
        unregisterReceiver(overlayReceiver);
    }

    private BroadcastReceiver overlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Bundle data = intent.getExtras();

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "BroadcastReceiver SCREEN_ON");
                showDialog(data);
            }
            else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                hideDialog();
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                hideDialog();
            }
        }
    };

    private void showDialog(Bundle bundle) {
        if (view != null) return;

        final Bundle data = bundle;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutId = getResources().getIdentifier("fragment_overlay", "layout", getPackageName());
        view = View.inflate(getApplicationContext(), layoutId, null);
        view.setTag(TAG);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideDialog();
                return true;
            }
        });

        Drawable background = view.getBackground();
        background.setAlpha(192);

        int dialogId = getResources().getIdentifier("dialog", "id", getPackageName());
        RelativeLayout dialog = view.findViewById(dialogId);
        dialog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideDialog();
                startActivity(data);
                return true;
            }
        });

        dialog.setFocusableInTouchMode(true);
        dialog.requestFocus();
        dialog.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    hideDialog();
                    return true;
                } else {
                    return false;
                }
            }
        });

        int titleId = getResources().getIdentifier("textTitle", "id", getPackageName());
        TextView titleText = view.findViewById(titleId);
        titleText.setText(bundle.getString("workAddress"));

        int contentId = getResources().getIdentifier("textContent", "id", getPackageName());
        TextView contentText = view.findViewById(contentId);
        String content = bundle.getString("workType");
        content += "(" + bundle.getString("workEquipments") + ")";
        content += "\n" + bundle.getString("workDate");
        content += "\n" + bundle.getString("workPayTime");

        String payPerDay = bundle.getString("workPayPerDay");
        if (payPerDay != null) content += "\n" + payPerDay;

        String pickupPosition = bundle.getString("workPickupPosition");
        if (pickupPosition != null) content += "\n" + pickupPosition;

        String requestText = bundle.getString("workRequestText");
        if (requestText != null) content += "\n" + requestText;

        String attachments = bundle.getString("workAttachments");
        if (attachments != null) content += "\n" + attachments;

        contentText.setText(content);
        contentText.setMovementMethod(new ScrollingMovementMethod());

        int okId = getResources().getIdentifier("buttonOk", "id", getPackageName());
        Button buttonOk = view.findViewById(okId);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button OK clicked");

                hideDialog();
                Bundle dataChanged = data;
                dataChanged.putString("link", "/my-order-bids/new/" + data.getString("workId"));
                startActivity(data);
            }
        });

        int cancelId = getResources().getIdentifier("buttonCancel", "id", getPackageName());
        Button buttonCancel = view.findViewById(cancelId);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Cancel clicked");

                hideDialog();
                stopSelf();
            }
        });

        WindowManager.LayoutParams layoutParams;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "Build version low");

            layoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
        } else {
            Log.d(TAG, "Build version high");

            layoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
        }

        view.setVisibility(View.VISIBLE);
        windowManager.addView(view, layoutParams);
        windowManager.updateViewLayout(view, layoutParams);

        try {
            Uri soundPath = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            String sound = bundle.getString("sound");
            if (sound != null) {
                Log.d(TAG, "sound before path is: " + sound);
                soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/raw/" + sound);
                Log.d(TAG, "Parsed sound is: " + soundPath.toString());
            } else {
                Log.d(TAG, "Sound was null ");
            }

            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundPath);
            ringtone.play();
        } catch (Exception ex) {
            Log.d(TAG, "Sound file load failed");
        }
    }

    private void hideDialog() {
        if (view != null && windowManager != null) {
            Log.d(TAG, "Dialog removed");

            view.setVisibility(View.INVISIBLE);
            view.invalidate();
            windowManager.removeView(view);
            view = null;
        }
    }

    private void startActivity(Bundle data) {
        final String packageName = "kr.co.gongdoc.mobile";
        final String className = "MainActivity";

        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(new ComponentName(packageName, packageName + "." + className));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        FirebasePlugin.sendNotification(data, getApplicationContext());
        intent.putExtras(data);

        startActivity(intent);
    }
}
