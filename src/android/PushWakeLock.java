package org.apache.cordova.firebase;

import android.content.Context;
import android.os.PowerManager;

public class PushWakeLock {

    private static final String TAG = PushWakeLock.class.getSimpleName();

    private static PowerManager.WakeLock wakeLock;

    public static void acquireWakeLock(Context context) {
        synchronized (PushWakeLock.class) {
            if (wakeLock != null) {
                // Lock을 Acquire한 상태라면
                if (wakeLock.isHeld()) {
                    try { // 추가 예외 처리
                        wakeLock.release();
                    } catch(Exception ex) {
                        ;
                    }
                }
                wakeLock = null;
            }

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        }

        wakeLock.acquire(6000);
    }

    public static void releaseWakeLock() {
        synchronized (PushWakeLock.class) {
            if (wakeLock != null) {
                // Lock을 Acquire한 상태라면
                if (wakeLock.isHeld()) {
                    try {
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
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

}