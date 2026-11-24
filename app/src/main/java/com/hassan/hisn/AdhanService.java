package com.hassan.hisn;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import org.json.JSONArray;

/** Plays the adhan, then posts the dua after the adhan and the iqama countdown. */
public class AdhanService extends Service {
    public static final int NOTIF_PLAY = 8, NOTIF_DUA = 9, NOTIF_IQAMA = 10;
    static final String ACTION_STOP = "com.hassan.hisn.STOP_ADHAN";
    MediaPlayer mp;
    int prayer = 0;
    boolean finished = false;
    final Handler h = new Handler(Looper.getMainLooper());

    /** Returns true if the service started. */
    public static boolean start(Context c, int prayer) {
        try {
            Intent i = new Intent(c, AdhanService.class).putExtra("prayer", prayer);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
            return true;
        } catch (Exception e) { return false; }
    }

    public static String voiceFile(Context c, int prayer) {
        String v = Store.p(c).getString("voice", "makkah");
        if (prayer == 0) return "adhan/adhan_fajr.ogg";
        switch (v) {
            case "madinah": return "adhan/adhan_madinah.ogg";
            case "makkah2": return "adhan/adhan_makkah2.ogg";
            default: return "adhan/adhan_makkah.ogg";
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { finishAdhan(); return START_NOT_STICKY; }
        prayer = intent == null ? 0 : intent.getIntExtra("prayer", 0);
        Scheduler.channels(this);
        Intent stop = new Intent(this, AdhanService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 5, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Scheduler.builder(this, Scheduler.CH_ADHAN).setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(L.s("أذان ") + Scheduler.PRAYER_AR[prayer]).setContentText(L.s("حان الآن موعد صلاة ") + Scheduler.PRAYER_AR[prayer])
                .setOngoing(true).addAction(new Notification.Action.Builder(null, L.s("إيقاف الأذان"), stopPi).build())
                .setContentIntent(PendingIntent.getActivity(this, 6, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_PLAY, b.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            else startForeground(NOTIF_PLAY, b.build());
        } catch (Exception e) { stopSelf(); return START_NOT_STICKY; }
        play();
        return START_NOT_STICKY;
    }

    void play() {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            AssetFileDescriptor fd = getAssets().openFd(voiceFile(this, prayer));
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { public void onCompletion(MediaPlayer m) { finishAdhan(); } });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() { public boolean onError(MediaPlayer m, int w, int e) { finishAdhan(); return true; } });
            mp.prepare();
            mp.start();
        } catch (Exception e) { finishAdhan(); }
    }

    synchronized void finishAdhan() {
        if (finished) return;
        finished = true;
        try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception ignored) { }
        mp = null;
        postAfterAdhan(this, prayer);
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        try { if (mp != null) mp.release(); } catch (Exception ignored) { }
        super.onDestroy();
    }

    /** The dua after the adhan, plus the iqama countdown when enabled. */
    public static void postAfterAdhan(Context c, int prayer) {
        Scheduler.channels(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        String dua = L.s("اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ");
        try {
            nm.notify(NOTIF_DUA, Scheduler.builder(c, Scheduler.CH_PRAYER).setSmallIcon(R.drawable.ic_stat).setContentTitle(L.s("دعاء بعد الأذان"))
                    .setContentText(dua).setStyle(new Notification.BigTextStyle().bigText(dua + L.s("\n\nثم صلِّ على النبي ﷺ، وادعُ لنفسك بين الأذان والإقامة فالدعاء لا يُرد.")))
                    .setAutoCancel(true).setContentIntent(PendingIntent.getActivity(c, 7, ChapterActivity.intent(c, 14), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)).build());
            long at = Store.p(c).getLong("iqama_at", 0);
            if (Store.p(c).getBoolean("iqama_on", true) && at > System.currentTimeMillis()) {
                Notification.Builder b = Scheduler.builder(c, Scheduler.CH_ADHAN).setSmallIcon(R.drawable.ic_stat)
                        .setContentTitle(L.s("إقامة صلاة ") + Scheduler.PRAYER_AR[prayer] + L.s(" بعد")).setContentText(L.s("ادعُ بين الأذان والإقامة"))
                        .setWhen(at).setShowWhen(true).setUsesChronometer(true).setOngoing(false).setAutoCancel(false)
                        .setTimeoutAfter(Math.max(1000, at - System.currentTimeMillis()));
                if (Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(true);
                nm.notify(NOTIF_IQAMA, b.build());
            }
        } catch (SecurityException ignored) { }
    }
}
