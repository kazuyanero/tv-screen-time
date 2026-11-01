package local.tv.screentime;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

/**
 * Counts time spent in apps (not the free ones in config.xml) while the screen is on, and once today's limit is
 * used opens LockActivity in front of any counted app. Home, Live TV and HDMI stay free. Polls which app is in
 * front: every 5 s while counting, every second once the limit is reached, every 30 s with the screen off.
 */
public class TimeService extends Service {
    private static final String TAG = "ScreenTime";
    private static final String CHANNEL = "screen_time";
    private static final int NOTICE_ID = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private PowerManager power;
    private UsageStatsManager usage;
    private Set<String> free;
    private long lastTick;
    private long lastQuery;
    private String front;
    private long lastNoticeMinute = -1;
    private long lastLockStart;

    static void start(Context c) {
        c.startForegroundService(new Intent(c, TimeService.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        power = getSystemService(PowerManager.class);
        usage = getSystemService(UsageStatsManager.class);
        free = Store.freeApps(this);
        NotificationChannel ch = new NotificationChannel(CHANNEL, getString(R.string.channel), NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = notice();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTICE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTICE_ID, n);
        }
        handler.removeCallbacks(tick);
        lastTick = System.currentTimeMillis();
        handler.post(tick);
        return START_STICKY;
    }

    private Notification notice() {
        int used = (int) (Store.usedSeconds(this) / 60);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notice, used, Store.limitMinutes(this)))
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true)
                .build();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean on = power.isInteractive();
        String app = on ? frontApp(now) : null;
        boolean counted = app != null && !free.contains(app);
        boolean extra = now < Store.extraUntil(this);
        long limit = Store.limitMinutes(this) * 60L;
        long used = Store.usedSeconds(this);

        if (on && counted && !extra && Store.hasPin(this)) {
            if (used >= limit) {
                lock();
            } else {
                long seconds = Math.min(now - lastTick, 15000) / 1000;
                if (seconds > 0) Store.addUsed(this, seconds);
                warn(limit - used - seconds);
            }
        }
        // Advance only by whole seconds counted, so no time is lost between ticks.
        lastTick = now - (now - lastTick) % 1000;

        long minute = Store.usedSeconds(this) / 60;
        if (minute != lastNoticeMinute) {
            lastNoticeMinute = minute;
            // Android 13+ drops updates without POST_NOTIFICATIONS; the service keeps running either way.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                getSystemService(NotificationManager.class).notify(NOTICE_ID, notice());
            }
        }
        long delay = !on ? 30000 : (!extra && used >= limit) ? 1000 : 5000;
        handler.postDelayed(tick, delay);
    }

    private void lock() {
        // The lock takes a moment to come to the front: do not stack starts while it does.
        long t = SystemClock.elapsedRealtime();
        if (t - lastLockStart < 3000) return;
        lastLockStart = t;
        if (!Settings.canDrawOverlays(this)) Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted: the lock may not open from the background");
        startActivity(new Intent(this, LockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    private void warn(long secondsLeft) {
        int warned = Store.warned(this);
        if (secondsLeft <= 60 && warned != 1) {
            Store.setWarned(this, 1);
            Toast.makeText(this, getResources().getQuantityString(R.plurals.warn, 1, 1), Toast.LENGTH_LONG).show();
        } else if (secondsLeft <= 300 && secondsLeft > 60 && warned == 0) {
            Store.setWarned(this, 5);
            Toast.makeText(this, getResources().getQuantityString(R.plurals.warn, 5, 5), Toast.LENGTH_LONG).show();
        }
    }

    /** The package of the last activity that came to the front, from usage events (incremental after the first scan). */
    private String frontApp(long now) {
        long from = lastQuery == 0 ? now - 6 * 3600_000L : lastQuery - 2000;
        try {
            UsageEvents events = usage.queryEvents(from, now);
            UsageEvents.Event e = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                if (e.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) front = e.getPackageName();
            }
            lastQuery = now;
        } catch (RuntimeException ex) {
            Log.w(TAG, "usage query failed: " + ex);
        }
        return front;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
