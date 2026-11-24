package com.hassan.hisn;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/** Hadith collections from the open "hadith-api" dataset (jsDelivr), downloaded per collection and split into book files. */
public class HadithStore {
    static final String BASE = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/";
    static final String[][] COLS = {
            {"ara-bukhari", L.s("صحيح البخاري"), "9"},
            {"ara-muslim", L.s("صحيح مسلم"), "8"},
            {"ara-abudawud", L.s("سنن أبي داود"), "6"},
            {"ara-tirmidhi", L.s("جامع الترمذي"), "6"},
            {"ara-nasai", L.s("سنن النسائي"), "6"},
            {"ara-ibnmajah", L.s("سنن ابن ماجه"), "4"},
            {"ara-malik", L.s("موطأ مالك"), "2"},
            {"ara-nawawi", L.s("الأربعون النووية"), "0.05"},
            {"ara-qudsi", L.s("الأحاديث القدسية (الأربعون)"), "0.05"},
            {"ara-dehlawi", L.s("الأربعون الدهلوية"), "0.05"},
    };
    /** collections whose book contains only authentic hadith by the author's condition */
    static boolean sahihain(String ed) { return ed.equals("ara-bukhari") || ed.equals("ara-muslim"); }

    static File dir(Context c, String ed) { File d = new File(new File(c.getFilesDir(), "hadith"), ed); d.mkdirs(); return d; }

    static boolean has(Context c, String ed) { return new File(dir(c, ed), "index.json").length() > 5; }

    static String read(File f) throws Exception {
        return Store.readFile(f);
    }

    static JSONObject index(Context c, String ed) {
        try { return new JSONObject(read(new File(dir(c, ed), "index.json"))); } catch (Exception e) { return null; }
    }

    static JSONArray section(Context c, String ed, int sec) {
        try { return new JSONArray(read(new File(dir(c, ed), "s" + sec + ".json"))); } catch (Exception e) { return new JSONArray(); }
    }

    static boolean download(Context c, String ed) {
        for (String suffix : new String[]{".min.json", ".json"}) {
            try { if (download1(c, ed, BASE + ed + suffix)) return true; } catch (Throwable t) { }
        }
        return false;
    }

    static boolean download1(Context c, String ed, String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(20000);
        h.setReadTimeout(60000);
        if (h.getResponseCode() != 200) { h.disconnect(); return false; }
        JsonReader r = new JsonReader(new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"), 65536));
        String name = ed;
        TreeMap<Integer, String> secNames = new TreeMap<>();
        TreeMap<Integer, JSONArray> secs = new TreeMap<>();
        int total = 0;
        r.beginObject();
        while (r.hasNext()) {
            String k = r.nextName();
            if (k.equals("metadata")) {
                r.beginObject();
                while (r.hasNext()) {
                    String m = r.nextName();
                    if (m.equals("name") && r.peek() == JsonToken.STRING) name = r.nextString();
                    else if (m.equals("sections") && r.peek() == JsonToken.BEGIN_OBJECT) {
                        r.beginObject();
                        while (r.hasNext()) { String id = r.nextName(); String v = r.peek() == JsonToken.STRING ? r.nextString() : ""; try { secNames.put(Integer.parseInt(id), v); } catch (Exception e) { } }
                        r.endObject();
                    } else r.skipValue();
                }
                r.endObject();
            } else if (k.equals("hadiths")) {
                r.beginArray();
                while (r.hasNext()) {
                    String num = "", text = "";
                    int book = 0;
                    JSONArray grades = new JSONArray();
                    r.beginObject();
                    while (r.hasNext()) {
                        String f = r.nextName();
                        if (f.equals("arabicnumber") || f.equals("hadithnumber")) {
                            if (r.peek() == JsonToken.NULL) { r.nextNull(); } else { String v = r.nextString(); if (num.isEmpty() || f.equals("hadithnumber")) num = v; }
                        } else if (f.equals("text")) text = r.nextString();
                        else if (f.equals("grades")) {
                            r.beginArray();
                            while (r.hasNext()) {
                                String gn = "", gg = "";
                                r.beginObject();
                                while (r.hasNext()) { String q = r.nextName(); if (q.equals("name")) gn = r.nextString(); else if (q.equals("grade")) gg = r.nextString(); else r.skipValue(); }
                                r.endObject();
                                grades.put(new JSONArray().put(gn).put(gg));
                            }
                            r.endArray();
                        } else if (f.equals("reference")) {
                            r.beginObject();
                            while (r.hasNext()) { String q = r.nextName(); if (q.equals("book")) { try { book = Integer.parseInt(r.nextString()); } catch (Exception e) { book = 0; } } else r.skipValue(); }
                            r.endObject();
                        } else r.skipValue();
                    }
                    r.endObject();
                    JSONArray a = secs.get(book);
                    if (a == null) { a = new JSONArray(); secs.put(book, a); }
                    JSONObject o = new JSONObject();
                    o.put("n", num); o.put("t", text.replace("\u200f", "")); if (grades.length() > 0) o.put("g", grades);
                    a.put(o);
                    total++;
                }
                r.endArray();
            } else r.skipValue();
        }
        r.endObject();
        r.close();
        h.disconnect();
        if (total == 0) return false;
        File d = dir(c, ed);
        JSONObject idx = new JSONObject();
        idx.put("name", name);
        idx.put("total", total);
        JSONObject sj = new JSONObject();
        for (Map.Entry<Integer, JSONArray> e : secs.entrySet()) {
            JSONObject s1 = new JSONObject();
            s1.put("n", secNames.containsKey(e.getKey()) ? secNames.get(e.getKey()) : "");
            s1.put("c", e.getValue().length());
            sj.put(String.valueOf(e.getKey()), s1);
            write(new File(d, "s" + e.getKey() + ".json"), e.getValue().toString());
        }
        idx.put("secs", sj);
        write(new File(d, "index.json"), idx.toString());
        return true;
    }

    static void write(File f, String s) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        o.write(s.getBytes("UTF-8"));
        o.close();
    }

    static String norm(String s) {
        return s.replaceAll("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]", "")
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا').replace('ة', 'ه').replace('ى', 'ي').trim();
    }

    /** Search all downloaded collections. Each result: {edition, section, hadith object}. */
    static List<Object[]> search(Context c, String q, int limit) {
        List<Object[]> out = new ArrayList<>();
        String nq = norm(q);
        if (nq.length() < 2) return out;
        for (String[] col : COLS) {
            if (!has(c, col[0])) continue;
            JSONObject idx = index(c, col[0]);
            if (idx == null) continue;
            JSONObject secs = idx.optJSONObject("secs");
            if (secs == null) continue;
            java.util.Iterator<String> it = secs.keys();
            List<Integer> ids = new ArrayList<>();
            while (it.hasNext()) ids.add(Integer.parseInt(it.next()));
            java.util.Collections.sort(ids);
            for (int sid : ids) {
                JSONArray a = section(c, col[0], sid);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o != null && norm(o.optString("t")).contains(nq)) {
                        out.add(new Object[]{col[0], col[1], sid, o});
                        if (out.size() >= limit) return out;
                    }
                }
            }
        }
        return out;
    }
}
