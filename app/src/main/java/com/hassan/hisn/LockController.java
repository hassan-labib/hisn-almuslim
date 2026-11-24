package com.hassan.hisn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/* loaded from: classes.dex */
public class LockController {
    public static final long MAX_LOCK_MS = 3600000;
    public static final long TEST_LOCK_MS = 300000;

    public static boolean enabled(Context context) {
        return Store.p(context).getBoolean("lock_on", false);
    }

    public static boolean isActive(Context context) {
        SharedPreferences p = Store.p(context);
        if (!p.getBoolean("lock_active", false)) {
            return false;
        }
        if (System.currentTimeMillis() <= p.getLong("lock_until", 0L)) {
            return true;
        }
        release(context, "expired");
        return false;
    }

    public static void onPrayerTime(Context context, String str) {
        if (enabled(context)) {
            begin(context, str, MAX_LOCK_MS);
        }
    }

    public static void begin(Context context, String str, long j) {
        Store.p(context).edit().putBoolean("lock_active", true).putString("lock_name", str).putLong("lock_until", System.currentTimeMillis() + j).putLong("capture_until", 0L).apply();
        startService(context);
        launchLock(context);
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, (Class<?>) LockService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception unused) {
        }
    }

    public static void launchLock(Context context) {
        try {
            context.startActivity(new Intent(context, (Class<?>) LockActivity.class).addFlags(335609856));
        } catch (Exception unused) {
        }
    }

    public static void release(Context context, String str) {
        Store.p(context).edit().putBoolean("lock_active", false).putLong("capture_until", 0L).putString("lock_last_release", str).apply();
        context.stopService(new Intent(context, (Class<?>) LockService.class));
    }

    public static void resumeIfActive(Context context) {
        if (isActive(context)) {
            startService(context);
        }
    }
}
