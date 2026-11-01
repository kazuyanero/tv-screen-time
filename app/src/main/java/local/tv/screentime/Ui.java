package local.tv.screentime;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small view helpers shared by the lock and the settings screen: dark glass buttons that turn white on focus. */
final class Ui {
    static final int ACCENT = 0xFF5AA9FF;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, CharSequence s, float sp, int colour) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(colour);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    static TextView button(Context c, CharSequence label, float widthDp, Runnable onClick) {
        TextView b = text(c, label, 18, 0);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setClickable(true);
        b.setTextColor(new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {Color.BLACK, Color.WHITE}));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, round(c, Color.WHITE));
        bg.addState(new int[] {}, round(c, 0x26FFFFFF));
        b.setBackground(bg);
        b.setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6));
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                widthDp > 0 ? dp(c, widthDp) : LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(c, 5), dp(c, 4), dp(c, 5), dp(c, 4));
        b.setLayoutParams(lp);
        return b;
    }

    private static GradientDrawable round(Context c, int colour) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 14));
        d.setColor(colour);
        return d;
    }

    /** 0-9 for a number key (main row or keypad), -1 otherwise. */
    static int digit(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) return keyCode - KeyEvent.KEYCODE_0;
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) return keyCode - KeyEvent.KEYCODE_NUMPAD_0;
        return -1;
    }
}
