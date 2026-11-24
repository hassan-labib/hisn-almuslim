package com.hassan.hisn;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Hosts the five main tabs: Home, Prayer, Quran, Hisn (adhkar), More. */
public class MainActivity extends Activity {
    static final String[] TAB_NAMES = {L.s("الرئيسية"), L.s("الصلاة"), L.s("المصحف"), L.s("الحصن"), L.s("المزيد")};
    static final String[] TAB_ICONS = {"⌂", "◐", "☰", "✦", "⋯"};
    FrameLayout content;
    LinearLayout[] tabViews = new LinearLayout[5];
    int tab = 0;
    final Handler h = new Handler(Looper.getMainLooper());
    Runnable ticker;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Scheduler.channels(this);
        getWindow().setStatusBarColor(Ui.GREEN);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        Ui.rtl(root);
        root.addView(Ui.topBar(this, L.s("حصن المسلم"), false), new LinearLayout.LayoutParams(-1, -2));
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(navBar(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        askPermissions();
        Scheduler.scheduleAll(this, false);
        LockController.resumeIfActive(this);
        showTab(getIntent().getIntExtra("tab", 0));
    }

    LinearLayout navBar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(Ui.dp(this, 8));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            TextView ic = Ui.text(this, TAB_ICONS[i], 22, Ui.MUTED);
            ic.setGravity(Gravity.CENTER);
            TextView tx = Ui.text(this, TAB_NAMES[i], 12, Ui.MUTED);
            tx.setGravity(Gravity.CENTER);
            item.addView(ic);
            item.addView(tx);
            item.setTag(new TextView[]{ic, tx});
            item.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(idx); } });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            tabViews[i] = item;
        }
        return nav;
    }

    void showTab(int i) {
        tab = i;
        content.removeAllViews();
        stopTicker();
        for (int k = 0; k < 5; k++) {
            TextView[] tv = (TextView[]) tabViews[k].getTag();
            int col = k == i ? Ui.GREEN : Ui.MUTED;
            tv[0].setTextColor(col);
            tv[1].setTextColor(col);
            tv[1].setTypeface(null, k == i ? 1 : 0);
        }
        View v;
        switch (i) {
            case 1: v = Tabs.prayer(this); break;
            case 2: v = Tabs.quran(this); break;
            case 3: v = Tabs.hisn(this); break;
            case 4: v = Tabs.more(this); break;
            default: v = Tabs.home(this);
        }
        content.addView(v, new FrameLayout.LayoutParams(-1, -1));
    }

    void startTicker(final Runnable r) {
        stopTicker();
        ticker = new Runnable() { public void run() { r.run(); h.postDelayed(this, 1000); } };
        h.post(ticker);
    }

    void stopTicker() { if (ticker != null) h.removeCallbacks(ticker); ticker = null; }

    @Override
    protected void onResume() {
        super.onResume();
        if (LockController.isActive(this)) {
            LockController.launchLock(this);
            finish();
            return;
        }
        if (content != null && content.getChildCount() > 0) showTab(tab);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        showTab(i.getIntExtra("tab", tab));
    }

    @Override protected void onPause() { stopTicker(); super.onPause(); }

    void askPermissions() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}, 1);
        } else if (checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
            requestPermissions(new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        Store.refreshLocation(this, new Runnable() { public void run() { if (tab <= 1) showTab(tab); } });
        Scheduler.scheduleAll(this, false);
        if (Build.VERSION.SDK_INT >= 31 && !((AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {
            try { startActivity(new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM", Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) { }
        }
    }
}
