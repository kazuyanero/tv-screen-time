package local.tv.screentime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts counting at boot and after an update. On TCL it also needs the APP_AUTO_START app-op (README). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        TimeService.start(context);
    }
}
