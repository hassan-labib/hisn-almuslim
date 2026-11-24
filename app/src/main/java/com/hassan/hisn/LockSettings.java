package com.hassan.hisn;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/* loaded from: classes.dex */
public class LockSettings {
    static boolean usageGranted(Context context) {
        return ((AppOpsManager) context.getSystemService("appops")).unsafeCheckOpNoThrow("android:get_usage_stats", Process.myUid(), context.getPackageName()) == 0;
    }

    public static void add(final Activity activity, LinearLayout linearLayout, final SharedPreferences sharedPreferences) {
        TextView text = Ui.text(activity, L.s("قفل الصلاة"), 22, Ui.GREEN);
        text.setPadding(0, Ui.dp(activity, 20), 0, Ui.dp(activity, 6));
        linearLayout.addView(text);
        linearLayout.addView(Ui.text(activity, L.s("وقت الأذان تتقفل كل التطبيقات لحد ما تصور سجادة الصلاة، أو تضغط \"أنا مسافر\" وتحديد المواقع يأكد إنك بتتحرك. الحد الأقصى للقفل ٦٠ دقيقة كأمان."), 15, Ui.INK));
        Switch r0 = new Switch(activity);
        r0.setText(L.s("تفعيل قفل الصلاة"));
        r0.setTextSize(18.0f);
        r0.setTextColor(Ui.INK);
        r0.setChecked(sharedPreferences.getBoolean("lock_on", false));
        r0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                LockSettings.lambda$add$0(activity, sharedPreferences, compoundButton, z);
            }
        });
        linearLayout.addView(r0);
        final Button perm = perm(activity, linearLayout);
        final Button perm2 = perm(activity, linearLayout);
        final Button perm3 = perm(activity, linearLayout);
        perm.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                activity.startActivity(new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + activity.getPackageName())));
            }
        });
        perm2.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                activity.startActivity(new Intent("android.settings.USAGE_ACCESS_SETTINGS", Uri.parse("package:" + activity.getPackageName())));
            }
        });
        perm3.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LockSettings.lambda$add$3(activity, view);
            }
        });
        Button button = Ui.button(activity, L.s("تجربة القفل الآن (٥ دقائق)"), Ui.GREEN, -1);
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LockSettings.lambda$add$4(activity, view);
            }
        });
        linearLayout.addView(button);
        Runnable runnable = new Runnable() { // from class: com.hassan.hisn.LockSettings$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                LockSettings.lambda$add$5(activity, perm, perm2, perm3);
            }
        };
        linearLayout.setTag(runnable);
        runnable.run();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$add$0(Activity activity, SharedPreferences sharedPreferences, CompoundButton compoundButton, boolean z) {
        if (LockController.isActive(activity)) {
            compoundButton.setChecked(true);
        } else {
            sharedPreferences.edit().putBoolean("lock_on", z).apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$add$3(Activity activity, View view) {
        try {
            activity.startActivity(new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", Uri.parse("package:" + activity.getPackageName())));
        } catch (Exception unused) {
            activity.startActivity(new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$add$4(Activity activity, View view) {
        if (!Settings.canDrawOverlays(activity) || !usageGranted(activity)) {
            Toast.makeText(activity, L.s("فعّل إذن الظهور فوق التطبيقات وإذن الوصول للاستخدام أولًا"), 1).show();
        } else {
            LockController.begin(activity, L.s("تجربة"), LockController.TEST_LOCK_MS);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$add$5(Activity activity, Button button, Button button2, Button button3) {
        boolean canDrawOverlays = Settings.canDrawOverlays(activity);
        boolean usageGranted = usageGranted(activity);
        boolean isIgnoringBatteryOptimizations = ((PowerManager) activity.getSystemService("power")).isIgnoringBatteryOptimizations(activity.getPackageName());
        button.setText((canDrawOverlays ? "✓ " : "✗ ").concat(L.s("الظهور فوق التطبيقات")));
        button2.setText((usageGranted ? "✓ " : "✗ ").concat(L.s("الوصول لاستخدام التطبيقات")));
        button3.setText((isIgnoringBatteryOptimizations ? "✓ " : "✗ ").concat(L.s("استثناء من توفير البطارية")));
    }

    static Button perm(Context context, LinearLayout linearLayout) {
        Button button = Ui.button(context, "", Color.parseColor("#E5F2EC"), Ui.GREEN);
        linearLayout.addView(button);
        return button;
    }

    public static void refresh(LinearLayout linearLayout) {
        Object tag = linearLayout.getTag();
        if (tag instanceof Runnable) {
            ((Runnable) tag).run();
        }
    }
}
