package com.hassan.hisn;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Quran bookmarks: the first one is the main bookmark, the rest are extras (up to 5 extras). */
public class Bookmarks {
    static final int MAX_EXTRA = 5;

    static List<Integer> all(Context c) {
        List<Integer> r = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Store.p(c).getString("bookmarks", "[]"));
            for (int i = 0; i < a.length(); i++) r.add(a.getInt(i));
        } catch (Exception ignored) { }
        return r;
    }

    static void save(Context c, List<Integer> l) {
        JSONArray a = new JSONArray();
        for (int x : l) a.put(x);
        Store.p(c).edit().putString("bookmarks", a.toString()).apply();
    }

    /** Main bookmark page or 0. */
    static int main(Context c) { List<Integer> l = all(c); return l.isEmpty() ? 0 : l.get(0); }

    static boolean has(Context c, int page) { return all(c).contains(page); }

    /** Make this page the main bookmark (a previous main one becomes an extra if there is room). */
    static void setMain(Context c, int page) {
        List<Integer> l = all(c);
        l.remove((Integer) page);
        l.add(0, page);
        while (l.size() > MAX_EXTRA + 1) l.remove(l.size() - 1);
        save(c, l);
    }

    static void addExtra(Context c, int page) {
        List<Integer> l = all(c);
        if (l.contains(page)) return;
        if (l.isEmpty()) { l.add(page); } else {
            if (l.size() >= MAX_EXTRA + 1) l.remove(l.size() - 1);
            l.add(page);
        }
        save(c, l);
    }

    static void remove(Context c, int page) {
        List<Integer> l = all(c);
        l.remove((Integer) page);
        save(c, l);
    }

    static String label(Context c, int page) {
        String name = "";
        try {
            JSONArray s = Store.mushaf(c).getJSONArray("surahs");
            for (int i = 0; i < s.length(); i++) { JSONObject o = s.getJSONObject(i); if (o.getInt("p") <= page) name = o.getString("n"); }
        } catch (Exception ignored) { }
        return L.s("صفحة ") + Ui.ar(page) + (name.isEmpty() ? "" : "  •  " + name);
    }
}
