package com.hassan.hisn;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A small dhikr card drawn over whatever app is open. Needs the "draw over other apps" permission. */
public class OverlayPopup {
    static View current;
    static final Handler H = new Handler(Looper.getMainLooper());
    static final int SHOW_MS = 15000;

    static boolean canShow(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    /** Returns true if the card was put on screen. */
    public static boolean show(final Context ctx, final String text, final String title) {
        final Context c = ctx.getApplicationContext();
        if (!canShow(c)) return false;
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) return false; // screen is off, nobody to read it
        try {
            final WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            dismiss(c);
            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            int pad = Ui.dp(c, 18);
            card.setPadding(pad, pad, pad, Ui.dp(c, 10));
            card.setBackground(Ui.gradient(Ui.GREEN_DARK, Ui.GREEN, 22, c));
            card.setElevation(Ui.dp(c, 12));
            TextView t = Ui.bold(c, title, 15, Color.parseColor("#F4E9C8"));
            t.setGravity(Gravity.CENTER);
            card.addView(t, new LinearLayout.LayoutParams(-1, -2));
            TextView d = Ui.text(c, text, 22, Color.WHITE);
            d.setGravity(Gravity.CENTER);
            d.setLineSpacing(0, 1.3f);
            d.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
            card.addView(d, new LinearLayout.LayoutParams(-1, -2));
            TextView close = Ui.bold(c, L.s("إغلاق"), 16, Color.parseColor("#F4E9C8"));
            close.setGravity(Gravity.CENTER);
            close.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
            close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dismiss(c); } });
            card.addView(close, new LinearLayout.LayoutParams(-1, -2));
            card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
                dismiss(c);
                try { c.startActivity(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
            } });
            int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -2, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP;
            lp.y = Ui.dp(c, 60);
            lp.horizontalMargin = 0.04f;
            wm.addView(card, lp);
            current = card;
            H.postDelayed(new Runnable() { public void run() { if (current == card0(card)) dismiss(c); } }, SHOW_MS);
            return true;
        } catch (Throwable t) {
            current = null;
            return false;
        }
    }

    private static View card0(View v) { return v; }

    public static void dismiss(Context c) {
        View v = current;
        current = null;
        if (v == null) return;
        try { ((WindowManager) c.getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).removeView(v); } catch (Throwable ignored) { }
    }
}
