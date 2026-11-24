package com.hassan.hisn;

import java.util.Calendar;
import java.util.TimeZone;

/** Offline prayer time calculation (solar position + twilight angles). Index: 0 Fajr, 1 Sunrise, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha. */
public class PrayerTimes {
    public static final String[] NAMES = {L.s("الفجر"), L.s("الشروق"), L.s("الظهر"), L.s("العصر"), L.s("المغرب"), L.s("العشاء")};
    public static final String[] METHOD_NAMES = {
            L.s("الهيئة المصرية العامة للمساحة"), L.s("رابطة العالم الإسلامي"), L.s("جامعة العلوم الإسلامية بكراتشي"),
            L.s("الجمعية الإسلامية لأمريكا الشمالية"), L.s("أم القرى (مكة المكرمة)")};
    /** fajr angle, isha angle, isha minutes after maghrib (0 = use angle) */
    static final double[][] METHODS = {{19.5, 17.5, 0}, {18, 17, 0}, {18, 18, 0}, {15, 15, 0}, {18.5, 0, 90}};

    static double rad(double d) { return Math.toRadians(d); }
    static double fix(double a, double b) { double r = a - b * Math.floor(a / b); return r < 0 ? r + b : r; }

    static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** returns {declination degrees, equation of time hours} */
    static double[] sun(double jd) {
        double D = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * Math.sin(rad(g)) + 0.020 * Math.sin(rad(2 * g)), 360);
        double e = 23.439 - 0.00000036 * D;
        double ra = Math.toDegrees(Math.atan2(Math.cos(rad(e)) * Math.sin(rad(L)), Math.cos(rad(L)))) / 15.0;
        ra = fix(ra, 24);
        double eqt = q / 15.0 - ra;
        if (eqt > 12) eqt -= 24;
        if (eqt < -12) eqt += 24;
        double decl = Math.toDegrees(Math.asin(Math.sin(rad(e)) * Math.sin(rad(L))));
        return new double[]{decl, eqt};
    }

    static double midDay(double jd, double t) { return fix(12 - sun(jd + t)[1], 24); }

    static double angleTime(double jd, double lat, double angle, double t, boolean ccw) {
        double decl = sun(jd + t)[0];
        double noon = midDay(jd, t);
        double x = (-Math.sin(rad(angle)) - Math.sin(rad(decl)) * Math.sin(rad(lat))) / (Math.cos(rad(decl)) * Math.cos(rad(lat)));
        if (x > 1 || x < -1) return Double.NaN;
        double z = Math.toDegrees(Math.acos(x)) / 15.0;
        return noon + (ccw ? -z : z);
    }

    static double asrTime(double jd, double lat, double factor, double t) {
        double decl = sun(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + Math.tan(rad(Math.abs(lat - decl))))));
        return angleTime(jd, lat, angle, t, false);
    }

    /** hours (local, may be NaN) for the date in calendar. */
    public static double[] hours(double lat, double lng, int y, int m, int d, double tz, int method, boolean hanafi) {
        double[] P = METHODS[Math.max(0, Math.min(METHODS.length - 1, method))];
        double jd = julian(y, m, d) - lng / (15.0 * 24.0);
        double[] t = {5, 6, 12, 13, 18, 18};
        double[] r = new double[6];
        for (int it = 0; it < 3; it++) {
            r[0] = angleTime(jd, lat, P[0], t[0] / 24, true);
            r[1] = angleTime(jd, lat, 0.833, t[1] / 24, true);
            r[2] = midDay(jd, t[2] / 24);
            r[3] = asrTime(jd, lat, hanafi ? 2 : 1, t[3] / 24);
            r[4] = angleTime(jd, lat, 0.833, t[4] / 24, false);
            r[5] = P[2] > 0 ? Double.NaN : angleTime(jd, lat, P[1], t[5] / 24, false);
            for (int i = 0; i < 6; i++) if (!Double.isNaN(r[i])) t[i] = r[i];
        }
        double adj = tz - lng / 15.0;
        for (int i = 0; i < 6; i++) r[i] += adj;
        // high latitude / minutes-after-maghrib handling
        if (P[2] > 0) r[5] = r[4] + P[2] / 60.0;
        double night = fix(r[1] - r[4], 24);
        if (Double.isNaN(night) || night == 0) night = 12;
        if (Double.isNaN(r[0])) r[0] = r[1] - night * (P[0] / 60.0);
        if (Double.isNaN(r[5])) r[5] = r[4] + night * (P[1] / 60.0);
        return r;
    }

    public static long[] millis(double lat, double lng, Calendar day, int method, boolean hanafi) {
        TimeZone tz = day.getTimeZone();
        Calendar noon = (Calendar) day.clone();
        noon.set(Calendar.HOUR_OF_DAY, 12); noon.set(Calendar.MINUTE, 0); noon.set(Calendar.SECOND, 0); noon.set(Calendar.MILLISECOND, 0);
        double off = tz.getOffset(noon.getTimeInMillis()) / 3600000.0;
        double[] h = hours(lat, lng, day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH), off, method, hanafi);
        Calendar mid = (Calendar) day.clone();
        mid.set(Calendar.HOUR_OF_DAY, 0); mid.set(Calendar.MINUTE, 0); mid.set(Calendar.SECOND, 0); mid.set(Calendar.MILLISECOND, 0);
        long base = mid.getTimeInMillis();
        long[] out = new long[6];
        for (int i = 0; i < 6; i++) {
            out[i] = Double.isNaN(h[i]) ? 0 : base + (long) (Math.ceil(h[i] * 60.0 - 1e-6) * 60000.0);
        }
        return out;
    }
}
