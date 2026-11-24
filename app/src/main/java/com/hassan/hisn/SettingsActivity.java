package com.hassan.hisn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    SharedPreferences p;
    LinearLayout body;
    LinearLayout lockBox;
    static MediaPlayer preview;
    static final Handler H = new Handler(Looper.getMainLooper());
    static final String[][] VOICES = {{"makkah", L.s("الحرم المكي (مكة المكرمة)")}, {"makkah2", L.s("الحرم المكي (تسجيل المغرب)")}, {"madinah", L.s("الحرم النبوي (المدينة المنورة)")}, {"off", L.s("إشعار فقط بدون صوت أذان")}};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Store.p(this);
        LinearLayout[] root = new LinearLayout[1];
        body = Ui.screen(this, L.s("الإعدادات"), root);
        build();
        setContentView(root[0]);
        if ("lock".equals(getIntent().getStringExtra("scroll"))) {
            final View target = lockBox;
            body.post(new Runnable() { public void run() {
                ScrollView sv = (ScrollView) body.getParent();
                sv.smoothScrollTo(0, target.getTop());
            } });
        }
    }

    void switchLanguage(boolean english) {
        L.set(this, english);
        android.content.Intent i = new android.content.Intent(this, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        H.postDelayed(new Runnable() { public void run() { android.os.Process.killProcess(android.os.Process.myPid()); } }, 300);
    }

    void build() {
        body.removeAllViews();
        body.addView(Ui.section(this, L.s("اللغة / Language")));
        for (int k = 0; k < 2; k++) {
            final boolean wantEn = k == 1;
            boolean sel = L.en() == wantEn;
            Button lb = Ui.button(this, (sel ? "● " : "○ ") + (wantEn ? "English" : "العربية"), sel ? Ui.GREEN : Ui.GREEN_LIGHT, sel ? Color.WHITE : Ui.GREEN);
            lb.setTextSize(16);
            lb.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { if (L.en() != wantEn) switchLanguage(wantEn); } });
            body.addView(lb);
        }
        body.addView(Ui.text(this, L.s("المصحف والتفسير والأحاديث الأصلية تبقى بالعربية دائمًا. تتغير القوائم والعناوين فقط، ويعاد تشغيل التطبيق عند تغيير اللغة."), 13, Ui.MUTED));
        body.addView(Ui.section(this, L.s("الأذان")));
        body.addView(sw(L.s("تشغيل صوت الأذان وقت الصلاة"), "adhan_on", true));
        for (final String[] v : VOICES) {
            boolean sel = v[0].equals(p.getString("voice", "makkah"));
            Button bt = Ui.button(this, (sel ? "● " : "○ ") + v[1], sel ? Ui.GREEN : Ui.GREEN_LIGHT, sel ? Color.WHITE : Ui.GREEN);
            bt.setTextSize(15);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { p.edit().putString("voice", v[0]).apply(); build(); } });
            body.addView(bt);
        }
        body.addView(Ui.text(this, L.s("أذان الفجر يُشغَّل دائمًا بتسجيل خاص بالفجر (فيه \"الصلاة خير من النوم\"). يُستخدم صوت قناة المنبه، فارفع صوت المنبه."), 13, Ui.MUTED));
        Button test = Ui.button(this, L.s("▶ تجربة الصوت المختار"), Ui.GREEN, Color.WHITE);
        test.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { previewAdhan(SettingsActivity.this); } });
        body.addView(test);
        body.addView(Ui.text(this, L.s("تشغيل الأذان لكل صلاة:"), 15, Ui.INK));
        for (int i = 0; i < 5; i++) body.addView(sw(Scheduler.PRAYER_AR[i], "adhan_" + i, true));

        body.addView(Ui.section(this, L.s("الإقامة")));
        body.addView(sw(L.s("تنبيه الإقامة وعدّاد الانتظار بعد الأذان"), "iqama_on", true));
        for (int i = 0; i < 5; i++) body.addView(stepper(i));

        body.addView(Ui.section(this, L.s("حساب المواقيت")));
        for (int m = 0; m < PrayerTimes.METHOD_NAMES.length; m++) {
            final int mm = m;
            boolean sel = Store.method(this) == m;
            Button bt = Ui.button(this, (sel ? "● " : "○ ") + PrayerTimes.METHOD_NAMES[m], sel ? Ui.GREEN : Ui.GREEN_LIGHT, sel ? Color.WHITE : Ui.GREEN);
            bt.setTextSize(15);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { p.edit().putInt("method", mm).apply(); Scheduler.scheduleAll(SettingsActivity.this, true); build(); } });
            body.addView(bt);
        }
        body.addView(sw(L.s("العصر بالمذهب الحنفي (ظل مثلين)"), "hanafi", false));

        body.addView(Ui.section(this, L.s("تنبيهات الأذكار")));
        body.addView(sw(L.s("أذكار الصباح (٦:٠٠ ص)"), "morning_on", true));
        body.addView(sw(L.s("أذكار المساء (٥:٣٠ م)"), "evening_on", true));
        body.addView(sw(L.s("أذكار النوم (١٠:٣٠ م)"), "sleep_on", true));
        body.addView(Ui.section(this, L.s("ذكر يظهر على الشاشة")));
        body.addView(sw(L.s("إظهار ذكر فوق أي تطبيق مفتوح"), "ov_on", true));
        body.addView(Ui.text(this, L.s("يظهر كبطاقة فوق التطبيق الذي تستخدمه ويختفي بعد ١٥ ثانية أو عند الضغط على إغلاق. يحتاج إذن \"الظهور فوق التطبيقات\"، وبدونه يصل كإشعار. لا يظهر والشاشة مطفأة."), 13, Ui.MUTED));
        LinearLayout orow = new LinearLayout(this);
        for (final int mn : new int[]{30, 60, 120, 180}) {
            boolean sel = p.getInt("ov_min", 60) == mn;
            Button bt = Ui.button(this, mn < 60 ? Ui.ar(mn) + L.s(" د") : Ui.ar(mn / 60) + L.s(" س"), sel ? Ui.GREEN : Ui.GREEN_LIGHT, sel ? Color.WHITE : Ui.GREEN);
            bt.setTextSize(14);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { p.edit().putInt("ov_min", mn).apply(); Scheduler.scheduleAll(SettingsActivity.this, true); build(); } });
            orow.addView(bt, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(orow, new LinearLayout.LayoutParams(-1, -2));
        body.addView(hourStepper(L.s("من الساعة"), "ov_from", 8));
        body.addView(hourStepper(L.s("إلى الساعة"), "ov_to", 23));
        Button ovPerm = Ui.button(this, android.provider.Settings.canDrawOverlays(this) ? L.s("✓ إذن الظهور فوق التطبيقات مفعّل") : L.s("تفعيل إذن الظهور فوق التطبيقات"), Ui.GREEN_LIGHT, Ui.GREEN);
        ovPerm.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
            if (!android.provider.Settings.canDrawOverlays(SettingsActivity.this)) startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName())));
        } });
        body.addView(ovPerm);
        Button ovTest = Ui.button(this, L.s("تجربة الذكر على الشاشة بعد ٥ ثوانٍ"), Ui.GREEN, Color.WHITE);
        ovTest.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
            Toast.makeText(SettingsActivity.this, L.s("اخرج إلى أي تطبيق وانتظر ٥ ثوانٍ"), Toast.LENGTH_LONG).show();
            final Context ac = getApplicationContext();
            H.postDelayed(new Runnable() { public void run() { if (!OverlayPopup.show(ac, Dhikr.SHORT[(int) (System.currentTimeMillis() % Dhikr.SHORT.length)], L.s("ذكر الساعة"))) Toast.makeText(ac, L.s("فعّل إذن الظهور فوق التطبيقات أولاً"), Toast.LENGTH_LONG).show(); } }, 5000);
        } });
        body.addView(ovTest);
        body.addView(Ui.text(this, L.s("ولعمل التذكير بدقة مع توفير الطاقة: اسمح للتطبيق بالمنبّهات الدقيقة، واستثنِه من تحسين البطارية من إعدادات الهاتف."), 13, Ui.MUTED));
        Button bat = Ui.button(this, L.s("فتح إعدادات البطارية للتطبيق"), Ui.GREEN_LIGHT, Ui.GREEN);
        bat.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
            try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } catch (Exception e) { Toast.makeText(SettingsActivity.this, L.s("غير متاح على هذا الهاتف"), Toast.LENGTH_SHORT).show(); }
        } });
        body.addView(bat);
        body.addView(Ui.text(this, L.s("تذكير بذكر قصير كل:"), 15, Ui.INK));
        LinearLayout row = new LinearLayout(this);
        for (final int h : new int[]{0, 1, 3, 6}) {
            boolean sel = p.getInt("periodic_h", 3) == h;
            Button bt = Ui.button(this, h == 0 ? L.s("إيقاف") : Ui.ar(h) + L.s(" س"), sel ? Ui.GREEN : Ui.GREEN_LIGHT, sel ? Color.WHITE : Ui.GREEN);
            bt.setTextSize(14);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View x) { p.edit().putInt("periodic_h", h).apply(); Scheduler.scheduleAll(SettingsActivity.this, true); build(); } });
            row.addView(bt, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(row, new LinearLayout.LayoutParams(-1, -2));

        lockBox = new LinearLayout(this);
        ((LinearLayout) lockBox).setOrientation(LinearLayout.VERTICAL);
        body.addView(lockBox, new LinearLayout.LayoutParams(-1, -2));
        LockSettings.add(this, (LinearLayout) lockBox, p);
    }

    View sw(String label, final String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(17);
        s.setTextColor(Ui.INK);
        s.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        s.setChecked(p.getBoolean(key, def));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { public void onCheckedChanged(CompoundButton c, boolean on) {
            p.edit().putBoolean(key, on).apply();
            Scheduler.scheduleAll(SettingsActivity.this, true);
        } });
        return s;
    }

    View hourStepper(final String label, final String key, final int def) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        final TextView t = Ui.text(this, "", 17, Ui.INK);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final Runnable upd = new Runnable() { public void run() { int h = p.getInt(key, def); t.setText(label + ": " + Ui.ar(h == 0 ? 24 : h) + L.s(":٠٠")); } };
        upd.run();
        for (final int d : new int[]{-1, 1}) {
            Button b = Ui.button(this, d < 0 ? "−" : "+", Ui.GREEN_LIGHT, Ui.GREEN);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
                p.edit().putInt(key, (p.getInt(key, def) + d + 24) % 24).apply();
                Scheduler.scheduleAll(SettingsActivity.this, true);
                upd.run();
            } });
            r.addView(b, new LinearLayout.LayoutParams(Ui.dp(this, 64), -2));
        }
        return r;
    }

    View stepper(final int i) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        final TextView t = Ui.text(this, "", 17, Ui.INK);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final Runnable upd = new Runnable() { public void run() { t.setText(Scheduler.PRAYER_AR[i] + ": " + Ui.ar(Store.iqamaMinutes(SettingsActivity.this, i)) + L.s(" دقيقة")); } };
        upd.run();
        for (final int d : new int[]{-5, 5}) {
            Button b = Ui.button(this, d < 0 ? "−" : "+", Ui.GREEN_LIGHT, Ui.GREEN);
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View x) {
                int v = Math.max(5, Math.min(60, Store.iqamaMinutes(SettingsActivity.this, i) + d));
                p.edit().putInt("iqama_" + i, v).apply();
                upd.run();
            } });
            r.addView(b, new LinearLayout.LayoutParams(Ui.dp(this, 64), -2));
        }
        return r;
    }

    @Override protected void onResume() { super.onResume(); if (lockBox != null) LockSettings.refresh((LinearLayout) lockBox); }
    @Override protected void onPause() { stopPreview(); super.onPause(); }

    static void stopPreview() {
        try { if (preview != null) { preview.stop(); preview.release(); } } catch (Exception ignored) { }
        preview = null;
    }

    /** Plays the first 25 seconds of the chosen adhan. Press again to stop. */
    public static void previewAdhan(Context c) {
        if (preview != null) { stopPreview(); return; }
        try {
            AssetFileDescriptor fd = c.getAssets().openFd(AdhanService.voiceFile(c, 1).replace("makkah.ogg", "makkah.ogg"));
            String v = Store.p(c).getString("voice", "makkah");
            if ("off".equals(v)) { Toast.makeText(c, L.s("الخيار الحالي: إشعار فقط"), Toast.LENGTH_SHORT).show(); return; }
            preview = new MediaPlayer();
            preview.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            preview.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            preview.prepare();
            preview.start();
            H.postDelayed(new Runnable() { public void run() { stopPreview(); } }, 25000);
            Toast.makeText(c, L.s("جاري التشغيل (٢٥ ثانية). اضغط مرة أخرى للإيقاف"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) { stopPreview(); Toast.makeText(c, L.s("تعذر تشغيل الصوت"), Toast.LENGTH_SHORT).show(); }
    }
}
