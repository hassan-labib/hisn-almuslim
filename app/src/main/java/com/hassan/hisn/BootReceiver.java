package com.hassan.hisn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Scheduler.scheduleAll(c, true);
        LockController.resumeIfActive(c);
    }
}
