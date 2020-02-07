package org.apache.cordova.firebase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class OverlayActivity extends Activity {

    private static final String TAG = "FirebasePlugin";

    private int CLICK_TIME_THRESHOLD = 200;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getIntent().getExtras();
        if (bundle.getString("flagWakeUp").equals("X")) {
            exit();
            return;
        }

        PushWakeLock.acquireWakeLock(getApplicationContext());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        View view;
        int layoutId = getResources().getIdentifier("fragment_overlay", "layout", getPackageName());
        view = View.inflate(getApplicationContext(), layoutId, null);
        view.setTag(TAG);

        Drawable background = view.getBackground();
        if (bundle.getString("screen").equals("on")) {
            background.setAlpha(128);
        } else {
            background.setAlpha(255);
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                exit();
                return true;
            }
        });

        setContentView(view);

        int textBgId = getResources().getIdentifier("textBg", "id", getPackageName());
        TextView textBg = view.findViewById(textBgId);
        textBg.setText("알림창 외에 터치하시면\n사라집니다.");

        int dialogId = getResources().getIdentifier("dialog", "id", getPackageName());
        RelativeLayout dialog = view.findViewById(dialogId);

        dialog.setFocusableInTouchMode(true);
        dialog.requestFocus();
        dialog.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    exit();
                    return true;
                } else {
                    return false;
                }
            }
        });

        int titleId = getResources().getIdentifier("textTitle", "id", getPackageName());
        final TextView titleText = view.findViewById(titleId);
        titleText.setText(bundle.getString("workAddress"));

        titleText.setOnTouchListener(new View.OnTouchListener() {
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        ((TextView)v).setTextColor(0xffcccccc);
                        // v.getBackground().setColorFilter(0xff999999, PorterDuff.Mode.MULTIPLY);
                        v.invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        //v.getBackground().clearColorFilter();
                        ((TextView)v).setTextColor(0xffffffff);
                        v.invalidate();

                        if (endTime - startTime < CLICK_TIME_THRESHOLD) {
                            bundle.putString("link", "/orders/view/" + bundle.getString("workId"));
                            startActivity(bundle);
                            return true;
                        }
                        break;

                }

                return false;
            }
        });


        String webViewStyle = "@font-face { font-family:noto_sans;src:url('font/noto_sans_kr_regular.otf'); }\n"
                + "body { padding:10px;font-family:noto_sans, sans-serif;font-size:16px; }"
                + "h3 { margin:0;margin-bottom:10px;font-size:1.5em; }\n"
                + "ul { margin:0;padding:0 0 0 5px;list-style:none; }\n"
                + "li { padding-left:20px;color:#000;font-size:1.2em;line-height:1.5;background:url('drawable/ic_bullet_triangle.png') no-repeat 0 5px;background-size:20px; }\n";
        String webViewData = "<html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1,user-scalable=yes\">"
                + "<style type=\"text/css\">"
                + webViewStyle
                + "</style></head><body>";

        webViewData += "<h3>" + bundle.getString("workType") + "(" + bundle.getString("workEquipments") + ")</h3>";

        webViewData += "<ul>";
        webViewData += "<li>" + bundle.getString("workAddress") + "</li>";
        webViewData += "<li>" + bundle.getString("workDate") + "</li>";
        webViewData += "<li>" + bundle.getString("workPayTime") + "</li>";
        String payPerDay = bundle.getString("workPayPerDay");
        if (payPerDay != null) {
            webViewData += "<li>" + payPerDay + "</li>";
        }

        String pickupPosition = bundle.getString("workPickupPosition");
        if (pickupPosition != null) {
            webViewData += "<li>" + pickupPosition + "</li>";
        }

        String requestText = bundle.getString("workRequestText");
        if (requestText != null) {
            webViewData += "<li>" + requestText + "</li>";
        }

        String attachments = bundle.getString("workAttachments");
        if (attachments != null) {
            webViewData += "<li>" + attachments + "</li>";
        }

        webViewData += "</ul></body></html>";

        int contentId = getResources().getIdentifier("textContent", "id", getPackageName());
        WebView contentText = view.findViewById(contentId);

        WebSettings webSettings = contentText.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        webSettings.setTextZoom(100);

        contentText.loadDataWithBaseURL("file:///android_res/", webViewData, "text/html", "UTF-8", null);

        contentText.setOnTouchListener(new View.OnTouchListener() {
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        v.setBackgroundColor(0xffeeeeee);
                        v.invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        v.setBackgroundColor(0xffffffff);
                        v.invalidate();

                        if (endTime - startTime < CLICK_TIME_THRESHOLD) {
                            bundle.putString("link", "/orders/view/" + bundle.getString("workId"));
                            startActivity(bundle);
                            return true;
                        }
                        break;

                }

                return false;
            }
        });

        int okId = getResources().getIdentifier("buttonOk", "id", getPackageName());
        Button buttonOk = view.findViewById(okId);
        buttonOk.setOnTouchListener(new View.OnTouchListener() {

            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        v.getBackground().setColorFilter(0xff999999, PorterDuff.Mode.DARKEN);
                        v.invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        v.getBackground().clearColorFilter();
                        v.invalidate();

                        if (endTime - startTime < CLICK_TIME_THRESHOLD) {
                            bundle.putString("link", "/my-order-bids/new/" + bundle.getString("workId"));
                            startActivity(bundle);
                            return true;
                        }
                        break;

                }

                return false;
            }
        });


        int cancelId = getResources().getIdentifier("buttonCancel", "id", getPackageName());
        ImageButton buttonCancel = view.findViewById(cancelId);
        buttonCancel.setOnTouchListener(new View.OnTouchListener() {
            private long startTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        v.getBackground().setColorFilter(0x99999999, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        long endTime = System.currentTimeMillis();
                        v.getBackground().clearColorFilter();
                        v.invalidate();

                        if (endTime - startTime < CLICK_TIME_THRESHOLD) {
                            exit();
                            return true;
                        }
                        break;

                }

                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        View view;
        int layoutId = getResources().getIdentifier("fragment_overlay", "layout", getPackageName());
        view = View.inflate(getApplicationContext(), layoutId, null);

        int contentId = getResources().getIdentifier("textContent", "id", getPackageName());
        WebView contentText = view.findViewById(contentId);
        contentText.removeAllViews();

        super.onDestroy();

        PushWakeLock.releaseWakeLock();
    }

    private void startActivity(Bundle data) {
        final String packageName = "kr.co.gongdoc.mobile";
        final String className = "MainActivity";

        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(new ComponentName(packageName, packageName + "." + className));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        FirebasePlugin.sendNotification(data, getApplicationContext());
        intent.putExtras(data);

        startActivity(intent);

        String id = data.getString("id");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id.hashCode());

        finish();
    }

    private void exit() {
        if (isTaskRoot()) {
            ExitActivity.exitApp(getApplicationContext());
        } else {
            finish();
        }
    }
}
