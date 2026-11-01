package local.tv.screentime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A 4-digit PIN entry for the remote: four dots and a 3x4 keypad the D-pad moves over, plus the remote's own number
 * keys (feed them to {@link #digit}). Calls back with the four digits once the last one is in.
 */
@SuppressLint("ViewConstructor") // built in code only, never inflated from XML
final class PinPad extends LinearLayout {
    interface Listener {
        void onPin(String pin);
    }

    private final TextView title;
    private final TextView dots;
    private final TextView message;
    private final StringBuilder entered = new StringBuilder();
    private final Listener listener;
    private TextView first;

    PinPad(Context c, CharSequence heading, Listener listener) {
        super(c);
        this.listener = listener;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        title = Ui.text(c, heading, 18, 0xCCFFFFFF);
        addView(title);
        dots = Ui.text(c, "", 30, Color.WHITE);
        dots.setLetterSpacing(0.3f);
        addView(dots);
        message = Ui.text(c, " ", 14, 0xFFFF6B6B);
        addView(message);
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {c.getString(R.string.delete), "0", ""}};
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(c);
            r.setOrientation(HORIZONTAL);
            r.setGravity(Gravity.CENTER);
            for (String k : row) {
                if (k.isEmpty()) {
                    TextView gap = Ui.text(c, "", 20, 0);
                    gap.setLayoutParams(new LayoutParams(Ui.dp(c, 86), Ui.dp(c, 10)));
                    r.addView(gap);
                    continue;
                }
                TextView b = Ui.button(c, k, 76, () -> press(k));
                if (first == null) first = b;
                r.addView(b);
            }
            addView(r);
        }
        showDots();
    }

    void focusFirst() {
        if (first != null) first.requestFocus();
    }

    void setTitle(CharSequence s) {
        title.setText(s);
    }

    void setMessage(CharSequence s) {
        message.setText(s == null || s.length() == 0 ? " " : s);
    }

    void clear() {
        entered.setLength(0);
        showDots();
    }

    void digit(int d) {
        press(String.valueOf(d));
    }

    private void press(String k) {
        if (k.equals(getContext().getString(R.string.delete))) {
            if (entered.length() > 0) entered.setLength(entered.length() - 1);
        } else if (entered.length() < 4) {
            entered.append(k);
            setMessage(null);
        }
        showDots();
        if (entered.length() == 4) {
            String pin = entered.toString();
            entered.setLength(0);
            post(() -> {
                showDots();
                listener.onPin(pin);
            });
        }
    }

    private void showDots() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 4; i++) s.append(i < entered.length() ? '●' : '–');
        dots.setText(s);
    }
}
