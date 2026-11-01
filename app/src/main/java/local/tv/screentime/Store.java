package local.tv.screentime;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Settings and today's count, in private preferences. The PIN is kept only as a SHA-256 hash. */
final class Store {
    private Store() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("screen_time", Context.MODE_PRIVATE);
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    static long endOfToday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static String hash(String pin) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(("screen-time:" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : d) s.append(String.format(Locale.ROOT, "%02x", b));
            return s.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean hasPin(Context c) {
        return !p(c).getString("pin", "").isEmpty();
    }

    static boolean checkPin(Context c, String pin) {
        return hasPin(c) && hash(pin).equals(p(c).getString("pin", ""));
    }

    static void setPin(Context c, String pin) {
        p(c).edit().putString("pin", hash(pin)).apply();
    }

    static int limitMinutes(Context c) {
        return p(c).getInt("limit", c.getResources().getInteger(R.integer.default_limit));
    }

    static void setLimitMinutes(Context c, int minutes) {
        p(c).edit().putInt("limit", Math.max(5, Math.min(480, minutes))).apply();
    }

    /** Seconds of app time used today; a new day starts from zero. */
    static long usedSeconds(Context c) {
        SharedPreferences s = p(c);
        if (!today().equals(s.getString("day", ""))) {
            s.edit().putString("day", today()).putLong("used", 0).putInt("warned", 0).apply();
            return 0;
        }
        return s.getLong("used", 0);
    }

    static void addUsed(Context c, long seconds) {
        long now = usedSeconds(c);
        p(c).edit().putString("day", today()).putLong("used", now + seconds).apply();
    }

    static void resetToday(Context c) {
        p(c).edit().putString("day", today()).putLong("used", 0).putInt("warned", 0).putLong("extra_until", 0).apply();
    }

    /** Wall-clock time until which a parent's extra time runs (0 = none). */
    static long extraUntil(Context c) {
        return p(c).getLong("extra_until", 0);
    }

    static void setExtraUntil(Context c, long until) {
        p(c).edit().putLong("extra_until", until).apply();
    }

    /** The warning already shown today: 0 none, 5 the five-minute one, 1 the one-minute one. */
    static int warned(Context c) {
        usedSeconds(c);
        return p(c).getInt("warned", 0);
    }

    static void setWarned(Context c, int w) {
        p(c).edit().putInt("warned", w).apply();
    }

    static Set<String> freeApps(Context c) {
        return new HashSet<>(Arrays.asList(c.getResources().getStringArray(R.array.free_apps)));
    }
}
