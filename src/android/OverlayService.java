package org.apache.cordova.firebase;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


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
        Log.d(TAG, "OverlayService onStartCommand called");

        String title = intent.getExtras().getString("title");
        String text = intent.getExtras().getString("text");

        Log.d(TAG, "title: " + title);
        Log.d(TAG, "text: " + text);

        showDialog(title, text);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OverlayService onCreate called");

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
            Log.d(TAG, "[onReceive]" + action);

            String title = intent.getExtras().getString("title");
            String text = intent.getExtras().getString("text");

            Log.d(TAG, "title: " + title);
            Log.d(TAG, "text: " + text);

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "BroadcastReceiver SCREEN_ON");
                showDialog(title, text);
            }
            else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                hideDialog();
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                hideDialog();
            }
        }
    };

    private void showDialog(String title, String text) {
        Log.d(TAG, "showDialog begin");

        if (view != null) {
            Log.d(TAG, "Dialog already shown");
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutId = getResources().getIdentifier("fragment_overlay", "layout", getPackageName());
        view = View.inflate(getApplicationContext(), layoutId, null);
        view.setTag(TAG);

        int titleId = getResources().getIdentifier("textTitle", "id", getPackageName());
        TextView titleText = view.findViewById(titleId);
        titleText.setText(title);

        int contentId = getResources().getIdentifier("textContent", "id", getPackageName());
        TextView contentText = view.findViewById(contentId);
        contentText.setText(text);

        int okId = getResources().getIdentifier("buttonOk", "id", getPackageName());
        Button buttonOk = view.findViewById(okId);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button OK clicked");

                final String packageName = "kr.co.gongdoc.mobile";
                final String className = "MainActivity";

                hideDialog();

                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName(packageName, packageName + "." + className));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
        } else {
            Log.d(TAG, "Build version high");

            layoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
        }

        view.setVisibility(View.VISIBLE);
        windowManager.addView(view, layoutParams);
        windowManager.updateViewLayout(view, layoutParams);
    }

    private void hideDialog() {
        if (view != null && windowManager != null) {
            Log.d(TAG, "Dialog removed");

            view.setVisibility(View.INVISIBLE);
            view.invalidate();
            windowManager.removeView(view);
            view = null;
        } else {
            Log.d(TAG, "Dialog NOT removed");
        }
    }
}
