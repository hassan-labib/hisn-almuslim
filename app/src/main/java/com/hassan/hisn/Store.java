package com.hassan.hisn;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

/** Preferences, bundled data and location helpers. */
public class Store {
    public static final double CAIRO_LAT = 30.0444, CAIRO_LNG = 31.2357;
    public static final int[] IQAMA_DEFAULT = {20, 15, 15, 10, 15};

    public static String readFile(java.io.File f) throws Exception {
        try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    public static SharedPreferences p(Context c) { return c.getSharedPreferences("hisn", 0); }

    public static String asset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) { return null; }
    }

    static JSONObject mushafCache;
    static JSONArray hisnCache;

    public static synchronized JSONObject mushaf(Context c) {
        if (mushafCache == null) {
            try { mushafCache = new JSONObject(asset(c, "mushaf.json")); } catch (Exception e) { mushafCache = new JSONObject(); }
        }
        return mushafCache;
    }

    public static synchronized JSONArray hisn(Context c) {
        if (hisnCache == null) {
            try { hisnCache = new JSONArray(asset(c, "hisn.json")); } catch (Exception e) { hisnCache = new JSONArray(); }
        }
        return hisnCache;
    }

    public static boolean hasSavedLocation(Context c) { return p(c).contains("lat"); }

    public static double[] latLng(Context c) {
        SharedPreferences p = p(c);
        try {
            LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            Iterator<String> it = lm.getProviders(true).iterator();
            Location best = null;
            while (it.hasNext()) {
                Location l = lm.getLastKnownLocation(it.next());
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best != null) {
                p.edit().putFloat("lat", (float) best.getLatitude()).putFloat("lng", (float) best.getLongitude()).apply();
                return new double[]{best.getLatitude(), best.getLongitude()};
            }
        } catch (RuntimeException ignored) { }
        return p.contains("lat") ? new double[]{p.getFloat("lat", 0), p.getFloat("lng", 0)} : new double[]{CAIRO_LAT, CAIRO_LNG};
    }

    /** One fresh location fix (needs location permission), then runs done on the main thread. */
    public static void refreshLocation(final Context c, final Runnable done) {
        try {
            final LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            String prov = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
            lm.requestSingleUpdate(prov, new LocationListener() {
                public void onLocationChanged(Location l) {
                    p(c).edit().putFloat("lat", (float) l.getLatitude()).putFloat("lng", (float) l.getLongitude()).apply();
                    Scheduler.scheduleAll(c, true);
                    if (done != null) done.run();
                }
                public void onStatusChanged(String s, int i, Bundle b) { }
                public void onProviderEnabled(String s) { }
                public void onProviderDisabled(String s) { }
            }, android.os.Looper.getMainLooper());
        } catch (Exception e) { if (done != null) done.run(); }
    }

    public static int method(Context c) { return p(c).getInt("method", 0); }
    public static boolean hanafi(Context c) { return p(c).getBoolean("hanafi", false); }

    public static long[] today(Context c) {
        double[] ll = latLng(c);
        return PrayerTimes.millis(ll[0], ll[1], Calendar.getInstance(), method(c), hanafi(c));
    }

    public static int iqamaMinutes(Context c, int prayer) { return p(c).getInt("iqama_" + prayer, IQAMA_DEFAULT[prayer]); }
}
