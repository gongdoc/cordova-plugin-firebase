package org.apache.cordova.firebase;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ExitActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    public static void exitApp(Context context, Boolean removeFromRecents) {
        Intent intent;

        if (removeFromRecents) {
            intent = new Intent(context, ExitActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        } else {
            final String packageName = "kr.co.gongdoc.mobile";
            final String className = "MainActivity";

            intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(packageName, packageName + "." + className));

            intent.addCategory( Intent.CATEGORY_HOME );
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            Bundle bundle = new Bundle();
            bundle.putBoolean("cdvStartInBackground", true);
            intent.putExtras(bundle);
        }

        context.startActivity(intent);
    }
}
