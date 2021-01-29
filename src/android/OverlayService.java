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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
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
        if (view != null) {
            hideDialog();
        }

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
        String contentHtml = "<h3>" + bundle.getString("workType") + "(" + bundle.getString("workEquipments") + ")</h3>";
        contentHtml += "<ul style=\"list-style:none;padding-left:10px;\">";
        contentHtml += "<li>" + bundle.getString("workDate") + "</li>";
        contentHtml += "<li>" + bundle.getString("workPayTime") + "</li>";

        String payPerDay = bundle.getString("workPayPerDay");
        if (payPerDay != null) contentHtml += "<li>" + payPerDay + "</li>";

        String pickupPosition = bundle.getString("workPickupPosition");
        if (pickupPosition != null) contentHtml += "<li>" + pickupPosition + "</li>";

        String requestText = bundle.getString("workRequestText");
        if (requestText != null) contentHtml += "<li>" + requestText + "</li>";

        String attachments = bundle.getString("workAttachments");
        if (attachments != null) contentHtml += "<li>" + attachments + "</li>";
        contentHtml += "</ul>";

        contentText.setText(Html.fromHtml(contentHtml));
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
                startActivity(dataChanged);
            }
        });

        int cancelId = getResources().getIdentifier("buttonCancel", "id", getPackageName());
        ImageButton buttonCancel = view.findViewById(cancelId);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Cancel clicked");

                hideDialog();
                stopSelf();
            }
        });

        try {
            Log.d(TAG, "Build.VERSION : " + Build.VERSION.SDK_INT);

            WindowManager.LayoutParams layoutParams;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                        PixelFormat.TRANSLUCENT);
            } else {
                layoutParams = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_TOAST,
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                        PixelFormat.TRANSLUCENT);
            }

            view.setVisibility(View.VISIBLE);
            windowManager.addView(view, layoutParams);
            windowManager.updateViewLayout(view, layoutParams);
        } catch (Exception ex) {
            Log.d(TAG, "Load view failed");
            Log.d(TAG, ex.getMessage());
        }

        try {
            Uri soundPath = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.gongdoc);
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
