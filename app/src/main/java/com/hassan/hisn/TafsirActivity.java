package com.hassan.hisn;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;

/** Tafsir of one ayah (Muyassar, Saadi, Ibn Kathir), or the download manager when "manage" is set. */
public class TafsirActivity extends Activity {
    int surah, ayah, which;
    LinearLayout body;
    volatile boolean stop;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        which = Store.p(this).getInt("tafsir_which", 0);
        LinearLayout[] root = new LinearLayout[1];
        if (getIntent().getBooleanExtra("manage", false)) {
            body = Ui.screen(this, L.s("تحميل التفاسير"), root);
            setContentView(root[0]);
            manage();
            return;
        }
        String key = getIntent().getStringExtra("key");
        try { String[] kv = key.split(":"); surah = Integer.parseInt(kv[0]); ayah = Integer.parseInt(kv[1]); } catch (Exception e) { surah = 1; ayah = 1; }
        body = Ui.screen(this, L.s("التفسير"), root);
        setContentView(root[0]);
        render();
    }

    @Override protected void onDestroy() { stop = true; super.onDestroy(); }

    int ayahCount(int s) {
        try { return Store.mushaf(this).getJSONArray("surahs").getJSONObject(s - 1).getInt("v"); } catch (Exception e) { return 286; }
    }

    String surahName(int s) {
        try { return Store.mushaf(this).getJSONArray("surahs").getJSONObject(s - 1).getString("n"); } catch (Exception e) { return ""; }
    }

    void render() {
        body.removeAllViews();
        LinearLayout seg = new LinearLayout(this);
        for (int i = 0; i < 3; i++) {
            final int w = i;
            Button bt = Ui.button(this, TafsirStore.NAMES[i].replace(L.s("التفسير "), "").replace(L.s("تفسير "), ""), i == which ? Ui.GREEN : Ui.GREEN_LIGHT, i == which ? Color.WHITE : Ui.GREEN);
            bt.setTextSize(14);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { which = w; Store.p(TafsirActivity.this).edit().putInt("tafsir_which", w).apply(); render(); } });
            seg.addView(bt, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(seg, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout card = Ui.card(this);
        TextView h = Ui.bold(this, L.s("سورة ") + surahName(surah) + L.s(" • الآية ") + Ui.ar(surah == 0 ? 0 : ayah), 16, Ui.GOLD);
        h.setGravity(Gravity.CENTER);
        card.addView(h);
        TextView at = Ui.text(this, TafsirStore.ayahText(this, surah + ":" + ayah) + " ﴿" + Ui.ar(ayah) + "﴾", 24, Ui.INK);
        at.setGravity(Gravity.CENTER);
        at.setLineSpacing(0, 1.4f);
        at.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        card.addView(at);
        body.addView(card);
        final TextView tv = Ui.text(this, "", 19, Ui.INK);
        tv.setLineSpacing(0, 1.35f);
        tv.setTextIsSelectable(true);
        LinearLayout tc = Ui.card(this);
        tc.addView(Ui.bold(this, TafsirStore.NAMES[which], 17, Ui.GREEN));
        tc.addView(tv);
        body.addView(tc);
        body.addView(Ui.text(this, L.s("المصدر: ") + TafsirStore.NAMES[which] + " (" + TafsirStore.AUTHORS[which] + L.s(")، عبر واجهة موقع القرآن (quran.com) لمؤسسة القرآن."), 12, Ui.MUTED));
        LinearLayout nav = new LinearLayout(this);
        Button prev = Ui.button(this, L.s("› الآية السابقة"), Ui.GREEN_LIGHT, Ui.GREEN), next = Ui.button(this, L.s("الآية التالية ‹"), Ui.GREEN_LIGHT, Ui.GREEN);
        prev.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { step(-1); } });
        next.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { step(1); } });
        nav.addView(next, new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(prev, new LinearLayout.LayoutParams(0, -2, 1f));
        body.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        load(tv);
    }

    void step(int d) {
        int a = ayah + d, s = surah;
        if (a < 1) { if (s == 1) return; s--; a = ayahCount(s); }
        else if (a > ayahCount(s)) { if (s == 114) return; s++; a = 1; }
        surah = s; ayah = a;
        render();
    }

    void show(TextView tv, int id) {
        String[] note = new String[1];
        String t = TafsirStore.get(this, id, surah, ayah, note);
        if (t == null) { tv.setText(L.s("لا يوجد شرح لهذه الآية في هذا الكتاب.")); return; }
        CharSequence cs = Html.fromHtml(t, Html.FROM_HTML_MODE_COMPACT);
        tv.setText((note[0] != null ? note[0] + "\n\n" : "") + cs.toString().trim());
    }

    void load(final TextView tv) {
        final int id = TafsirStore.IDS[which], s = surah, a = ayah;
        if (TafsirStore.cached(this, id, s)) { show(tv, id); return; }
        tv.setText(L.s("جارٍ تحميل التفسير (مرة واحدة لكل سورة، ثم يُحفظ على الهاتف)..."));
        final android.content.Context ctx = getApplicationContext();
        new Thread(new Runnable() { public void run() {
            final boolean ok = TafsirStore.fetchSurah(ctx, id, s);
            runOnUiThread(new Runnable() { public void run() {
                if (s != surah || a != ayah || id != TafsirStore.IDS[which]) return;
                if (ok) show(tv, id); else tv.setText(L.s("تعذّر تحميل التفسير. تأكد من الإنترنت وحاول مرة أخرى."));
            } });
        } }).start();
    }

    void manage() {
        body.removeAllViews();
        body.addView(Ui.text(this, L.s("تُحمَّل التفاسير سورةً سورة وتُحفظ على الهاتف. الميسر صغير، السعدي متوسط، وابن كثير كبير (عدة عشرات من الميجابايت). يُفضّل واي فاي."), 15, Ui.INK));
        for (int i = 0; i < 3; i++) {
            final int id = TafsirStore.IDS[i];
            final LinearLayout card = Ui.card(this);
            card.addView(Ui.bold(this, TafsirStore.NAMES[i], 18, Ui.GREEN));
            final TextView st = Ui.text(this, L.s("المحمّل: ") + Ui.ar(TafsirStore.cachedCount(this, id)) + L.s(" من ١١٤ سورة"), 15, Ui.INK);
            card.addView(st);
            final Button bt = Ui.button(this, L.s("تحميل الكل"), Ui.GREEN, Color.WHITE);
            card.addView(bt);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                bt.setEnabled(false);
                stop = false;
                final android.content.Context ctx = getApplicationContext();
                new Thread(new Runnable() { public void run() {
                    int fail = 0;
                    for (int s = 1; s <= 114 && !stop; s++) {
                        if (!TafsirStore.fetchSurah(ctx, id, s)) { fail++; if (fail > 6) break; }
                        final int n = TafsirStore.cachedCount(ctx, id);
                        runOnUiThread(new Runnable() { public void run() { st.setText(L.s("المحمّل: ") + Ui.ar(n) + L.s(" من ١١٤ سورة")); } });
                    }
                    runOnUiThread(new Runnable() { public void run() { bt.setEnabled(TafsirStore.cachedCount(ctx, id) < 114); bt.setText(TafsirStore.cachedCount(ctx, id) >= 114 ? L.s("تم التحميل كاملاً") : L.s("أكمل التحميل")); } });
                } }).start();
            } });
            body.addView(card);
        }
    }
}
