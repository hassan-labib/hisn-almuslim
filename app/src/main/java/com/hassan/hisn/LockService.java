package com.hassan.hisn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class LockService extends Service {
    static final Set<String> ALLOWED = new HashSet(Arrays.asList("com.hassan.hisn", "com.android.systemui", "com.android.phone", "com.android.server.telecom", "com.android.incallui", "com.google.android.dialer", "com.samsung.android.incallui", "com.samsung.android.dialer", "com.android.dialer", "com.android.emergency", "com.android.permissioncontroller", "com.google.android.permissioncontroller", "com.samsung.android.app.telephonyui", "com.miui.securitycenter"));
    Handler h = new Handler(Looper.getMainLooper());
    String fg = null;
    long lastQuery = 0;
    final Runnable tick = new Runnable() { // from class: com.hassan.hisn.LockService.1
        @Override // java.lang.Runnable
        public void run() {
            if (!LockController.isActive(LockService.this)) {
                LockService.this.stopSelf();
            } else {
                LockService.this.poll();
                LockService.this.h.postDelayed(this, 600L);
            }
        }
    };

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            notificationManager.createNotificationChannel(new NotificationChannel("lock", L.s("قفل الصلاة"), 2));
        }
        Notification build = (Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "lock") : new Notification.Builder(this)).setSmallIcon(R.drawable.ic_stat).setContentTitle(L.s("قفل الصلاة شغال")).setContentText(L.s("صوّر سجادة الصلاة لفتح الهاتف")).setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, (Class<?>) LockActivity.class), 67108864)).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(7, build, 1073741824);
        } else {
            startForeground(7, build);
        }
        this.h.removeCallbacks(this.tick);
        this.h.post(this.tick);
        return 1;
    }

    void poll() {
        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService("usagestats");
            long currentTimeMillis = System.currentTimeMillis();
            long j = this.lastQuery;
            UsageEvents queryEvents = usageStatsManager.queryEvents(j == 0 ? currentTimeMillis - LockController.MAX_LOCK_MS : j - 1000, currentTimeMillis);
            UsageEvents.Event event = new UsageEvents.Event();
            while (queryEvents.hasNextEvent()) {
                queryEvents.getNextEvent(event);
                if (event.getEventType() == 1) {
                    this.fg = event.getPackageName();
                }
            }
            this.lastQuery = currentTimeMillis;
        } catch (Exception unused) {
        }
        if (this.fg == null || System.currentTimeMillis() < Store.p(this).getLong("capture_until", 0L) || ALLOWED.contains(this.fg)) {
            return;
        }
        LockController.launchLock(this);
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.h.removeCallbacks(this.tick);
        super.onDestroy();
    }
}
