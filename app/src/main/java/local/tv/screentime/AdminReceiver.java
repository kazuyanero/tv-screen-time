package local.tv.screentime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Setup and testing from ADB (the manifest limits it to holders of DUMP: the system and the shell).
 * Extras: pin (4 digits), limit (minutes), used (minutes already used today), reset (true clears today).
 */
public class AdminReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String pin = i.getStringExtra("pin");
        if (pin != null && pin.matches("\\d{4}")) Store.setPin(c, pin);
        if (i.hasExtra("limit")) Store.setLimitMinutes(c, i.getIntExtra("limit", 30));
        if (i.getBooleanExtra("reset", false)) Store.resetToday(c);
        if (i.hasExtra("used")) {
            Store.resetToday(c);
            Store.addUsed(c, i.getIntExtra("used", 0) * 60L);
        }
        Log.i("ScreenTime", "admin: pin set=" + Store.hasPin(c) + " limit=" + Store.limitMinutes(c)
                + " used=" + Store.usedSeconds(c) / 60 + "min");
        TimeService.start(c);
    }
}
