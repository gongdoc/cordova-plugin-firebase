package org.apache.cordova.firebase;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class PushWakeLock {

    private static final String TAG = PushWakeLock.class.getSimpleName();

    private static PowerManager.WakeLock wakeLock;

    public static void acquireWakeLock(Context context) {
        synchronized (PushWakeLock.class) {
            Log.d(TAG, "Byzin 15");
            if (wakeLock != null) {
                Log.d(TAG, "Byzin 16");
                // Lock을 Acquire한 상태라면
                if (wakeLock.isHeld()) {
                    Log.d(TAG, "Byzin 17");
                    try { // 추가 예외 처리
                    Log.d(TAG, "Byzin 18");
                        wakeLock.release();
                    } catch(Exception ex) {
                        ;
                    }
                }
                Log.d(TAG, "Byzin 28");
                wakeLock = null;
            }

            Log.d(TAG, "Byzin 32");

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        }

        wakeLock.acquire(6000);
    }

    public static void releaseWakeLock() {
        synchronized (PushWakeLock.class) {
            Log.d(TAG, "Byzin 44");
            if (wakeLock != null) {
                Log.d(TAG, "Byzin 46");
                // Lock을 Acquire한 상태라면
                if (wakeLock.isHeld()) {
                    Log.d(TAG, "Byzin 49");
                    try {
                        Log.d(TAG, "Byzin 51");
                        wakeLock.release();
                    } catch(Exception ex) {
                        ;
                    }
                }

                wakeLock = null;
            }
        }
    }

    public static boolean isScreenOn(Context context) {
        Log.d(TAG, "Byzin 64");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

}
