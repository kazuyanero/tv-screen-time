package local.tv.screentime;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** Parent settings behind the PIN: daily limit, today's use, extra time, reset, change PIN. Creates the PIN first time. */
public class SettingsActivity extends Activity {
    private FrameLayout root;
    private PinPad pad;
    private String firstEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TimeService.start(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B14);
        setContentView(root);
        if (Store.hasPin(this)) askPin();
        else createPin();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int d = Ui.digit(keyCode);
        if (d >= 0 && pad != null) {
            pad.digit(d);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void show(android.view.View v) {
        root.removeAllViews();
        root.addView(v, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private void askPin() {
        pad = new PinPad(this, getString(R.string.enter_pin), pin -> {
            if (Store.checkPin(this, pin)) showSettings();
            else pad.setMessage(getString(R.string.wrong_pin));
        });
        show(pad);
        pad.focusFirst();
    }

    private void createPin() {
        firstEntry = null;
        pad = new PinPad(this, getString(R.string.create_pin), pin -> {
            if (firstEntry == null) {
                firstEntry = pin;
                pad.setTitle(getString(R.string.confirm_pin));
            } else if (firstEntry.equals(pin)) {
                Store.setPin(this, pin);
                showSettings();
            } else {
                firstEntry = null;
                pad.setTitle(getString(R.string.create_pin));
                pad.setMessage(getString(R.string.pin_mismatch));
            }
        });
        show(pad);
        pad.focusFirst();
    }

    private void showSettings() {
        pad = null;
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(Ui.text(this, getString(R.string.settings_title), 34, 0xFFFFFFFF));

        TextView limit = Ui.button(this, getString(R.string.limit_row, Store.limitMinutes(this)), 420, () -> { });
        limit.setOnKeyListener((v, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            int step = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 15 : keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -15 : 0;
            if (step == 0) return false;
            Store.setLimitMinutes(this, Store.limitMinutes(this) + step);
            limit.setText(getString(R.string.limit_row, Store.limitMinutes(this)));
            return true;
        });
        col.addView(limit);

        TextView used = Ui.text(this, getString(R.string.used_row, Store.usedSeconds(this) / 60), 18, 0xCCFFFFFF);
        used.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        col.addView(used);
        long extra = Store.extraUntil(this);
        if (extra > System.currentTimeMillis()) {
            col.addView(Ui.text(this, getString(R.string.extra_row,
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(extra))), 18, Ui.ACCENT));
        }
        col.addView(Ui.button(this, getString(R.string.reset_today), 420, () -> {
            Store.resetToday(this);
            showSettings();
        }));
        col.addView(Ui.button(this, getString(R.string.change_pin), 420, this::createPin));
        TextView hint = Ui.text(this, getString(R.string.settings_hint), 14, 0x80FFFFFF);
        hint.setMaxWidth(Ui.dp(this, 520));
        hint.setPadding(0, Ui.dp(this, 16), 0, 0);
        col.addView(hint);
        show(col);
        limit.requestFocus();
    }
}
