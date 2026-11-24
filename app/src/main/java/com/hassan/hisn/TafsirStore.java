package com.hassan.hisn;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/** Tafsir text from the Quran.com (Quran Foundation) API, fetched one surah at a time and cached on the phone. */
public class TafsirStore {
    static final int[] IDS = {16, 91, 14};
    static final String[] NAMES = {L.s("التفسير الميسر"), L.s("تفسير السعدي"), L.s("تفسير ابن كثير")};
    static final String[] AUTHORS = {L.s("مجمع الملك فهد لطباعة المصحف الشريف"), L.s("عبد الرحمن بن ناصر السعدي"), L.s("إسماعيل بن عمر بن كثير")};
    static final String BASE = "https://api.quran.com/api/v4/tafsirs/";

    static JSONObject ayat;

    static synchronized String ayahText(Context c, String key) {
        if (ayat == null) { try { ayat = new JSONObject(Store.asset(c, "ayat.json")); } catch (Exception e) { ayat = new JSONObject(); } }
        return ayat.optString(key, "");
    }

    static File file(Context c, int id, int surah) {
        File d = new File(c.getFilesDir(), "tafsir");
        if (!d.exists()) d.mkdirs();
        return new File(d, id + "_" + surah + ".json");
    }

    static boolean cached(Context c, int id, int surah) { return file(c, id, surah).length() > 10; }

    static int cachedCount(Context c, int id) {
        int n = 0;
        for (int s = 1; s <= 114; s++) if (cached(c, id, s)) n++;
        return n;
    }

    static String http(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        try {
            h.setConnectTimeout(15000);
            h.setReadTimeout(40000);
            if (h.getResponseCode() != 200) throw new Exception("http " + h.getResponseCode());
            InputStream in = h.getInputStream();
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            return o.toString("UTF-8");
        } finally { h.disconnect(); }
    }

    /** Blocking. Downloads a whole surah of one tafsir (all pages of the API answer) and caches it. */
    static boolean fetchSurah(Context c, int id, int surah) {
        if (cached(c, id, surah)) return true;
        try {
            JSONObject out = new JSONObject();
            int page = 1;
            while (page > 0) {
                JSONObject d = new JSONObject(http(BASE + id + "/by_chapter/" + surah + "?per_page=300&page=" + page));
                JSONArray a = d.getJSONArray("tafsirs");
                for (int i = 0; i < a.length(); i++) {
                    JSONObject t = a.getJSONObject(i);
                    out.put(t.getString("verse_key"), t.getString("text"));
                }
                JSONObject pg = d.optJSONObject("pagination");
                page = pg != null && !pg.isNull("next_page") ? pg.getInt("next_page") : 0;
            }
            if (out.length() == 0) return false;
            File f = file(c, id, surah), tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            FileOutputStream os = new FileOutputStream(tmp);
            os.write(out.toString().getBytes("UTF-8"));
            os.close();
            return tmp.renameTo(f);
        } catch (Throwable t) { return false; }
    }

    /** Cached tafsir for one ayah, or null. If the book comments on a group of ayat, returns the text of the group start and sets note[0]. */
    static String get(Context c, int id, int surah, int ayah, String[] note) {
        try {
            JSONObject o = new JSONObject(Store.readFile(file(c, id, surah)));
            for (int a = ayah; a >= 1; a--) {
                String t = o.optString(surah + ":" + a, "");
                if (t.trim().length() > 0) {
                    if (a != ayah && note != null) note[0] = L.s("هذا الشرح من الكتاب يشمل الآيات من ") + Ui.ar(a) + L.s(" إلى ") + Ui.ar(ayah) + L.s(" (تفسير مجمل).");
                    return t;
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }
}
