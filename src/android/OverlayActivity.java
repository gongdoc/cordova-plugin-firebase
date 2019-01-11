package org.apache.cordova.firebase;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;

public class OverlayActivity extends Activity {

    private static final String TAG = OverlayActivity.class.getSimpleName();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getIntent().getExtras();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        View view;
        int layoutId = getResources().getIdentifier("fragment_overlay", "layout", getPackageName());
        view = View.inflate(getApplicationContext(), layoutId, null);
        view.setTag(TAG);

        Drawable background = view.getBackground();
        background.setAlpha(208);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                finish();
                return true;
            }
        });

        setContentView(view);

        int dialogId = getResources().getIdentifier("dialog", "id", getPackageName());
        RelativeLayout dialog = view.findViewById(dialogId);

        dialog.setFocusableInTouchMode(true);
        dialog.requestFocus();
        dialog.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
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

        String contentHead = bundle.getString("workType") + "(" + bundle.getString("workEquipments") + ")\n";

        ArrayList<String> contentBody = new ArrayList<String>();
        ArrayList<Integer> contentBodySize = new ArrayList<Integer>();

        String workDate = bundle.getString("workDate") + "\n";
        contentBody.add(workDate);
        contentBodySize.add(workDate.length());

        String workPayTime = bundle.getString("workPayTime") + "\n";
        contentBody.add(workPayTime);
        contentBodySize.add(workPayTime.length());

        String payPerDay = bundle.getString("workPayPerDay");
        if (payPerDay != null) {
            contentBody.add(payPerDay + "\n");
            contentBodySize.add(payPerDay.length() + 1);
        }

        String pickupPosition = bundle.getString("workPickupPosition");
        if (pickupPosition != null) {
            contentBody.add(pickupPosition + "\n");
            contentBodySize.add(pickupPosition.length() + 1);
        }

        String requestText = bundle.getString("workRequestText");
        if (requestText != null) {
            contentBody.add(requestText + "\n");
            contentBodySize.add(requestText.length() + 1);
        }

        String attachments = bundle.getString("workAttachments");
        if (attachments != null) {
            contentBody.add(attachments + "\n");
            contentBodySize.add(attachments.length() + 1);
        }

        String contentAll = contentHead;
        for (String item: contentBody) {
            contentAll = contentAll.concat(item);
        }

        SpannableString contentSpan = new SpannableString(contentAll);
        contentSpan.setSpan(
                new TextAppearanceSpan(null, 0, 100, null, null),
                0, contentHead.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Integer pos = contentHead.length();
        for (Integer item: contentBodySize) {
            contentSpan.setSpan(new BulletSpan(25), pos, pos + item, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos += item;
        }

        contentText.setText(contentSpan, TextView.BufferType.SPANNABLE);
        contentText.setMovementMethod(new ScrollingMovementMethod());

        contentText.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private int CLICK_DIFF_THRESHOLD = 200;
            private long startTime;
            private int CLICK_TIME_THRESHOLD = 100;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        startTime = System.currentTimeMillis();
                        break;

                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        long endTime = System.currentTimeMillis();

                        if (isClick(startX, startY, endX, endY)
                                || (endTime - startTime < CLICK_TIME_THRESHOLD)) {
                            bundle.putString("link", "/orders/view/" + bundle.getString("workId"));
                            startActivity(bundle);
                            return true;
                        }
                        break;

                }

                return false;
            }

            private boolean isClick(float x1, float y1, float x2, float y2) {
                float diffX = Math.abs(x1 - x2);
                float diffY = Math.abs(y1 - y2);

                return !(diffX > CLICK_DIFF_THRESHOLD || diffY > CLICK_DIFF_THRESHOLD);
            }
        });

        int okId = getResources().getIdentifier("buttonOk", "id", getPackageName());
        Button buttonOk = view.findViewById(okId);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button OK clicked");

                bundle.putString("link", "/my-order-bids/new/" + bundle.getString("workId"));
                startActivity(bundle);
            }
        });

        int cancelId = getResources().getIdentifier("buttonCancel", "id", getPackageName());
        ImageButton buttonCancel = view.findViewById(cancelId);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Cancel clicked");

                finish();
            }
        });

        int alarmId = getResources().getIdentifier("buttonAlarm", "id", getPackageName());
        ImageButton buttonAlarm = view.findViewById(alarmId);
        buttonAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Cancel clicked");

                bundle.putString("link", "/alarms");
                startActivity(bundle);
            }
        });

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

        PushWakeLock.acquireWakeLock(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        PushWakeLock.releaseWakeLock();

        super.onDestroy();
    }


    private void startActivity(Bundle data) {
        final String packageName = "kr.co.gongdoc.mobile";
        final String className = "MainActivity";

        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(new ComponentName(packageName, packageName + "." + className));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        FirebasePlugin.sendNotification(data, getApplicationContext());
        intent.putExtras(data);

        finish();
        startActivity(intent);
    }

}
