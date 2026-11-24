package com.hassan.hisn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared look: colors, cards, buttons, headers. All screens are built in code (no XML layouts). */
public class Ui {
    public static final int GREEN = Color.parseColor("#0B6E4F");
    public static final int GREEN_DARK = Color.parseColor("#084C37");
    public static final int GREEN_LIGHT = Color.parseColor("#E5F2EC");
    public static final int CREAM = Color.parseColor("#FBF7EC");
    public static final int GOLD = Color.parseColor("#B8892B");
    public static final int INK = Color.parseColor("#1F2A24");
    public static final int MUTED = Color.parseColor("#6B7A72");

    public static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void rtl(View v) { v.setLayoutDirection(L.en() ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL); }

    public static GradientDrawable rounded(int color, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable gradient(int from, int to, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{from, to});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static TextView text(Context c, String s, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(0, 1.35f);
        return t;
    }

    public static TextView bold(Context c, String s, int sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static Button button(Context c, String s, int bg, int fg) {
        Button b = new Button(c);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setBackground(rounded(bg, 14, c));
        b.setStateListAnimator(null);
        b.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 6), 0, dp(c, 6));
        b.setLayoutParams(lp);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(Color.WHITE, 16, c));
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 7), 0, dp(c, 7));
        l.setLayoutParams(lp);
        l.setElevation(dp(c, 2));
        return l;
    }

    public static TextView section(Context c, String s) {
        TextView t = bold(c, s, 18, GREEN);
        t.setPadding(dp(c, 4), dp(c, 18), dp(c, 4), dp(c, 4));
        return t;
    }

    public static void space(LinearLayout l, int dpv) {
        View v = new View(l.getContext());
        l.addView(v, new LinearLayout.LayoutParams(1, dp(l.getContext(), dpv)));
    }

    /** Wraps a body in a vertical scroll view; returns the inner column. */
    public static LinearLayout scrollBody(Context c, ViewGroup parent) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(false);
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 24));
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
        parent.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        return col;
    }

    /** Top bar with optional back arrow. */
    public static LinearLayout topBar(final Activity a, String title, boolean back) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(GREEN);
        bar.setPadding(dp(a, 8), dp(a, 14), dp(a, 8), dp(a, 14));
        if (back) {
            TextView b = bold(a, L.en() ? "‹" : "›", 30, Color.WHITE);
            b.setPadding(dp(a, 14), 0, dp(a, 14), dp(a, 4));
            b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { a.finish(); } });
            bar.addView(b);
        }
        TextView t = bold(a, title, 21, Color.WHITE);
        t.setGravity(back ? Gravity.START : Gravity.CENTER);
        bar.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        if (back) { View sp = new View(a); bar.addView(sp, new LinearLayout.LayoutParams(dp(a, 52), 1)); }
        return bar;
    }

    /** Standard sub-screen: top bar + scroll body. Returns the body column; call a.setContentView(root) via root(). */
    public static LinearLayout screen(Activity a, String title, LinearLayout[] rootOut) {
        a.getWindow().setStatusBarColor(GREEN);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(CREAM);
        rtl(root);
        root.addView(topBar(a, title, true), new LinearLayout.LayoutParams(-1, -2));
        LinearLayout body = scrollBody(a, root);
        rootOut[0] = root;
        return body;
    }

    public static String ar(String s) {
        if (L.en()) return s;
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) sb.append(ch >= '0' && ch <= '9' ? (char) (ch + 1584) : ch);
        return sb.toString();
    }

    public static String ar(long n) { return ar(String.valueOf(n)); }

    /** Removes Arabic diacritics and normalises letters, for searching. */
    public static String plain(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if ((ch >= 0x064B && ch <= 0x065F) || ch == 0x0670 || ch == 0x0640) continue;
            if (ch == 'أ' || ch == 'إ' || ch == 'آ' || ch == 'ٱ') ch = 'ا';
            if (ch == 'ى') ch = 'ي';
            if (ch == 'ة') ch = 'ه';
            sb.append(ch);
        }
        return sb.toString();
    }
}
