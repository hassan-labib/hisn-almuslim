package com.hassan.hisn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        String kind = in.getStringExtra("kind");
        int code = in.getIntExtra("code", 0);
        if (kind == null) return;
        try {
            switch (kind) {
                case "prayer": {
                    int i = code - 1;
                    String name = Scheduler.PRAYER_AR[i];
                    Scheduler.scheduleIqama(c, i, System.currentTimeMillis());
                    boolean played = false;
                    if (Store.p(c).getBoolean("adhan_on", true) && Store.p(c).getBoolean("adhan_" + i, true)
                            && !"off".equals(Store.p(c).getString("voice", "makkah"))) {
                        played = AdhanService.start(c, i);
                    }
                    if (!played) {
                        Scheduler.notify(c, 100 + code, Scheduler.CH_PRAYER, L.s("حان الآن موعد صلاة ") + name, L.s("قم إلى الصلاة، تقبل الله منك"), new Intent(c, MainActivity.class));
                        AdhanService.postAfterAdhan(c, i);
                    }
                    LockController.onPrayerTime(c, name);
                    break;
                }
                case "iqama": {
                    int i = code - 31;
                    Scheduler.notify(c, 140, Scheduler.CH_IQAMA, L.s("أُقيمت الصلاة"), L.s("حان وقت إقامة صلاة ") + Scheduler.PRAYER_AR[i], new Intent(c, MainActivity.class));
                    ((android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(AdhanService.NOTIF_IQAMA);
                    break;
                }
                case "morning":
                    Scheduler.notify(c, 111, Scheduler.CH_ADHKAR, L.s("أذكار الصباح"), L.s("حان وقت أذكار الصباح"), ChapterActivity.intent(c, 26));
                    break;
                case "evening":
                    Scheduler.notify(c, 112, Scheduler.CH_ADHKAR, L.s("أذكار المساء"), L.s("حان وقت أذكار المساء"), ChapterActivity.intent(c, 27));
                    break;
                case "sleep":
                    Scheduler.notify(c, 113, Scheduler.CH_ADHKAR, L.s("أذكار النوم"), L.s("لا تنس أذكار النوم"), ChapterActivity.intent(c, 28));
                    break;
                case "overlay": {
                    String[] pool = Dhikr.SHORT;
                    String d = pool[new Random().nextInt(pool.length)];
                    if (!OverlayPopup.show(c, d, L.s("ذكر الساعة"))) {
                        android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
                        if (pm == null || pm.isInteractive()) Scheduler.notify(c, 121, Scheduler.CH_ADHKAR, L.s("ذكر الساعة"), d, new Intent(c, MainActivity.class));
                    }
                    break;
                }
                case "periodic": {
                    String[] pool = Dhikr.SHORT;
                    Scheduler.notify(c, 120, Scheduler.CH_ADHKAR, L.s("ذكّر نفسك"), pool[new Random().nextInt(pool.length)], new Intent(c, MainActivity.class));
                    Scheduler.scheduleAll(c, true);
                    return;
                }
                default:
            }
        } catch (Exception ignored) { }
        Scheduler.scheduleAll(c, false);
    }
}
