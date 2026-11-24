package com.hassan.hisn;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TasbihActivity extends Activity {
    int count, target, total;
    TextView big, tot, phraseView;
    SharedPreferences p;
    static final String[] PHRASES = {L.s("سُبْحَانَ اللَّهِ"), L.s("الْحَمْدُ لِلَّهِ"), L.s("اللَّهُ أَكْبَرُ"), L.s("لَا إِلَهَ إِلَّا اللَّهُ"), L.s("أَسْتَغْفِرُ اللَّهَ"), L.s("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ")};
    int phrase;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = Store.p(this);
        count = p.getInt("tasbih_count", 0);
        target = p.getInt("tasbih_target", 33);
        total = p.getInt("tasbih_total", 0);
        phrase = p.getInt("tasbih_phrase", 0);
        LinearLayout[] root = new LinearLayout[1];
        LinearLayout body = Ui.screen(this, L.s("السبحة"), root);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        phraseView = Ui.bold(this, PHRASES[phrase], 26, Ui.GREEN);
        phraseView.setGravity(Gravity.CENTER);
        phraseView.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        phraseView.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            phrase = (phrase + 1) % PHRASES.length; p.edit().putInt("tasbih_phrase", phrase).apply(); phraseView.setText(PHRASES[phrase]);
        } });
        body.addView(phraseView);
        body.addView(Ui.text(this, L.s("اضغط على الذكر لتغييره"), 12, Ui.MUTED));
        big = new TextView(this);
        big.setGravity(Gravity.CENTER);
        big.setTextSize(64);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        big.setTextColor(Color.WHITE);
        big.setBackground(Ui.gradient(Ui.GREEN, Ui.GREEN_DARK, 200, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 230), Ui.dp(this, 230));
        lp.setMargins(0, Ui.dp(this, 24), 0, Ui.dp(this, 24));
        big.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { tap(); } });
        body.addView(big, lp);
        tot = Ui.text(this, "", 15, Ui.MUTED);
        tot.setGravity(Gravity.CENTER);
        body.addView(tot);
        LinearLayout row = new LinearLayout(this);
        for (final int tg : new int[]{33, 99, 100}) {
            Button bt = Ui.button(this, L.s("هدف ") + Ui.ar(tg), tg == target ? Ui.GOLD : Ui.GREEN_LIGHT, tg == target ? Color.WHITE : Ui.GREEN);
            bt.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { target = tg; p.edit().putInt("tasbih_target", tg).apply(); recreate(); } });
            row.addView(bt, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        body.addView(row, new LinearLayout.LayoutParams(-1, -2));
        Button reset = Ui.button(this, L.s("تصفير العداد"), Ui.GREEN_LIGHT, Ui.GREEN);
        reset.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { count = 0; save(); show(); } });
        body.addView(reset);
        show();
        setContentView(root[0]);
    }

    void tap() {
        count++;
        total++;
        boolean done = count >= target;
        try { ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(done ? 200 : 20); } catch (Exception ignored) { }
        if (done) count = 0;
        save();
        show();
    }

    void save() { p.edit().putInt("tasbih_count", count).putInt("tasbih_total", total).apply(); }

    void show() {
        big.setText(Ui.ar(count));
        tot.setText(L.s("الهدف: ") + Ui.ar(target) + L.s("  •  الإجمالي: ") + Ui.ar(total));
    }
}
