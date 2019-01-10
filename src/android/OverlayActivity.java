package org.apache.cordova.firebase;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
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

public class OverlayActivity extends Activity {

    private static final String TAG = OverlayActivity.class.getSimpleName();

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
        dialog.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                finish();
                // startActivity(data);
                return true;
            }
        });

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

                final String packageName = "kr.co.gongdoc.mobile";
                final String className = "MainActivity";

                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName(packageName, packageName + "." + className));
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                Bundle dataChanged = bundle;
                dataChanged.putString("link", "/my-order-bids/new/" + bundle.getString("workId"));
                intent.putExtras(dataChanged);

                startActivity(intent);
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


        PushWakeLock.acquireWakeLock(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        PushWakeLock.releaseWakeLock();

        super.onDestroy();
    }
}
