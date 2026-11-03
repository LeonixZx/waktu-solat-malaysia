Current v3 results are in [VERIFICATION_V3.md](VERIFICATION_V3.md). The record below documents the earlier v2 build.

# Verification · Waktu Solat Malaysia 2.0

Verified 2 November 2026. Final debug APK SHA-256: `A24BE7F80E1A065AA69E4696EFAE71F1B4EE78AFD2D2FB4C6BD71DFC9071C75E`.

## Build

JDK 21 / Gradle wrapper 8.9 / AGP 8.5.2; minSdk 24, compile/target 34, Build Tools 34.0.0. Debug and optimized unsigned release builds pass. 25 JVM tests pass. Android lint passes with **0 errors**; warnings include Malay text/localization suggestions, older dependency notices and optional attributes. These warnings have not been hidden with a baseline.

## Physical tablet

Xiaomi 2405CRPFDG, serial `DEVICE_SERIAL`, Android 16 / API 36, 1600×2560 at 320 dpi. Installed package `com.example.waktusolatmalaysia`, version 2.0.0 (code 2). The reference publisher's package remains separate.

**18 distinct instrumented checks pass:**

- Six native smoke checks: drawer/menu navigation, settings/format/night-mode recreation, official 60-zone catalog and genuine JAKIM download plus disk cache.
- Four feature checks: five displayed home times and countdown match JAKIM; full timetable date picker/previous/next/today and recreation; state/zone selection persists and loads its own schedule; portrait/landscape controls remain reachable.
- Six expanded feature checks: searchable 114-surah Quran library, seven Al-Fatihah verses/Malay meaning and saved readings; all four devotional collections and bookmarks; tasbih persistence/confirmed reset; daily/weekly/monthly schedules plus calendar; next actual reminder scheduling/cancellation; azan-option persistence, both local recordings decode, and Quran audio prepares successfully **without starting playback**.
- Physical rotation-vector sensor events verified.
- Explicitly offline run: cached prayer refresh remains usable and labelled offline, cached Quran and hadith remain readable, an uncached 1901 schedule shows a failure with no invented prayer times. Wi-Fi was disabled only for this check and restored in `finally`.

Latest main run: `app/build/verification/premium/tablet-verified.log` → **OK (16 tests)**. Separate compass and offline logs each report **OK (1 test)**. After the final phone-spacing polish, the tablet drawer/screen check also passed again.

## Phone emulator

API 34, 1080×2400 at 420 dpi. **10 distinct checks pass**: the six expanded feature checks plus the four date/zone/countdown/rotation checks. The rotation test explicitly reveals controls after Android restores the scroll position; being below the fold in landscape is expected and the controls remain reachable.

Logs: `phone-verified.log` → **OK (6 tests)** and `phone-layout-retry.log` → **OK (4 tests)**. The first software-rendered emulator cold boot encountered a system ANR; a restarted emulator using host rendering completed the checks. This was separate from the switch-theme defects found and corrected in the app.

## Corrected during verification

The hero's constraint cycle was replaced with a bottom-anchored prayer panel. Programmatic switches now use the Material Components switch matching the app theme; notification/azan and settings toggles persist. Shortcut labels are centered; phone spacing brings prayer cards into the initial viewport. Arabic reading text has explicit right alignment. The first test-package install was declined by the device; it succeeded after the owner approved the retry.

## Final state and limits

The redesigned debug app remains installed and open on the tablet. Original defaults (Kuala Lumpur zone, 24-hour display, light mode, reminder/azan off) and personal test state were restored. Wi-Fi is on; system auto-rotate remains 1 and user rotation remains 0. Test recordings were prepared without playing sound or changing volume. The temporary instrumentation package is removed after verification. The headless emulator is stopped after saving previews.

Actual notification delivery across prolonged Doze, manufacturer battery restrictions, a physical reboot and background azan playback have **not** been exercised. Scheduling/cancellation, opt-in controls and audio preparation have been checked. API 24 compatibility is configured/desugared and checked by lint; no API 24 device was available for runtime testing. Release signing, store submission and third-party distribution permissions are outside this development verification.

## Preview files

- `app/build/verification/premium/tablet-home.png`
- `app/build/verification/premium/phone-home.png`
- `app/build/verification/premium/tablet-reader.png`
- `app/build/verification/premium/phone-reader.png`
- `app/build/verification/premium/tablet-calendar.png`

The screenshots are direct device/emulator captures. No screenshot content was composited or retouched. Reference directory `C:\Tools\waktu-re\` was only read. Selected authorized asset copies have matching SHA-256 records in `design/assets/reference-copies.json`.
