package com.hassan.hisn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.LruCache;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;

/** Draws one page of the Madani mushaf (15 lines) using the per-page KFGQPC glyph font. */
public class MushafView extends View {
    static final LruCache<Integer, Typeface> FONTS = new LruCache<>(8);
    int page = 1;
    String[] lines = new String[15];
    String[] keys = new String[15];
    int rub = 1;
    String selKey = null;
    float[][] tokX = new float[15][];
    String[][] tokK = new String[15][];
    float lastTop, lastLh;
    Typeface font, bismFont;
    String bism = "";
    String[] surahNames = new String[115];
    boolean night = false, failed = false;
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint deco = new Paint(Paint.ANTI_ALIAS_FLAG);
    static float refNatural100 = 0; // natural width of a full line at 100px, measured on a normal page
    static final float GAP = 0.12f;

    public MushafView(Context c) {
        super(c);
        try {
            JSONObject m = Store.mushaf(c);
            bism = m.optString("bism", "");
            JSONArray s = m.getJSONArray("surahs");
            for (int i = 0; i < s.length(); i++) surahNames[i + 1] = s.getJSONObject(i).getString("n");
        } catch (Exception ignored) { }
        bismFont = font(c, 1);
    }

    static Typeface font(Context c, int p) {
        Typeface t = FONTS.get(p);
        if (t == null) {
            try {
                if (FontStore.cached(c, p)) t = Typeface.createFromFile(FontStore.file(c, p));
                else if (FontStore.bundled(p)) t = Typeface.createFromAsset(c.getAssets(), "fonts/p" + p + ".ttf");
            } catch (Throwable e) { t = null; FontStore.file(c, p).delete(); }
            if (t != null) FONTS.put(p, t);
        }
        return t;
    }

    /** Verse key (like "2:255") under a touch point, or null. */
    public String keyAt(float x, float y) {
        if (lastLh <= 0) return null;
        int line = (int) ((y - lastTop) / lastLh);
        if (line < 0 || line > 14 || tokX[line] == null) return null;
        float[] tx = tokX[line];
        int best = -1; float bd = 1e9f;
        for (int i = 0; i < tx.length / 2; i++) {
            float d = x < tx[2 * i] ? tx[2 * i] - x : (x > tx[2 * i + 1] ? x - tx[2 * i + 1] : 0);
            if (d < bd) { bd = d; best = i; }
        }
        return best < 0 ? null : tokK[line][best];
    }

    public void select(String key) { selKey = key; invalidate(); }

    /** Hizb label for the page start, like L.s("الحزب ١٥ ¾"). */
    public String hizbLabel() {
        int h = (rub - 1) / 4 + 1, q = (rub - 1) % 4;
        String[] qs = {"", " ¼", " ½", " ¾"};
        return L.s("الحزب ") + Ui.ar(h) + qs[q];
    }

    public void setNight(boolean n) { night = n; invalidate(); }

    public void setPage(int p) {
        page = Math.max(1, Math.min(604, p));
        try {
            JSONArray l = Store.mushaf(getContext()).getJSONArray("pages").getJSONObject(page - 1).getJSONArray("l");
            for (int i = 0; i < 15; i++) lines[i] = i < l.length() ? l.getString(i) : "";
            JSONObject po = Store.mushaf(getContext()).getJSONArray("pages").getJSONObject(page - 1);
            JSONArray k = po.optJSONArray("k");
            for (int i = 0; i < 15; i++) keys[i] = k != null && i < k.length() ? k.getString(i) : "";
            rub = po.optInt("r", 1);
        } catch (Exception e) { for (int i = 0; i < 15; i++) lines[i] = ""; }
        selKey = null;
        font = font(getContext(), page);
        failed = false;
        if (font == null) {
            final int want = page;
            final Context ctx = getContext();
            new Thread(new Runnable() { public void run() {
                final boolean ok = FontStore.fetch(ctx, want);
                post(new Runnable() { public void run() {
                    if (page != want) return;
                    if (ok) setPage(want); else { failed = true; invalidate(); }
                } });
                if (ok) { FontStore.fetch(ctx, want + 1); FontStore.fetch(ctx, want + 2); FontStore.fetch(ctx, want - 1); }
            } }).start();
        } else {
            final Context ctx = getContext();
            final int want = page;
            new Thread(new Runnable() { public void run() { FontStore.fetch(ctx, want + 1); FontStore.fetch(ctx, want + 2); } }).start();
        }
        invalidate();
    }

    float natural(Paint pt, String line, float size) {
        pt.setTextSize(size);
        float sum = 0;
        String[] w = line.split(" ");
        for (String x : w) sum += pt.measureText(x);
        return sum + GAP * size * (w.length - 1);
    }

    /** Reference size: font size at which a full line fills the width on a typical page. */
    float fitSize(float avail) {
        Paint pt = new Paint(paint);
        Typeface ref = page <= 2 ? font(getContext(), 3) : font;
        if (ref == null) ref = Typeface.DEFAULT;
        String[] src = lines;
        if (page <= 2) {
            try {
                JSONArray l = Store.mushaf(getContext()).getJSONArray("pages").getJSONObject(2).getJSONArray("l");
                src = new String[15];
                for (int i = 0; i < 15; i++) src[i] = l.getString(i);
            } catch (Exception ignored) { }
        }
        pt.setTypeface(ref);
        float max = 1;
        for (String s : src) if (s.length() > 0 && s.charAt(0) != '#' && s.charAt(0) != '*') max = Math.max(max, natural(pt, s, 100));
        return 100 * avail / max;
    }

    @Override
    protected void onDraw(Canvas c) {
        float W = getWidth(), H = getHeight();
        if (font == null) {
            c.drawColor(night ? Color.parseColor("#14181A") : Color.parseColor("#FBF7EC"));
            Paint m = new Paint(Paint.ANTI_ALIAS_FLAG);
            m.setColor(night ? Color.parseColor("#E9E2CF") : Color.parseColor("#1F2A24"));
            m.setTextAlign(Paint.Align.CENTER);
            m.setTextSize(Ui.dp(getContext(), 18));
            c.drawText(failed ? L.s("تعذّر تحميل الصفحة. تأكد من الإنترنت ثم اضغط للمحاولة") : L.s("جارٍ تحميل صفحة المصحف..."), W / 2, H / 2, m);
            return;
        }
        int ink = night ? Color.parseColor("#E9E2CF") : Color.parseColor("#1F2A24");
        c.drawColor(night ? Color.parseColor("#14181A") : Color.parseColor("#FBF7EC"));
        float margin = Ui.dp(getContext(), 10);
        float avail = W - 2 * margin;
        float size = fitSize(avail);
        float lh = Math.min(H / 15f, size * 2.1f);
        // keep the text inside the height too
        if (lh < size * 1.35f) { size = lh / 1.35f; }
        float top = (H - 15 * lh) / 2f;
        lastTop = top; lastLh = lh;
        paint.setTypeface(font);
        paint.setColor(ink);
        for (int i = 0; i < 15; i++) {
            String l = lines[i];
            float cy = top + i * lh + lh / 2f;
            tokX[i] = null;
            if (l == null || l.length() == 0) continue;
            if (l.charAt(0) == '#') { header(c, l, margin, cy, avail, lh, size, ink); continue; }
            if (l.charAt(0) == '*') {
                paint.setTypeface(bismFont);
                paint.setTextSize(size);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(ink);
                drawBase(c, bism.replace(" ", ""), W / 2f, cy, size);
                paint.setTypeface(font);
                continue;
            }
            paint.setTypeface(font);
            paint.setColor(ink);
            drawLine(c, l, margin, W - margin, cy, size, avail, i, lh);
        }
    }

    void drawBase(Canvas c, String t, float x, float cy, float size) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(t, x, cy - (fm.ascent + fm.descent) / 2f, paint);
    }

    void drawLine(Canvas c, String line, float left, float right, float cy, float size, float avail, int idx, float lh) {
        String[] w = line.split(" ");
        float s = size;
        paint.setTextSize(s);
        float[] widths = new float[w.length];
        float sum = 0;
        for (int i = 0; i < w.length; i++) { widths[i] = paint.measureText(w[i]); sum += widths[i]; }
        float natural = sum + GAP * s * (w.length - 1);
        float gap;
        float x;
        if (sum > avail) { // too wide: shrink this line
            float k = avail / (sum + GAP * s * (w.length - 1));
            s *= k; paint.setTextSize(s);
            for (int i = 0; i < w.length; i++) widths[i] *= k;
            sum *= k;
            gap = w.length > 1 ? (avail - sum) / (w.length - 1) : 0;
            x = right;
        } else if (natural >= 0.88f * avail) { // justified line
            gap = w.length > 1 ? (avail - sum) / (w.length - 1) : 0;
            x = right;
        } else { // short line (end of surah, pages 1 and 2): centered
            gap = GAP * s;
            x = (left + right) / 2f + natural / 2f;
        }
        paint.setTextAlign(Paint.Align.LEFT);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float base = cy - (fm.ascent + fm.descent) / 2f;
        String[] kk = keys[idx] == null ? new String[0] : keys[idx].split(" ");
        boolean ok = kk.length == w.length;
        tokX[idx] = new float[w.length * 2];
        tokK[idx] = new String[w.length];
        int oldColor = paint.getColor();
        for (int i = 0; i < w.length; i++) {
            x -= widths[i];
            tokX[idx][2 * i] = x - gap / 2f;
            tokX[idx][2 * i + 1] = x + widths[i] + gap / 2f;
            tokK[idx][i] = ok ? kk[i] : null;
            if (ok && selKey != null && selKey.equals(kk[i])) {
                deco.setStyle(Paint.Style.FILL);
                deco.setColor(night ? Color.parseColor("#3A4A2A") : Color.parseColor("#F3DFA0"));
                c.drawRect(x - gap / 2f, cy - lh / 2f, x + widths[i] + gap / 2f, cy + lh / 2f, deco);
                paint.setColor(oldColor);
            }
            c.drawText(w[i], x, base, paint);
            x -= gap;
        }
    }

    void header(Canvas c, String l, float margin, float cy, float avail, float lh, float size, int ink) {
        int n = 1;
        try { n = Integer.parseInt(l.substring(1)); } catch (Exception ignored) { }
        float h = Math.min(lh * 0.86f, size * 1.7f);
        RectF r = new RectF(margin, cy - h / 2, margin + avail, cy + h / 2);
        deco.setStyle(Paint.Style.FILL);
        deco.setColor(night ? Color.parseColor("#22301F") : Color.parseColor("#EFE4C4"));
        c.drawRoundRect(r, h / 4, h / 4, deco);
        deco.setStyle(Paint.Style.STROKE);
        deco.setStrokeWidth(Ui.dp(getContext(), 2));
        deco.setColor(Ui.GOLD);
        c.drawRoundRect(r, h / 4, h / 4, deco);
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(h * 0.5f);
        t.setTextAlign(Paint.Align.CENTER);
        t.setColor(night ? Color.parseColor("#E9E2CF") : Ui.GREEN_DARK);
        Paint.FontMetrics fm = t.getFontMetrics();
        c.drawText(L.s("سورة ") + (n >= 1 && n <= 114 && surahNames[n] != null ? surahNames[n] : ""), margin + avail / 2f, cy - (fm.ascent + fm.descent) / 2f, t);
    }
}
