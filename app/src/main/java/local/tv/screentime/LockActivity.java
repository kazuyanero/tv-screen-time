package local.tv.screentime;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The lock: a full-screen activity in front of the counted app (an activity always gets the remote's keys, which an
 * overlay window does not on this firmware). Back does nothing; Home leaves it, since Home is free. The right PIN
 * offers extra time and closes it, returning to the app underneath. TimeService opens it again whenever a counted app
 * comes to the front with no time left.
 */
public class LockActivity extends Activity {
    private LinearLayout col;
    private PinPad pad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B14);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.icon);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56));
        iconLp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(icon, iconLp);
        col.addView(Ui.text(this, getString(R.string.locked_title), 30, 0xFFFFFFFF));
        TextView body = Ui.text(this, getString(R.string.locked_body, Store.limitMinutes(this)), 15, 0x99FFFFFF);
        body.setPadding(Ui.dp(this, 120), Ui.dp(this, 4), Ui.dp(this, 120), Ui.dp(this, 8));
        col.addView(body);
        pad = new PinPad(this, getString(R.string.enter_pin), pin -> {
            if (Store.checkPin(this, pin)) showChoices();
            else pad.setMessage(getString(R.string.wrong_pin));
        });
        col.addView(pad, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(col, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
        pad.focusFirst();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Time granted meanwhile (settings, ADB): nothing to lock.
        if (System.currentTimeMillis() < Store.extraUntil(this)
                || Store.usedSeconds(this) < Store.limitMinutes(this) * 60L) finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int d = Ui.digit(keyCode);
        if (d >= 0) {
            if (pad != null) pad.digit(d);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // The lock stays: only the PIN or Home (which is free) leaves it.
    }

    private void showChoices() {
        col.removeView(pad);
        pad = null;
        col.addView(Ui.text(this, getString(R.string.how_long), 20, 0xCCFFFFFF));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        View first = Ui.button(this, getString(R.string.plus_15), 0, () -> grant(15));
        row.addView(first);
        row.addView(Ui.button(this, getString(R.string.plus_30), 0, () -> grant(30)));
        row.addView(Ui.button(this, getString(R.string.plus_60), 0, () -> grant(60)));
        row.addView(Ui.button(this, getString(R.string.rest_of_day), 0, () -> grant(-1)));
        col.addView(row);
        first.requestFocus();
    }

    private void grant(int minutes) {
        long now = System.currentTimeMillis();
        Store.setExtraUntil(this, minutes < 0 ? Store.endOfToday() : Math.min(now + minutes * 60_000L, Store.endOfToday()));
        finish();
    }
}
