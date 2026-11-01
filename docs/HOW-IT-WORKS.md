# How Screen Time works

This is for anyone who wants to change the app or port it to another TV. The user guide is the
[README](../README.md).

## The pieces

```
BootReceiver ─┐
AdminReceiver ├─> TimeService (foreground) ──tick──> UsageStatsManager: which app is in front?
SettingsActivity┘        │                                   │
                         │ counted app, time left  ──> Store.addUsed(seconds)
                         │ counted app, no time    ──> startActivity(LockActivity)
                         │ free app / screen off   ──> nothing
                         v
                   LockActivity ──right PIN──> Store.setExtraUntil(...) and finish()
```

All state is in one private `SharedPreferences` file (`Store`): the PIN hash, the daily limit, today's date and
seconds used, which warning has been shown, and the time until which a parent's extra time runs.

## Counting

`TimeService` is a foreground service of type `specialUse`. Each tick it:

1. Returns early (30 s delay) when `PowerManager.isInteractive()` is false: a TV in standby counts nothing.
2. Reads usage events since the previous query and keeps the package of the last `ACTIVITY_RESUMED` event. The first
   query looks back 6 hours; later ones only from the previous query, so a tick costs little.
3. If that package is not in `free_apps`, no extra time is running and a PIN exists:
   - with time left, adds the whole seconds since the previous tick (capped at 15 s, so a long gap such as a sleep
     never counts as use) and shows the 5- and 1-minute warnings;
   - with no time left, opens the lock.
4. Schedules the next tick: 5 s while counting, 1 s once the limit is reached (so a newly opened app is covered
   quickly), 30 s with the screen off.

`Store.usedSeconds()` compares the stored date with today, so the first read after midnight starts a new day.

## Why the lock is an activity, not an overlay

The first version drew the lock as a `TYPE_APPLICATION_OVERLAY` window, which stays above everything, Home included.
On the TCL Google TV it was tested on, such a window is drawn but does **not** keep input focus: Android kept focus on
the app underneath, so remote key presses went to YouTube behind the lock and the PIN pad missed keys.

An activity always gets focus, and the app behind it is paused by Android, which also stops its sound. Apps that
hold `SYSTEM_ALERT_WINDOW` are exempt from Android 10's background-activity-start limits, so the service can still
open it from the background. `LockActivity` is declared with:

- `singleTask` and its own `taskAffinity`: one lock, in its own task, so finishing it returns to the app that was in
  front;
- `excludeFromRecents` and `noHistory`: pressing Home drops it, and it never shows in Recents.

Home leaving the lock is intended: Home is free time. When a counted app comes to the front again, the 1-second tick
opens the lock again. `onResume` closes the lock if time was granted meanwhile (from settings or ADB).

## PIN entry

`PinPad` is a vertical `LinearLayout`: title, four slots, an error line and a 3×4 keypad of focusable `TextView`s
that the D-pad moves between. Number keys on the remote (`KEYCODE_0`–`9` and the numpad) are routed to it by the
hosting activity. After the fourth digit it calls back with the PIN and clears itself.

The PIN is stored as `SHA-256("screen-time:" + pin)`. With only 10,000 possible PINs that is not protection against
someone who can read the app's private files (which needs root); it only keeps the PIN out of plain sight.

## ADB setup receiver

`AdminReceiver` is exported but requires `android.permission.DUMP`. That permission is `signature|privileged|
development`, held by the system and by the ADB shell, so `adb shell am broadcast` works and no installed app can use
it. Extras: `pin` (4 digits), `limit` (minutes), `used` (minutes already used today, for testing), `reset` (boolean).
It logs the resulting state under the tag `ScreenTime` and starts the service.

## Boot

`BootReceiver` listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` and starts the service. Some vendors block
boot receivers of sideloaded apps: on TCL that is `TclAppBoot`, which lets an app start only if it holds the
`APP_AUTO_START` app-op. App-ops are written to disk lazily, so run `appops write-settings` after granting it, or a
reboot straight afterwards loses the grant.

## Porting to another TV

- Put the TV's home screen and Live TV app in `free_apps` (`config.xml`). The package names are in the README.
- If the lock does not open from the background, check that `SYSTEM_ALERT_WINDOW` is granted
  (`adb shell appops get local.tv.screentime SYSTEM_ALERT_WINDOW`).
- If counting stops after a reboot, look in `adb logcat` around boot for the vendor's auto-start manager and the
  app-op or whitelist it wants.

## Resource use

One process of about 15–25 MB, one handler tick every 1–30 s, one usage-events query per tick and a preferences
write while counting. No network, no wake locks, no animations.
