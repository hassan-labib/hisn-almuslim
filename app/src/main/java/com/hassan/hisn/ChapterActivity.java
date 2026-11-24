package com.hassan.hisn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** One chapter of Hisn al-Muslim: one dhikr per screen, tap counter, auto-advance when the required count is reached. */
public class ChapterActivity extends Activity {
    public static Intent intent(Context c, int index) { return new Intent(c, ChapterActivity.class).putExtra("i", index); }

    int idx;
    JSONArray items = new JSONArray();
    String titleAr = "";
    AdhkarPager pager;
    final Handler h = new Handler(Looper.getMainLooper());
    boolean locked; // true during the short pause before auto-advance
    LinearLayout root, content;
    TextView progressText;
    ProgressBar bar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        idx = getIntent().getIntExtra("i", 0);
        try {
            JSONObject ch = Store.hisn(this).getJSONObject(idx);
            titleAr = ch.getString("t");
            items = ch.getJSONArray("a");
        } catch (Exception ignored) { }
        int[] need = new int[items.length()];
        for (int i = 0; i < need.length; i++) need[i] = Math.max(1, items.optJSONObject(i).optInt("c", 1));
        pager = new AdhkarPager(need);
        getWindow().setStatusBarColor(Ui.GREEN);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CREAM);
        Ui.rtl(root);
        root.addView(Ui.topBar(this, L.s("الحصن"), true), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        render();
    }

    @Override protected void onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy(); }

    void render() {
        while (root.getChildCount() > 1) root.removeViewAt(1);
        locked = false;
        if (items.length() == 0) { root.addView(Ui.text(this, L.s("تعذر تحميل هذا الباب"), 16, Ui.INK)); return; }
        if (allDoneScreen) { renderDone(); return; }
        final int i = pager.cur;
        JSONObject o = items.optJSONObject(i);
        int need = pager.need[i];

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 4));
        TextView t = Ui.bold(this, L.title(idx, titleAr), 18, Ui.GREEN);
        t.setGravity(Gravity.CENTER);
        head.addView(t);
        progressText = Ui.text(this, L.s("الذكر ") + Ui.ar(i + 1) + L.s(" من ") + Ui.ar(pager.size()), 13, Ui.MUTED);
        progressText.setGravity(Gravity.CENTER);
        head.addView(progressText);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(pager.size());
        bar.setProgress(i + 1);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
        head.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 6)));
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));

        ScrollView sv = new ScrollView(this);
        LinearLayout card = Ui.card(this);
        TextView x = Ui.text(this, o.optString("x"), 23, Ui.INK);
        x.setLineSpacing(0, 1.6f);
        x.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(x);
        TextView cnt = Ui.bold(this, L.times(need), 15, Ui.GOLD);
        cnt.setGravity(Gravity.CENTER);
        cnt.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(cnt);
        String ref = o.optString("r", "");
        if (ref.length() > 0) {
            TextView r = Ui.text(this, ref, 12, Ui.MUTED);
            r.setPadding(0, Ui.dp(this, 8), 0, 0);
            r.setGravity(Gravity.CENTER);
            card.addView(r);
        }
        if (L.en()) {
            JSONObject en = L.adhkarEn(idx, i);
            if (en != null) {
                TextView tl = Ui.text(this, en.optString("l"), 15, Ui.GREEN_DARK);
                tl.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                tl.setPadding(0, Ui.dp(this, 14), 0, 0);
                card.addView(tl);
                TextView tr = Ui.text(this, en.optString("t"), 16, Ui.INK);
                tr.setPadding(0, Ui.dp(this, 10), 0, 0);
                card.addView(tr);
                String s = en.optString("s");
                if (s.length() > 0) {
                    TextView ts = Ui.text(this, "Translation source: " + s + " (fitrahive/dua-dhikr, MIT)", 11, Ui.MUTED);
                    ts.setPadding(0, Ui.dp(this, 8), 0, 0);
                    card.addView(ts);
                }
            } else {
                TextView na = Ui.text(this, "English translation is not available for this item yet.", 12, Ui.MUTED);
                na.setPadding(0, Ui.dp(this, 12), 0, 0);
                card.addView(na);
            }
        }
        sv.addView(card, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1f);
        sp.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        root.addView(sv, sp);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 10));
        final Button counter = Ui.button(this, counterLabel(i), Ui.GREEN, Color.WHITE);
        counter.setTextSize(22);
        counter.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 22));
        counter.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onTap(counter); } });
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        col.addView(counter);
        TextView hint = Ui.text(this, L.s("اضغط عند كل مرة تذكره"), 12, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        col.addView(hint);
        root.addView(col, new LinearLayout.LayoutParams(-1, -2));
        Button prev = Ui.button(this, L.s("السابق"), Ui.GREEN_LIGHT, Ui.GREEN);
        Button next = Ui.button(this, L.s("التالي"), Ui.GREEN_LIGHT, Ui.GREEN);
        prev.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { if (pager.prev()) render(); } });
        next.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { if (pager.next()) render(); } });
        nav.addView(prev, new LinearLayout.LayoutParams(0, -2, 1f));
        View gap = new View(this);
        nav.addView(gap, new LinearLayout.LayoutParams(Ui.dp(this, 10), 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
    }

    boolean allDoneScreen;

    String counterLabel(int i) {
        int d = pager.done[i], n = pager.need[i];
        if (d >= n) return L.s("✓ تم");
        return Ui.ar(d) + " / " + Ui.ar(n);
    }

    void onTap(final Button counter) {
        if (locked) return;
        int r = pager.tap();
        buzz(r == AdhkarPager.COUNTED ? 15 : 120);
        counter.setText(counterLabel(pager.cur));
        if (r == AdhkarPager.COUNTED) return;
        locked = true;
        counter.setBackground(Ui.rounded(Ui.GOLD, 14, this));
        h.postDelayed(new Runnable() { public void run() {
            if (isFinishing()) return;
            if (pager.next()) render();
            else { allDoneScreen = true; renderDone(); }
        } }, 450);
    }

    void renderDone() {
        while (root.getChildCount() > 1) root.removeViewAt(1);
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER);
        TextView ok = Ui.bold(this, "✓", 60, Ui.GREEN);
        ok.setGravity(Gravity.CENTER);
        card.addView(ok);
        TextView m = Ui.bold(this, L.s("تمّ الباب بحمد الله، تقبل الله منك"), 20, Ui.GREEN);
        m.setGravity(Gravity.CENTER);
        card.addView(m);
        TextView ttl = Ui.text(this, L.title(idx, titleAr), 15, Ui.MUTED);
        ttl.setGravity(Gravity.CENTER);
        card.addView(ttl);
        Button again = Ui.button(this, L.s("إعادة من البداية"), Ui.GREEN_LIGHT, Ui.GREEN);
        again.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { pager.reset(); allDoneScreen = false; render(); } });
        Button close = Ui.button(this, L.s("إغلاق"), Ui.GREEN, Color.WHITE);
        close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
        card.addView(again);
        card.addView(close);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(Ui.dp(this, 16), Ui.dp(this, 40), Ui.dp(this, 16), 0);
        root.addView(card, lp);
    }

    void buzz(int ms) {
        try { ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(ms); } catch (Exception ignored) { }
    }
}
