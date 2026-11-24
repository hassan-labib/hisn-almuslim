package com.hassan.hisn;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compass pointing to the Kaaba. Bearing is computed offline from the saved location. */
public class QiblaActivity extends Activity implements SensorEventListener {
    static final double KAABA_LAT = 21.4225, KAABA_LNG = 39.8262;
    float heading = 0;
    double bearing;
    double decl = 0;
    boolean hasSensor;
    CompassView view;
    TextView info;
    SensorManager sm;

    static double bearingTo(double lat, double lng) {
        double p1 = Math.toRadians(lat), p2 = Math.toRadians(KAABA_LAT), dl = Math.toRadians(KAABA_LNG - lng);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        double[] ll = Store.latLng(this);
        bearing = bearingTo(ll[0], ll[1]);
        decl = new GeomagneticField((float) ll[0], (float) ll[1], 0, System.currentTimeMillis()).getDeclination();
        LinearLayout[] root = new LinearLayout[1];
        LinearLayout body = Ui.screen(this, L.s("اتجاه القبلة"), root);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        info = Ui.bold(this, "", 18, Ui.GREEN);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        body.addView(info);
        view = new CompassView(this);
        body.addView(view, new LinearLayout.LayoutParams(Ui.dp(this, 300), Ui.dp(this, 300)));
        String note = (Store.hasSavedLocation(this) ? L.s("حسب موقعك الحالي.") : L.s("الموقع الحالي غير محدد، الحساب من القاهرة. فعّل إذن الموقع من الصلاة > تحديث موقعي."))
                + L.s("\nأبعد الهاتف عن المعادن والمغناطيس، وحرّكه على شكل ٨ لمعايرة البوصلة. اجعل الهاتف أفقيًا.");
        TextView n = Ui.text(this, note, 13, Ui.MUTED);
        n.setGravity(Gravity.CENTER);
        body.addView(n);
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        hasSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
        update();
        setContentView(root[0]);
    }

    @Override protected void onResume() {
        super.onResume();
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    }

    @Override protected void onPause() { sm.unregisterListener(this); super.onPause(); }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float[] r = new float[9], o = new float[3];
        SensorManager.getRotationMatrixFromVector(r, e.values);
        SensorManager.getOrientation(r, o);
        float az = (float) ((Math.toDegrees(o[0]) + decl + 360) % 360);
        // low-pass on a circle
        float d = ((az - heading + 540) % 360) - 180;
        heading = (heading + d * 0.2f + 360) % 360;
        update();
    }

    @Override public void onAccuracyChanged(Sensor s, int a) { }

    void update() {
        double rel = (bearing - heading + 360) % 360;
        boolean aligned = hasSensor && (rel < 4 || rel > 356);
        info.setText(L.s("القبلة ") + Ui.ar(Math.round(bearing)) + L.s("° من الشمال") + (hasSensor ? (aligned ? L.s("  ✓ أنت في اتجاه القبلة") : "") : L.s("\n(لا يوجد حساس بوصلة في هذا الهاتف)")));
        view.set(heading, (float) bearing, aligned);
    }

    static class CompassView extends View {
        float heading, bearing;
        boolean aligned;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        CompassView(Context c) { super(c); }
        void set(float h, float b, boolean a) { heading = h; bearing = b; aligned = a; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - 8;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(6);
            p.setColor(aligned ? Ui.GOLD : Ui.GREEN);
            c.drawCircle(cx, cy, r, p);
            // dial rotates so that north points to true north
            c.save();
            c.rotate(-heading, cx, cy);
            p.setStrokeWidth(3);
            p.setColor(Ui.MUTED);
            for (int i = 0; i < 360; i += 30) {
                c.save(); c.rotate(i, cx, cy);
                c.drawLine(cx, cy - r + 6, cx, cy - r + (i % 90 == 0 ? 30 : 16), p);
                c.restore();
            }
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(r * 0.18f);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.parseColor("#B23B3B"));
            c.drawText("N", cx, cy - r + 62, p);
            // qibla arrow on the dial
            c.rotate(bearing, cx, cy);
            Path arrow = new Path();
            arrow.moveTo(cx, cy - r * 0.75f);
            arrow.lineTo(cx - r * 0.12f, cy);
            arrow.lineTo(cx + r * 0.12f, cy);
            arrow.close();
            p.setColor(aligned ? Ui.GOLD : Ui.GREEN);
            c.drawPath(arrow, p);
            p.setColor(Ui.GREEN_DARK);
            c.drawCircle(cx, cy - r * 0.82f, r * 0.07f, p);
            c.restore();
            p.setColor(Ui.INK);
            c.drawCircle(cx, cy, r * 0.05f, p);
        }
    }
}
