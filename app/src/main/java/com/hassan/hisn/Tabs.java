package com.hassan.hisn;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** The five main tab screens. */
public class Tabs {
    static final String[] HIJRI_MONTHS = {L.s("محرم"), L.s("صفر"), L.s("ربيع الأول"), L.s("ربيع الآخر"), L.s("جمادى الأولى"), L.s("جمادى الآخرة"), L.s("رجب"), L.s("شعبان"), L.s("رمضان"), L.s("شوال"), L.s("ذو القعدة"), L.s("ذو الحجة")};
    static final String[] DAYS = {L.s("الأحد"), L.s("الاثنين"), L.s("الثلاثاء"), L.s("الأربعاء"), L.s("الخميس"), L.s("الجمعة"), L.s("السبت")};

    static String hijriDate() {
        try {
            android.icu.util.IslamicCalendar ic = new android.icu.util.IslamicCalendar(); ic.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(System.currentTimeMillis());
            return Ui.ar(ic.get(android.icu.util.Calendar.DAY_OF_MONTH)) + " " + HIJRI_MONTHS[ic.get(android.icu.util.Calendar.MONTH)] + " " + Ui.ar(ic.get(android.icu.util.Calendar.YEAR)) + L.s(" هـ");
        } catch (Throwable t) { return ""; }
    }

    static String gregorian() {
        Calendar c = Calendar.getInstance();
        return DAYS[c.get(Calendar.DAY_OF_WEEK) - 1] + L.s("، ") + Ui.ar(new SimpleDateFormat("d/M/yyyy", Locale.US).format(new Date()));
    }

    static String clock(long ms) { return Ui.ar(new SimpleDateFormat("hh:mm a", new Locale(L.en() ? "en" : "ar", "EG")).format(new Date(ms))); }

    /** Index of the next entry among the 6 timings (incl. sunrise), or -1 if none today. */
    static int nextIdx(long[] t) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) if (t[i] > now) return i;
        return -1;
    }

    static View wrapScroll(MainActivity a, LinearLayout[] colOut) {
        LinearLayout holder = new LinearLayout(a);
        holder.setOrientation(LinearLayout.VERTICAL);
        colOut[0] = Ui.scrollBody(a, holder);
        return holder;
    }

    static LinearLayout tile(final MainActivity a, String icon, String label, final Intent open, int color) {
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.rounded(color, 16, a));
        t.setPadding(Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16));
        TextView ic = Ui.text(a, icon, 30, Color.WHITE);
        ic.setGravity(Gravity.CENTER);
        TextView tx = Ui.bold(a, label, 15, Color.WHITE);
        tx.setGravity(Gravity.CENTER);
        t.addView(ic);
        t.addView(tx);
        t.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(open); } });
        return t;
    }

    static View home(final MainActivity a) {
        LinearLayout[] col = new LinearLayout[1];
        View root = wrapScroll(a, col);
        LinearLayout c = col[0];
        // date + next prayer hero
        LinearLayout hero = new LinearLayout(a);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setBackground(Ui.gradient(Ui.GREEN, Ui.GREEN_DARK, 22, a));
        hero.setPadding(Ui.dp(a, 20), Ui.dp(a, 18), Ui.dp(a, 20), Ui.dp(a, 18));
        TextView date = Ui.text(a, hijriDate() + "  •  " + gregorian(), 14, Color.parseColor("#CFE8DC"));
        date.setGravity(Gravity.CENTER);
        final TextView next = Ui.bold(a, "", 24, Color.WHITE);
        next.setGravity(Gravity.CENTER);
        final TextView count = Ui.bold(a, "", 34, Color.parseColor("#F4E9C8"));
        count.setGravity(Gravity.CENTER);
        final TextView at = Ui.text(a, "", 15, Color.parseColor("#CFE8DC"));
        at.setGravity(Gravity.CENTER);
        hero.addView(date);
        Ui.space(hero, 8);
        hero.addView(next);
        hero.addView(count);
        hero.addView(at);
        c.addView(hero, new LinearLayout.LayoutParams(-1, -2));
        hero.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.showTab(1); } });
        a.startTicker(new Runnable() { public void run() { fillNext(a, next, count, at); } });

        c.addView(Ui.section(a, L.s("اختصارات")));
        GridLayout grid = new GridLayout(a);
        grid.setColumnCount(2);
        int[] ids = new int[0];
        addTile(a, grid, "📖", L.s("المصحف"), new Intent(a, ReaderActivity.class).putExtra("page", Store.p(a).getInt("last_page", 1)), Ui.GREEN);
        addTile(a, grid, "🌅", L.s("أذكار الصباح"), ChapterActivity.intent(a, 26), Color.parseColor("#C58A1B"));
        addTile(a, grid, "🌙", L.s("أذكار المساء"), ChapterActivity.intent(a, 27), Color.parseColor("#2E5C8A"));
        addTile(a, grid, "🛏", L.s("أذكار النوم"), ChapterActivity.intent(a, 28), Color.parseColor("#5A4B8A"));
        addTile(a, grid, "📜", L.s("الأحاديث"), new Intent(a, HadithActivity.class), Color.parseColor("#7A4A1B"));
        addTile(a, grid, "📿", L.s("السبحة"), new Intent(a, TasbihActivity.class), Color.parseColor("#0B7A75"));
        addTile(a, grid, "🧭", L.s("القبلة"), new Intent(a, QiblaActivity.class), Color.parseColor("#8A3B3B"));
        c.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        c.addView(Ui.section(a, L.s("ذكر اليوم")));
        LinearLayout card = Ui.card(a);
        String d = Dhikr.SHORT[(int) ((System.currentTimeMillis() / 86400000L) % Dhikr.SHORT.length)];
        TextView t = Ui.text(a, d, 20, Ui.GREEN);
        t.setGravity(Gravity.CENTER);
        card.addView(t);
        c.addView(card);
        final int bmh = Bookmarks.main(a);
        if (bmh > 0) {
            Button bb = Ui.button(a, L.s("🔖 العودة إلى علامتي (صفحة ") + Ui.ar(bmh) + ")", Ui.GREEN, Color.WHITE);
            bb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, ReaderActivity.class).putExtra("page", bmh)); } });
            c.addView(bb);
        }
        if (Store.p(a).getInt("last_page", 0) > 0) {
            Button cont = Ui.button(a, L.s("أكمل القراءة من صفحة ") + Ui.ar(Store.p(a).getInt("last_page", 1)), Ui.GREEN_LIGHT, Ui.GREEN);
            cont.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, ReaderActivity.class).putExtra("page", Store.p(a).getInt("last_page", 1))); } });
            c.addView(cont);
        }
        return root;
    }

    static void addTile(MainActivity a, GridLayout g, String ic, String label, Intent open, int color) {
        LinearLayout t = tile(a, ic, label, open, color);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.setMargins(Ui.dp(a, 5), Ui.dp(a, 5), Ui.dp(a, 5), Ui.dp(a, 5));
        g.addView(t, lp);
    }

    static void fillNext(MainActivity a, TextView next, TextView count, TextView at) {
        long[] t = Store.today(a);
        int i = nextIdx(t);
        long target;
        String name;
        if (i < 0) {
            Calendar tm = Calendar.getInstance();
            tm.add(Calendar.DAY_OF_YEAR, 1);
            double[] ll = Store.latLng(a);
            long[] n = PrayerTimes.millis(ll[0], ll[1], tm, Store.method(a), Store.hanafi(a));
            target = n[0];
            name = PrayerTimes.NAMES[0];
        } else { target = t[i]; name = PrayerTimes.NAMES[i]; }
        long sec = Math.max(0, (target - System.currentTimeMillis()) / 1000);
        next.setText((i == 1 ? L.s("الشروق") : L.s("الصلاة القادمة: ") + name));
        count.setText(Ui.ar(String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)));
        at.setText(clock(target));
    }

    static View prayer(final MainActivity a) {
        LinearLayout[] col = new LinearLayout[1];
        View root = wrapScroll(a, col);
        LinearLayout c = col[0];
        final long[] t = Store.today(a);
        int ni = nextIdx(t);
        boolean saved = Store.hasSavedLocation(a);
        TextView info = Ui.text(a, L.s("الطريقة: ") + PrayerTimes.METHOD_NAMES[Store.method(a)] + "\n" + (saved ? L.s("الموقع: حسب موقعك الحالي") : L.s("الموقع: القاهرة (فعّل إذن الموقع لتحديد موقعك)")), 14, Ui.MUTED);
        info.setGravity(Gravity.CENTER);
        c.addView(info);
        for (int i = 0; i < 6; i++) {
            LinearLayout card = Ui.card(a);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            boolean on = i == ni;
            if (on) card.setBackground(Ui.rounded(Ui.GREEN_LIGHT, 16, a));
            LinearLayout nameCol = new LinearLayout(a);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            nameCol.addView(Ui.bold(a, PrayerTimes.NAMES[i], 21, on ? Ui.GREEN : Ui.INK));
            if (i != 1 && Store.p(a).getBoolean("iqama_on", true)) nameCol.addView(Ui.text(a, L.s("الإقامة بعد ") + Ui.ar(Store.iqamaMinutes(a, i == 0 ? 0 : i - 1)) + L.s(" دقيقة"), 12, Ui.MUTED));
            View n = nameCol;
            TextView tm = Ui.bold(a, t[i] == 0 ? "--" : clock(t[i]), 21, on ? Ui.GREEN : Ui.INK);
            tm.setGravity(Gravity.END);
            card.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
            card.addView(tm, new LinearLayout.LayoutParams(0, -2, 1f));
            c.addView(card);
        }
        Button loc = Ui.button(a, L.s("تحديث موقعي"), Ui.GREEN_LIGHT, Ui.GREEN);
        loc.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= 23 && a.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) { a.askPermissions(); return; }
            Store.refreshLocation(a, new Runnable() { public void run() { a.showTab(1); } });
        } });
        c.addView(loc);
        c.addView(Ui.section(a, L.s("الأذان والإقامة")));
        Button test = Ui.button(a, L.s("▶ تجربة صوت الأذان"), Ui.GREEN, Color.WHITE);
        test.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { SettingsActivity.previewAdhan(a); } });
        c.addView(test);
        Button dua = Ui.button(a, L.s("دعاء بعد الأذان"), Ui.GREEN_LIGHT, Ui.GREEN);
        dua.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(ChapterActivity.intent(a, 14)); } });
        c.addView(dua);
        Button set = Ui.button(a, L.s("إعدادات الأذان والإقامة"), Ui.GREEN_LIGHT, Ui.GREEN);
        set.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, SettingsActivity.class)); } });
        c.addView(set);
        return root;
    }

    static View quran(final MainActivity a) {
        LinearLayout holder = new LinearLayout(a);
        holder.setOrientation(LinearLayout.VERTICAL);
        final boolean juz = Store.p(a).getBoolean("quran_juz", false);
        LinearLayout seg = new LinearLayout(a);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(Ui.dp(a, 16), Ui.dp(a, 10), Ui.dp(a, 16), 0);
        Button s1 = Ui.button(a, L.s("السور"), juz ? Ui.GREEN_LIGHT : Ui.GREEN, juz ? Ui.GREEN : Color.WHITE);
        Button s2 = Ui.button(a, L.s("الأجزاء"), juz ? Ui.GREEN : Ui.GREEN_LIGHT, juz ? Color.WHITE : Ui.GREEN);
        s1.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Store.p(a).edit().putBoolean("quran_juz", false).apply(); a.showTab(2); } });
        s2.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Store.p(a).edit().putBoolean("quran_juz", true).apply(); a.showTab(2); } });
        seg.addView(s1, new LinearLayout.LayoutParams(0, -2, 1f));
        seg.addView(s2, new LinearLayout.LayoutParams(0, -2, 1f));
        holder.addView(seg);
        LinearLayout c = Ui.scrollBody(a, holder);
        final int bm = Bookmarks.main(a);
        if (bm > 0) {
            Button bb = Ui.button(a, L.s("🔖 العودة إلى علامتي: ") + Bookmarks.label(a, bm), Ui.GREEN, Color.WHITE);
            bb.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, ReaderActivity.class).putExtra("page", bm)); } });
            c.addView(bb);
        }
        int last = Store.p(a).getInt("last_page", 0);
        if (last > 0) {
            Button cont = Ui.button(a, L.s("أكمل القراءة: صفحة ") + Ui.ar(last), Ui.GOLD, Color.WHITE);
            cont.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, ReaderActivity.class).putExtra("page", Store.p(a).getInt("last_page", 1))); } });
            c.addView(cont);
        }
        try {
            JSONObject m = Store.mushaf(a);
            if (!juz) {
                JSONArray s = m.getJSONArray("surahs");
                for (int i = 0; i < s.length(); i++) {
                    JSONObject o = s.getJSONObject(i);
                    c.addView(row(a, Ui.ar(i + 1) + ".  " + o.getString("n"), (o.getInt("m") == 1 ? L.s("مكية") : L.s("مدنية")) + " • " + Ui.ar(o.getInt("v")) + L.s(" آية"), L.s("ص ") + Ui.ar(o.getInt("p")), o.getInt("p")));
                }
            } else {
                JSONArray j = m.getJSONArray("juz");
                for (int i = 0; i < j.length(); i++) c.addView(row(a, L.s("الجزء ") + Ui.ar(i + 1), "", L.s("ص ") + Ui.ar(j.getInt(i)), j.getInt(i)));
            }
        } catch (Exception e) { c.addView(Ui.text(a, L.s("تعذر تحميل بيانات المصحف"), 16, Ui.INK)); }
        return holder;
    }

    static View row(final MainActivity a, String title, String sub, String right, final int page) {
        LinearLayout card = Ui.card(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = new LinearLayout(a);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(Ui.bold(a, title, 18, Ui.INK));
        if (sub.length() > 0) left.addView(Ui.text(a, sub, 13, Ui.MUTED));
        card.addView(left, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(Ui.text(a, right, 15, Ui.GREEN));
        card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(new Intent(a, ReaderActivity.class).putExtra("page", page)); } });
        return card;
    }

    static View hisn(final MainActivity a) {
        final LinearLayout holder = new LinearLayout(a);
        holder.setOrientation(LinearLayout.VERTICAL);
        final EditText search = new EditText(a);
        search.setHint(L.s("ابحث في حصن المسلم..."));
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setBackground(Ui.rounded(Color.WHITE, 14, a));
        search.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), 0);
        holder.addView(search, lp);
        final LinearLayout list = Ui.scrollBody(a, holder);
        final Runnable fill = new Runnable() { public void run() { fillHisn(a, list, Ui.plain(search.getText().toString().trim())); } };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int x, int y, int z) { }
            public void onTextChanged(CharSequence s, int x, int y, int z) { }
            public void afterTextChanged(Editable e) { fill.run(); }
        });
        fill.run();
        return holder;
    }

    static void fillHisn(MainActivity a, LinearLayout list, String q) {
        list.removeAllViews();
        JSONArray h = Store.hisn(a);
        if (q.length() == 0) {
            list.addView(Ui.section(a, L.s("الأكثر استخدامًا")));
            int[] top = {26, 27, 28, 24, 14, 0};
            for (int i : top) list.addView(hisnRow(a, h, i, true));
            list.addView(Ui.section(a, L.s("كل الأبواب (") + Ui.ar(h.length()) + ")"));
        }
        int shown = 0;
        for (int i = 0; i < h.length(); i++) {
            try {
                if (q.length() > 0) {
                    JSONObject o = h.getJSONObject(i);
                    boolean hit = Ui.plain(o.getString("t")).contains(q) || (L.en() && L.title(i, "").toLowerCase().contains(q.toLowerCase()));
                    if (!hit) {
                        JSONArray ad = o.getJSONArray("a");
                        for (int k = 0; k < ad.length() && !hit; k++) hit = Ui.plain(ad.getJSONObject(k).getString("x")).contains(q);
                    }
                    if (!hit) continue;
                }
                list.addView(hisnRow(a, h, i, false));
                shown++;
            } catch (Exception ignored) { }
        }
        if (q.length() > 0 && shown == 0) list.addView(Ui.text(a, L.s("لا توجد نتائج"), 17, Ui.MUTED));
    }

    static View hisnRow(final MainActivity a, JSONArray h, final int i, boolean accent) {
        LinearLayout card = Ui.card(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        String title = "";
        int n = 0;
        try { JSONObject o = h.getJSONObject(i); title = L.title(i, o.getString("t")); n = o.getJSONArray("a").length(); } catch (Exception ignored) { }
        if (accent) card.setBackground(Ui.rounded(Ui.GREEN_LIGHT, 16, a));
        card.addView(Ui.bold(a, title, 17, Ui.INK), new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(Ui.text(a, Ui.ar(n), 14, Ui.GREEN));
        card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(ChapterActivity.intent(a, i)); } });
        return card;
    }

    static View more(final MainActivity a) {
        LinearLayout[] col = new LinearLayout[1];
        View root = wrapScroll(a, col);
        LinearLayout c = col[0];
        c.addView(menu(a, "📜", L.s("الأحاديث (الكتب الستة وغيرها)"), new Intent(a, HadithActivity.class)));
        c.addView(menu(a, "📚", L.s("تحميل التفاسير للاستخدام بدون إنترنت"), new Intent(a, TafsirActivity.class).putExtra("manage", true)));
        c.addView(menu(a, "📿", L.s("السبحة"), new Intent(a, TasbihActivity.class)));
        c.addView(menu(a, "🧭", L.s("اتجاه القبلة"), new Intent(a, QiblaActivity.class)));
        c.addView(menu(a, "🔒", L.s("قفل الصلاة"), new Intent(a, SettingsActivity.class).putExtra("scroll", "lock")));
        c.addView(menu(a, "⚙", L.s("الإعدادات (الأذان، الإقامة، الإشعارات)"), new Intent(a, SettingsActivity.class)));
        c.addView(menu(a, "📤", L.s("مشاركة التطبيق"), shareIntent()));
        c.addView(menu(a, "ℹ", L.s("عن التطبيق والمصادر"), new Intent(a, AboutActivity.class)));
        c.addView(menu(a, "🛡", L.s("سياسة الخصوصية"), new Intent(a, AboutActivity.class).putExtra("privacy", true)));
        TextView v = Ui.text(a, L.s("حصن المسلم • الإصدار ٣.٥\nمجاني بالكامل، بنية صدقة جارية"), 13, Ui.MUTED);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, Ui.dp(a, 24), 0, 0);
        c.addView(v);
        return root;
    }

    /** Link used in the share message. Left empty until the Play Store listing exists. */
    static final String SHARE_URL = "";

    static Intent shareIntent() {
        String msg = L.en()
                ? "I recommend Hisn al-Muslim: a completely free Islamic app with the Madinah Mushaf, tafsir, hadith, adhan and iqama, daily adhkar and a prayer lock. It has no ads and no accounts, made as sadaqah jariyah."
                : "أنصحكم بتطبيق حصن المسلم: تطبيق إسلامي مجاني بالكامل فيه المصحف (مصحف المدينة) والتفسير والأحاديث والأذان والإقامة وأذكار اليوم وقفل الصلاة، بدون إعلانات ولا حسابات، صدقة جارية.";
        if (SHARE_URL.length() > 0) msg += "\n\n" + (L.en() ? "Download: " : "رابط التحميل: ") + SHARE_URL;
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, msg);
        return Intent.createChooser(i, L.s("مشاركة حصن المسلم"));
    }

    static View menu(final MainActivity a, String icon, String label, final Intent open) {
        LinearLayout card = Ui.card(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic = Ui.text(a, icon, 24, Ui.GREEN);
        ic.setPadding(0, 0, Ui.dp(a, 14), 0);
        card.addView(ic);
        card.addView(Ui.bold(a, label, 17, Ui.INK), new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(Ui.text(a, L.en() ? "›" : "‹", 24, Ui.MUTED));
        card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.startActivity(open); } });
        return card;
    }
}
