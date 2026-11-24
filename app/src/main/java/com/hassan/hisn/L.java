package com.hassan.hisn;

import android.content.Context;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/** Language switch: Arabic by default; English only when chosen in Settings. Literal-level translation from assets/en.json. */
public class L {
    static Context ctx;
    static boolean en;
    static HashMap<String, String> map;
    static String[] titles;
    static JSONObject adhkar;

    public static void init(Context c) {
        ctx = c.getApplicationContext();
        en = "en".equals(Store.p(ctx).getString("lang", "ar"));
        map = null;
    }

    public static boolean en() { return en; }

    public static void set(Context c, boolean english) {
        Store.p(c).edit().putString("lang", english ? "en" : "ar").commit();
        en = english;
    }

    static synchronized void load() {
        if (map != null) return;
        map = new HashMap<>();
        try {
            JSONObject o = new JSONObject(Store.asset(ctx, "en.json"));
            JSONObject m = o.getJSONObject("ui");
            java.util.Iterator<String> it = m.keys();
            while (it.hasNext()) { String k = it.next(); map.put(k, m.getString(k)); }
            JSONArray t = o.getJSONArray("titles");
            titles = new String[t.length()];
            for (int i = 0; i < titles.length; i++) titles[i] = t.getString(i);
            adhkar = o.getJSONObject("adhkar");
        } catch (Exception e) { titles = new String[0]; adhkar = new JSONObject(); }
    }

    /** Translates one source literal. Arabic mode returns it unchanged. */
    public static String s(String ar) {
        if (!en || ctx == null) return ar;
        load();
        String r = map.get(ar);
        return r != null ? r : ar;
    }

    public static String title(int idx, String ar) {
        if (!en || ctx == null) return ar;
        load();
        return idx >= 0 && idx < titles.length ? titles[idx] : ar;
    }

    /** English entry for one dhikr (keys l: transliteration, t: translation, s: source) or null. */
    public static JSONObject adhkarEn(int ch, int i) {
        if (ctx == null) return null;
        load();
        return adhkar.optJSONObject(ch + "_" + i);
    }

    /** Number of repetitions in words. */
    public static String times(int n) {
        if (en) return n == 1 ? "Once" : n + " times";
        if (n == 1) return "مرة واحدة";
        if (n == 2) return "مرتان";
        if (n <= 10) return Ui.ar(n) + " مرات";
        return Ui.ar(n) + " مرة";
    }
}
