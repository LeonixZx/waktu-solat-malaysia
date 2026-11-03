# Android 3.3.4 — azan and notification repair

3 November 2026. Application `com.example.waktusolatmalaysia`, **3.3.4/code15**.
JDK21, minSdk24, compile/targetSdk34 and Build Tools34.0.0 remain unchanged.

Debug SHA-256: `31742a4184f3caa44c67673ac2474f31e58799410c26d20655d20a0f03f60b16`.

## Findings and changes

The connected Xiaomi/POCO 24069PC21G (API36) had notifications enabled, alarm volume4/15,
Do Not Disturb off, and **exact alarm permission denied**. The owner enabled that permission
through Android's settings during this task. It remains enabled intentionally. Modern Android
can block automatic foreground playback from an inexact background alarm; the app now explains
this and offers a user-triggered Play adhan notification action.

The previous scheduler generated a new random alarm-generation token on every refresh, including
Application startup. A process started by a due alarm could race that refresh and reject the
incoming alarm as superseded. Generation now depends on the actual zone/mode/task configuration:
routine refresh retains it; edits/deletions invalidate it. Receivers reject future and older-than-
five-minute events rather than replaying stale prayer alerts. Exact-permission revocation between
checking/scheduling falls back to approximate delivery and records scheduling failures honestly.

Adhan playback is separate from POST_NOTIFICATIONS permission, uses the alarm audio stream,
requests focus after entering a media-playback foreground service, prepares asynchronously and
holds a MediaPlayer-managed partial wake lock only while playing. Errors/focus denial/zero alarm
volume and blocked background starts are recorded as local operational status. Playback and
five-second tests stop/release resources. Device volume, DND and channel settings are not changed.

All prayer, personal-reminder and playback notifications use the original project arch/crescent/
clock as a white alpha status-bar vector, plus the colour logo as their large icon. Android chooses
the status-bar tint. Existing channel IDs/settings are retained. A schedule section shows live
permissions/channel status, exact-versus-approximate scheduling, alarm volume, DND guidance and
real notification/5-second audio test buttons. Choosing Azan explains missing exact permission.

## Verified

- Debug/test/optimized unsigned release builds, 46 JVM tests and lint pass; lint has 0 errors and
  605 existing/resource/API warnings.
- **Physical API36 phone: 2 real AlarmManager tests passed**, exercising a branded prayer
  notification, scheduled azan reaching MediaPlayer.start and the Stop notification action.
  Installed APK hash matches the debug artifact. Playback state is verified; it is not a microphone
  measurement of speaker loudness or a guarantee for every OEM battery policy.
- A further direct phone notification capture passed; its cropped notification card shows the
  gold project logo. The full private notification-shade screenshot is excluded from delivery.
- **API34 emulator: 7 native alert tests passed**, covering stable generations, actual prayer and
  personal notifications, dedup/deleted tasks, both bundled recordings, five-second completion,
  Stop, device-selected volume and user-facing test controls.
- **API34 cold-process checks: notification and azan both delivered** after confirming the app
  process was absent. The normal Application startup/reschedule path was exercised.
- **API24 emulator: 7 native tests and both cold-process deliveries passed**, with healthy
  post-test System UI and the established compatibility graphics backend.
- Fresh phone snapshots retain all 3 existing app preference keys, 11 meaningful personal keys,
  the original placed-widget configuration, and all four existing cached monthly schedules
  (allowing only upstream server timestamp refresh). An additional widget configuration appeared
  during owner interaction; this task's alert tests do not create widgets or delete placements.
- Font scale, rotation, animations, alarm volume and DND retain their original state. No uninstall,
  clear-data or launcher reset was used. Private snapshots remain local and are excluded publicly.
- All 38 earlier APK/source/delivery archives are preserved and checksummed; the PWA remains3.1.0.

Two development assertions were corrected: the emulator clamps alarm volume to1 when an app
without volume-modification privileges requests0, and foreground-notification removal is
asynchronous. Tests now check the system-selected volume and await notification removal. These
superseded attempts are excluded from the passing release evidence.

## Practical limits and reproduction

Force-stopping an app in Android settings, manufacturer battery restrictions, muted/blocked
channels, DND rules, a zero alarm volume and revoked permissions can still affect delivery.
No battery exemption or DND bypass is requested automatically. Use Schedule → notification/azan
tests and the displayed health checks. This patch does not certify prolonged Doze/reboot behavior
or change prayer calculation/religious content. There is no new physical Kiblat claim.

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease
python tools/android_emulator_matrix.py run --profiles standard_phone --suite alerts
python tools/android_emulator_matrix.py run --profiles legacy_api24 --suite alerts --port 5562
```

`PrayerAlertsV334Test` and the cold-process harness explicitly refuse non-emulator devices.
`PhysicalAlertsV334Test` requires the explicit `phoneAlerts=true` opt-in and does not grant
permissions/change device audio. Source/build instructions and GitHub recovery are in
`CLONE_AND_BUILD.md` and `GITHUB_BACKUP_VERIFICATION.md`. Downloadable debug APKs are test builds;
production signing keys remain private and release-signing is still a separate step.
