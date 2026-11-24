package com.hassan.hisn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

/** Mushaf reader: one Madani mushaf page per screen. Tap = hide/show the bars, swipe = turn page, long press on an ayah = select it and open its tafsir. */
public class ReaderActivity extends Activity {
    MushafView mv;
    TextView title, pageTop, ribbon;
    int page;
    boolean night, immersive;
    GestureDetector gd;
    LinearLayout root, bar, toolbar;
    TextView hizbPop;
    org.json.JSONObject rubData;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        night = Store.p(this).getBoolean("reader_night", false);
        page = getIntent().getIntExtra("page", Store.p(this).getInt("last_page", 1));
        getWindow().setStatusBarColor(Ui.GREEN_DARK);
        getWindow().setNavigationBarColor(Color.parseColor("#14181A"));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.rtl(root);
        bar = Ui.topBar(this, "", true);
        bar.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        bar.setBackgroundColor(Ui.GREEN_DARK);
        title = (TextView) bar.getChildAt(1);
        title.setTextSize(16);
        bar.removeViewAt(bar.getChildCount() - 1);
        pageTop = Ui.bold(this, "", 15, Color.WHITE);
        pageTop.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        pageTop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { jump(); } });
        bar.addView(pageTop);
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        mv = new MushafView(this);
        mv.setNight(night);
        FrameLayout fl = new FrameLayout(this);
        fl.addView(mv, new FrameLayout.LayoutParams(-1, -1));
        ribbon = Ui.bold(this, "", 26, Color.parseColor("#D64545"));
        ribbon.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 6));
        ribbon.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggleMain(); } });
        FrameLayout.LayoutParams rl = new FrameLayout.LayoutParams(-2, -2);
        rl.gravity = Gravity.TOP | Gravity.LEFT;
        fl.addView(ribbon, rl);
        hizbPop = Ui.bold(this, "", 20, Color.WHITE);
        hizbPop.setBackground(Ui.rounded(Color.parseColor("#E6084C37"), 24, this));
        hizbPop.setPadding(Ui.dp(this, 22), Ui.dp(this, 10), Ui.dp(this, 22), Ui.dp(this, 10));
        hizbPop.setVisibility(View.GONE);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2);
        hp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        hp.topMargin = Ui.dp(this, 14);
        fl.addView(hizbPop, hp);
        try { rubData = new org.json.JSONObject(Store.asset(this, "rub.json")); } catch (Exception e) { rubData = new org.json.JSONObject(); }
        root.addView(fl, new LinearLayout.LayoutParams(-1, 0, 1f));
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(Color.parseColor("#14181A"));
        toolbar.addView(tool("🔖", L.s("حفظ علامة"), new View.OnClickListener() { public void onClick(View v) { toggleMain(); } }), new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.addView(tool("↩", L.s("إلى العلامة"), new View.OnClickListener() { public void onClick(View v) {
            int m = Bookmarks.main(ReaderActivity.this);
            if (m > 0) { page = m; show(); } else Toast.makeText(ReaderActivity.this, L.s("لا توجد علامة بعد. اضغط على الشريط الأحمر لوضع علامة"), Toast.LENGTH_LONG).show();
        } }), new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.addView(tool("📑", L.s("الفهرس"), new View.OnClickListener() { public void onClick(View v) {
            startActivity(new Intent(ReaderActivity.this, MainActivity.class).putExtra("tab", 2).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        } }), new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.addView(tool("🔢", L.s("صفحة"), new View.OnClickListener() { public void onClick(View v) { jump(); } }), new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.addView(tool("🌓", L.s("الوضع"), new View.OnClickListener() { public void onClick(View v) {
            night = !night; Store.p(ReaderActivity.this).edit().putBoolean("reader_night", night).apply(); mv.setNight(night);
        } }), new LinearLayout.LayoutParams(0, -2, 1f));
        toolbar.addView(tool("⋯", L.s("المزيد"), new View.OnClickListener() { public void onClick(View v) { moreDialog(); } }), new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent e) { toggleUi(); return true; }
            @Override public void onLongPress(MotionEvent e) {
                String k = mv.keyAt(e.getX(), e.getY());
                if (k != null) ayahDialog(k);
            }
            @Override public boolean onFling(MotionEvent a, MotionEvent e, float vx, float vy) {
                float dx = e.getX() - a.getX(), dy = e.getY() - a.getY();
                if (Math.abs(dx) > Math.abs(dy) * 1.3f && Math.abs(dx) > Ui.dp(ReaderActivity.this, 60)) {
                    go(dx > 0 ? 1 : -1); // finger moves right = next page (right-to-left book)
                    return true;
                }
                return false;
            }
        });
        mv.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP && mv.failed) mv.setPage(page);
            return gd.onTouchEvent(e); } });
        show();
    }

    View tool(String icon, String label, View.OnClickListener l) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        TextView i = Ui.text(this, icon, 20, Color.WHITE);
        i.setGravity(Gravity.CENTER);
        TextView s = Ui.text(this, label, 11, Color.parseColor("#CFE8DC"));
        s.setGravity(Gravity.CENTER);
        t.addView(i);
        t.addView(s);
        t.setOnClickListener(l);
        return t;
    }

    void toggleUi() {
        immersive = !immersive;
        bar.setVisibility(immersive ? View.GONE : View.VISIBLE);
        toolbar.setVisibility(immersive ? View.GONE : View.VISIBLE);
        applySystemUi();
    }

    @SuppressWarnings("deprecation")
    void applySystemUi() {
        View d = getWindow().getDecorView();
        if (immersive) d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        else d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f && immersive) applySystemUi(); }

    void ayahDialog(final String key) {
        mv.select(key);
        String[] kv = key.split(":");
        final int s = Integer.parseInt(kv[0]), a = Integer.parseInt(kv[1]);
        String sn = "";
        try { sn = Store.mushaf(this).getJSONArray("surahs").getJSONObject(s - 1).getString("n"); } catch (Exception ignored) { }
        new AlertDialog.Builder(this).setTitle(L.s("سورة ") + sn + L.s(" • الآية ") + Ui.ar(a))
                .setItems(new String[]{L.s("📖 التفسير (الميسر، السعدي، ابن كثير)"), L.s("🔖 اجعل صفحة هذه الآية علامتي"), L.s("📋 نسخ الآية")}, new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) {
                    if (w == 0) startActivity(new Intent(ReaderActivity.this, TafsirActivity.class).putExtra("key", key));
                    else if (w == 1) { Bookmarks.setMain(ReaderActivity.this, page); refreshMarks(); Toast.makeText(ReaderActivity.this, L.s("تم وضع العلامة"), Toast.LENGTH_SHORT).show(); }
                    else {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("ayah", TafsirStore.ayahText(ReaderActivity.this, key)));
                        Toast.makeText(ReaderActivity.this, L.s("تم النسخ"), Toast.LENGTH_SHORT).show();
                    }
                } })
                .setOnDismissListener(new android.content.DialogInterface.OnDismissListener() { public void onDismiss(android.content.DialogInterface d) { mv.select(null); } })
                .show();
    }

    void moreDialog() {
        String[] it = {L.s("العلامات (الرئيسية والإضافية)"), L.s("تحميل المصحف كاملاً للاستخدام بدون إنترنت"), L.s("تحميل التفاسير للاستخدام بدون إنترنت"), L.s("نصيحة: اضغط مطولاً على أي آية لفتح تفسيرها")};
        new AlertDialog.Builder(this).setItems(it, new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) {
            if (w == 0) bookmarksDialog();
            else if (w == 1) downloadAll();
            else if (w == 2) startActivity(new Intent(ReaderActivity.this, TafsirActivity.class).putExtra("manage", true));
        } }).show();
    }

    void go(int d) {
        int np = page + d;
        if (np < 1 || np > 604) return;
        final int dir = d;
        mv.animate().translationX(dir * mv.getWidth() * 0.35f).alpha(0f).setDuration(110).withEndAction(new Runnable() { public void run() {
            page += dir;
            show();
            mv.setTranslationX(-dir * mv.getWidth() * 0.35f);
            mv.animate().translationX(0).alpha(1f).setDuration(130).start();
        } }).start();
    }

    void show() {
        mv.setPage(page);
        String name = "";
        int juz = 1;
        try {
            JSONObject m = Store.mushaf(this);
            JSONArray s = m.getJSONArray("surahs");
            for (int i = 0; i < s.length(); i++) if (s.getJSONObject(i).getInt("p") <= page) name = s.getJSONObject(i).getString("n");
            juz = m.getJSONArray("pages").getJSONObject(page - 1).getInt("j");
        } catch (Exception ignored) { }
        title.setText(L.s("سورة ") + name + L.s("  •  الجزء ") + Ui.ar(juz));
        pageTop.setText(Ui.ar(page) + "  •  " + mv.hizbLabel());
        Store.p(this).edit().putInt("last_page", page).apply();
        refreshMarks();
        showHizbPopup();
    }

    /** Brief popup when the open page holds the start of a hizb, quarter, half or three quarters. */
    void showHizbPopup() {
        JSONArray a = rubData == null ? null : rubData.optJSONArray(String.valueOf(page));
        if (a == null || a.length() == 0) { hizbPop.setVisibility(View.GONE); return; }
        int q = a.optInt(0);
        int hz = (q - 1) / 4 + 1, pos = (q - 1) % 4;
        String[] names = {L.s("بداية الحزب "), L.s("ربع الحزب "), L.s("نصف الحزب "), L.s("ثلاثة أرباع الحزب ")};
        hizbPop.setText(names[pos] + Ui.ar(hz));
        hizbPop.animate().cancel();
        hizbPop.setAlpha(1f);
        hizbPop.setVisibility(View.VISIBLE);
        hizbPop.animate().alpha(0f).setStartDelay(1500).setDuration(500).withEndAction(new Runnable() { public void run() { hizbPop.setVisibility(View.GONE); } }).start();
    }

    void refreshMarks() {
        int m = Bookmarks.main(this);
        boolean isMain = m == page, isExtra = !isMain && Bookmarks.has(this, page);
        ribbon.setText(isMain || isExtra ? "🔖" : "🏷");
        ribbon.setAlpha(isMain || isExtra ? 1f : 0.4f);
    }

    void toggleMain() {
        if (Bookmarks.main(this) == page) { Bookmarks.remove(this, page); Toast.makeText(this, L.s("أُزيلت العلامة"), Toast.LENGTH_SHORT).show(); }
        else { Bookmarks.setMain(this, page); Toast.makeText(this, L.s("تم وضع علامتك الرئيسية هنا (ص ") + Ui.ar(page) + ")", Toast.LENGTH_SHORT).show(); }
        refreshMarks();
    }

    void bookmarksDialog() {
        final java.util.List<Integer> l = Bookmarks.all(this);
        java.util.List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < l.size(); i++) items.add((i == 0 ? L.s("★ علامتي الرئيسية: ") : L.s("علامة: ")) + Bookmarks.label(this, l.get(i)));
        items.add(L.s("＋ اجعل هذه الصفحة علامتي الرئيسية"));
        items.add(L.s("＋ أضف هذه الصفحة كعلامة إضافية"));
        if (!l.isEmpty()) items.add(L.s("🗑 احذف علامة"));
        new AlertDialog.Builder(this).setTitle(L.s("العلامات")).setItems(items.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) {
            if (w < l.size()) { page = l.get(w); show(); return; }
            int k = w - l.size();
            if (k == 0) { Bookmarks.setMain(ReaderActivity.this, page); refreshMarks(); }
            else if (k == 1) {
                if (Bookmarks.all(ReaderActivity.this).size() >= Bookmarks.MAX_EXTRA + 1) Toast.makeText(ReaderActivity.this, L.s("وصلت للحد الأقصى، سيُحذف أقدم علامة إضافية"), Toast.LENGTH_LONG).show();
                Bookmarks.addExtra(ReaderActivity.this, page); refreshMarks();
            } else deleteDialog();
        } }).setNegativeButton(L.s("إغلاق"), null).show();
    }

    void deleteDialog() {
        final java.util.List<Integer> l = Bookmarks.all(this);
        String[] it = new String[l.size()];
        for (int i = 0; i < it.length; i++) it[i] = Bookmarks.label(this, l.get(i));
        new AlertDialog.Builder(this).setTitle(L.s("احذف علامة")).setItems(it, new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) {
            Bookmarks.remove(ReaderActivity.this, l.get(w)); refreshMarks();
        } }).setNegativeButton(L.s("إلغاء"), null).show();
    }

    volatile boolean stopDl;

    void downloadAll() {
        final int have = FontStore.cachedCount(this);
        if (have >= 604) { Toast.makeText(this, L.s("المصحف محمّل بالكامل ويعمل بدون إنترنت"), Toast.LENGTH_LONG).show(); return; }
        final TextView msg = Ui.text(this, L.s("سيتم تنزيل نحو ٩٠ ميجابايت (يفضّل واي فاي) ليعمل المصحف كاملاً بدون إنترنت."), 16, Ui.INK);
        msg.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 4));
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(L.s("تحميل المصحف")).setView(msg)
                .setPositiveButton(L.s("ابدأ"), null).setNegativeButton(L.s("إغلاق"), new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) { stopDl = true; } }).create();
        dlg.setCancelable(false);
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() { public void onClick(final View b) {
            b.setEnabled(false);
            stopDl = false;
            final android.content.Context ctx = getApplicationContext();
            new Thread(new Runnable() { public void run() {
                int fail = 0;
                for (int p = 1; p <= 604 && !stopDl; p++) {
                    if (!FontStore.fetch(ctx, p)) { fail++; if (fail > 8) break; }
                    if (p % 5 == 0) { final int n = FontStore.cachedCount(ctx); runOnUiThread(new Runnable() { public void run() { msg.setText(L.s("تم تحميل ") + Ui.ar(n) + L.s(" من ") + Ui.ar(604) + L.s(" صفحة")); } }); }
                }
                final boolean done = FontStore.cachedCount(ctx) >= 604;
                runOnUiThread(new Runnable() { public void run() {
                    msg.setText(done ? L.s("تم تحميل المصحف كاملاً. يعمل الآن بدون إنترنت.") : (stopDl ? L.s("تم الإيقاف. يمكنك المتابعة لاحقاً.") : L.s("تعذّر إكمال التحميل. تأكد من الإنترنت وحاول مرة أخرى.")));
                    b.setEnabled(!done);
                } });
            } }).start();
        } });
    }

    void jump() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(L.s("١ - ٦٠٤"));
        new AlertDialog.Builder(this).setTitle(L.s("اذهب إلى صفحة")).setView(et)
                .setPositiveButton(L.s("اذهب"), new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) {
                    try { int p = Integer.parseInt(et.getText().toString().trim()); if (p >= 1 && p <= 604) { page = p; show(); } } catch (Exception ignored) { }
                } }).setNegativeButton(L.s("إلغاء"), null).show();
    }
}
