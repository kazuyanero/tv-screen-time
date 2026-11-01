# Changelog

## 1.0.0 (2026-11-01)

First release.

- Daily limit for apps (30 minutes by default, 5–480), counted only while the screen is on.
- Free apps that never count or lock: home screen, Live TV/HDMI, input switcher (`config.xml`).
- Full-screen PIN lock in front of counted apps once the time is used; Back does nothing, Home stays free.
- Parent PIN unlock with +15 min, +30 min, +1 hour or the rest of today.
- Warnings at 5 minutes and 1 minute left.
- Parent settings behind the PIN: daily limit, today's use, reset, change PIN; PIN creation on first open.
- ADB-only setup receiver (PIN, limit, reset) and setup scripts for Windows, macOS and Linux.
- Restarts after a reboot and keeps today's count.
- English and Indonesian interface.
- Tested on a TCL Google TV, Android 12.
