package com.hassan.hisn;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

/** Alarms for prayers, iqama, daily adhkar, and notification helpers. */
public class Scheduler {
    public static final String CH_ADHKAR = "adhkar", CH_PRAYER = "prayer", CH_IQAMA = "iqama", CH_ADHAN = "adhan_play";
    /** index into PrayerTimes arrays for the five daily prayers */
    public static final int[] PRAYER_IDX = {0, 2, 3, 4, 5};
    public static final String[] PRAYER_AR = {L.s("الفجر"), L.s("الظهر"), L.s("العصر"), L.s("المغرب"), L.s("العشاء")};

    public static void channels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH_ADHKAR, L.s("الأذكار"), NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(new NotificationChannel(CH_PRAYER, L.s("مواقيت الصلاة"), NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CH_IQAMA, L.s("الإقامة"), NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CH_ADHAN, L.s("تشغيل الأذان"), NotificationManager.IMPORTANCE_LOW));
    }

    static PendingIntent pi(Context c, int code, String kind, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction("com.hassan.hisn.ALARM." + code).putExtra("kind", kind).putExtra("code", code);
        return PendingIntent.getBroadcast(c, code, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void set(Context c, long at, int code, String kind) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pi(c, code, kind, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    static long nextDaily(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis() + 1000) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    /** Next on-screen dhikr time: on the clock grid of ov_min minutes, inside the active hours [ov_from, ov_to). */
    static long nextOverlay(SharedPreferences p) {
        int step = Math.max(15, p.getInt("ov_min", 60));
        int from = p.getInt("ov_from", 8), to = p.getInt("ov_to", 23);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, 1);
        int m = c.get(Calendar.MINUTE);
        c.add(Calendar.MINUTE, (step - (m % step)) % step);
        for (int i = 0; i < 24 * 60 / 15 + 2; i++) {
            int h = c.get(Calendar.HOUR_OF_DAY);
            boolean in = from <= to ? (h >= from && h < to) : (h >= from || h < to);
            if (in) return c.getTimeInMillis();
            c.add(Calendar.MINUTE, 15);
            int mm = c.get(Calendar.MINUTE);
            c.add(Calendar.MINUTE, (step - (mm % step)) % step);
        }
        return System.currentTimeMillis() + 3600000L;
    }

    /** Next time (ms) of daily prayer i (0..4), or 0. */
    public static long nextPrayer(Context c, int i) {
        double[] ll = Store.latLng(c);
        Calendar cal = Calendar.getInstance();
        for (int d = 0; d < 3; d++) {
            long t = PrayerTimes.millis(ll[0], ll[1], cal, Store.method(c), Store.hanafi(c))[PRAYER_IDX[i]];
            if (t > System.currentTimeMillis() + 1000) return t;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return 0;
    }

    public static void scheduleAll(Context c, boolean resetPeriodic) {
        SharedPreferences p = Store.p(c);
        if (p.getBoolean("prayer_on", true)) {
            for (int i = 0; i < 5; i++) {
                long t = nextPrayer(c, i);
                if (t > 0) set(c, t, i + 1, CH_PRAYER);
            }
        }
        if (p.getBoolean("morning_on", true)) set(c, nextDaily(p.getInt("morning_h", 6), p.getInt("morning_m", 0)), 11, "morning");
        if (p.getBoolean("evening_on", true)) set(c, nextDaily(p.getInt("evening_h", 17), p.getInt("evening_m", 30)), 12, "evening");
        if (p.getBoolean("sleep_on", true)) set(c, nextDaily(p.getInt("sleep_h", 22), p.getInt("sleep_m", 30)), 13, "sleep");
        AlarmManager am0 = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (p.getBoolean("ov_on", true)) set(c, nextOverlay(p), 21, "overlay");
        else am0.cancel(pi(c, 21, "overlay", PendingIntent.FLAG_UPDATE_CURRENT));
        int hrs = p.getInt("periodic_h", 3);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (hrs <= 0) am.cancel(pi(c, 20, "periodic", PendingIntent.FLAG_UPDATE_CURRENT));
        else if (resetPeriodic || pi(c, 20, "periodic", PendingIntent.FLAG_NO_CREATE) == null)
            set(c, System.currentTimeMillis() + hrs * 3600000L, 20, "periodic");
    }

    public static void scheduleIqama(Context c, int prayer, long adhanAt) {
        if (!Store.p(c).getBoolean("iqama_on", true)) return;
        long at = adhanAt + Store.iqamaMinutes(c, prayer) * 60000L;
        Store.p(c).edit().putLong("iqama_at", at).putInt("iqama_prayer", prayer).apply();
        if (at > System.currentTimeMillis()) set(c, at, 31 + prayer, "iqama");
    }

    public static Notification.Builder builder(Context c, String channel) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel) : new Notification.Builder(c);
    }

    public static void notify(Context c, int id, String channel, String title, String text, Intent intent) {
        channels(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent open = PendingIntent.getActivity(c, id, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = builder(c, channel).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 26 && !CH_ADHKAR.equals(channel)) b.setDefaults(Notification.DEFAULT_ALL);
        try { nm.notify(id, b.build()); } catch (SecurityException ignored) { }
    }
}
