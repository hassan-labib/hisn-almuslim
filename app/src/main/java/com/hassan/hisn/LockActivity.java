package com.hassan.hisn;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/* loaded from: classes.dex */
public class LockActivity extends Activity {
    static final int REQ_LOC = 42;
    static final int REQ_PHOTO = 41;
    static final double TRAVEL_MPS = 5.0d;
    Handler h = new Handler(Looper.getMainLooper());
    LocationListener listener;
    Button skip;
    Button snap;
    TextView status;
    TextView iqamaView;

    @Override // android.app.Activity
    public void onBackPressed() {
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(128);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().setStatusBarColor(Ui.GREEN);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setBackgroundColor(Ui.GREEN);
        linearLayout.setLayoutDirection(1);
        int dp = Ui.dp(this, 24);
        linearLayout.setPadding(dp, dp, dp, dp);
        TextView text = Ui.text(this, L.s("حان وقت صلاة ") + Store.p(this).getString("lock_name", ""), 30, -1);
        text.setGravity(17);
        text.setTypeface(null, 1);
        TextView text2 = Ui.text(this, L.s("﴿ إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا ﴾\n\nقم صلِّ، ثم صوّر سجادة الصلاة لتفتح الهاتف."), 20, Color.parseColor("#F4E9C8"));
        text2.setGravity(17);
        TextView text3 = Ui.text(this, "", 17, -1);
        this.status = text3;
        text3.setGravity(17);
        this.snap = Ui.button(this, L.s("📷 صوّر سجادة الصلاة"), Color.parseColor("#F4E9C8"), Ui.GREEN);
        this.skip = Ui.button(this, L.s("أنا مسافر الآن (تخطي بتحديد المواقع)"), Color.parseColor("#0A5A41"), -1);
        Button button = Ui.button(this, L.s("📞 مكالمة طوارئ"), Color.parseColor("#0A5A41"), -1);
        linearLayout.addView(text);
        linearLayout.addView(text2);
        TextView iq = Ui.text(this, "", 20, Color.parseColor("#F4E9C8"));
        iq.setGravity(17);
        this.iqamaView = iq;
        linearLayout.addView(iq);
        linearLayout.addView(this.status);
        tickIqama();
        linearLayout.addView(this.snap);
        linearLayout.addView(this.skip);
        linearLayout.addView(button);
        setContentView(linearLayout);
        this.snap.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LockActivity.this.lambda$onCreate$0(view);
            }
        });
        this.skip.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LockActivity.this.lambda$onCreate$1(view);
            }
        });
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LockActivity.this.lambda$onCreate$2(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View view) {
        takePhoto();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(View view) {
        checkTravel();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(View view) {
        try {
            startActivity(new Intent("android.intent.action.DIAL").addFlags(268435456));
        } catch (Exception unused) {
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (LockController.isActive(this)) {
            return;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        this.h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    void tickIqama() {
        long at = Store.p(this).getLong("iqama_at", 0L);
        long now = System.currentTimeMillis();
        if (at > now && at - now < 3600000L) {
            long sec = (at - now) / 1000;
            this.iqamaView.setText(Ui.ar(String.format(java.util.Locale.US, L.s("الإقامة بعد %02d:%02d"), sec / 60, sec % 60)));
        } else {
            this.iqamaView.setText("");
        }
        this.h.postDelayed(new Runnable() { public void run() { tickIqama(); } }, 1000L);
    }

    void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra("output", PhotoProvider.URI);
        intent.addFlags(3);
        intent.setClipData(ClipData.newRawUri("", PhotoProvider.URI));
        PhotoProvider.file(this).delete();
        Store.p(this).edit().putLong("capture_until", System.currentTimeMillis() + 120000).apply();
        try {
            startActivityForResult(intent, REQ_PHOTO);
        } catch (Exception unused) {
            this.status.setText(L.s("ما لقيتش تطبيق كاميرا على الهاتف"));
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        Store.p(this).edit().putLong("capture_until", 0L).apply();
        if (i != REQ_PHOTO) {
            return;
        }
        if (i2 != -1) {
            this.status.setText(L.s("لم يتم التصوير. حاول مرة أخرى."));
            return;
        }
        this.status.setText(L.s("جاري فحص الصورة..."));
        this.snap.setEnabled(false);
        new Thread(new Runnable() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LockActivity.this.lambda$onActivityResult$4();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onActivityResult$4() {
        final MatClassifier.Result check = MatClassifier.check(this, PhotoProvider.file(this).getAbsolutePath());
        this.h.post(new Runnable() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                LockActivity.this.lambda$onActivityResult$3(check);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onActivityResult$3(MatClassifier.Result result) {
        this.snap.setEnabled(true);
        if (!result.ok) {
            this.status.setText(L.s("لم أتعرف على سجادة صلاة في الصورة (النتيجة ") + result.score + L.s("). صوّر السجادة أوضح وكاملة وقريبة."));
            return;
        }
        LockController.release(this, "mat");
        Toast.makeText(this, L.s("تقبل الله 🤍"), 1).show();
        finish();
    }

    void checkTravel() {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQ_LOC);
            return;
        }
        this.status.setText(L.s("بقيس حركتك بتحديد المواقع... (حتى ٢٠ ثانية)"));
        this.skip.setEnabled(false);
        final LocationManager locationManager = (LocationManager) getSystemService("location");
        final Location[] locationArr = {null};
        final boolean[] zArr = {false};
        LocationListener locationListener = new LocationListener() { // from class: com.hassan.hisn.LockActivity.1
            @Override // android.location.LocationListener
            public void onProviderDisabled(String str) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String str) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }

            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                if (zArr[0]) {
                    return;
                }
                double speed = location.hasSpeed() ? location.getSpeed() : -1.0d;
                if (locationArr[0] != null) {
                    double time = (location.getTime() - locationArr[0].getTime()) / 1000.0d;
                    if (time >= 3.0d) {
                        speed = Math.max(speed, location.distanceTo(locationArr[0]) / time);
                    }
                }
                Location[] locationArr2 = locationArr;
                if (locationArr2[0] == null) {
                    locationArr2[0] = location;
                }
                if (speed >= LockActivity.TRAVEL_MPS) {
                    LockActivity.this.finishTravel(locationManager, true, speed);
                }
            }
        };
        this.listener = locationListener;
        try {
            locationManager.requestLocationUpdates("gps", 1000L, 0.0f, locationListener);
            this.h.postDelayed(new Runnable() { // from class: com.hassan.hisn.LockActivity$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    LockActivity.this.lambda$checkTravel$5(locationManager);
                }
            }, 20000L);
        } catch (Exception unused) {
            this.status.setText(L.s("تحديد المواقع مش شغال. شغّل الموقع وجرب تاني."));
            this.skip.setEnabled(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$checkTravel$5(LocationManager locationManager) {
        finishTravel(locationManager, false, 0.0d);
    }

    void finishTravel(LocationManager locationManager, boolean z, double d) {
        LocationListener locationListener = this.listener;
        if (locationListener == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Exception unused) {
        }
        this.listener = null;
        this.skip.setEnabled(true);
        if (z) {
            LockController.release(this, "travel");
            Toast.makeText(this, L.s("الله يسلمك في سفرك 🤍"), 1).show();
            finish();
            return;
        }
        this.status.setText(L.s("تحديد المواقع مش بيقول إنك بتتحرك. لازم تصلي وتصور السجادة."));
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == REQ_LOC && iArr.length > 0 && iArr[0] == 0) {
            checkTravel();
        } else if (i == REQ_LOC) {
            this.status.setText(L.s("محتاج إذن الموقع عشان أتأكد إنك مسافر."));
        }
    }
}
