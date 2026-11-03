# README screenshot correction — 3 November 2026

## What went wrong

The first README's worship and dark-dashboard images contained **“System UI isn't responding”**.
Those were stale screenshots in an older flat capture folder. Later ADB pulls had placed newer clean
images in a nested folder, leaving the older names in place. The publication selected the old files
and incorrectly associated them with subsequent passing test results. That selection and review failed.

The dialog identifies Android's `com.android.systemui` process as unresponsive; it does not identify
`com.example.waktusolatmalaysia` as the ANR process. An archived emulator health dump also records a
System UI ANR. This is evidence of a System UI failure in that emulator session, **not proof of its
underlying cause or a guarantee that the app can never ANR**. The retained old screenshots/logs do not
establish the historical root cause. See [Android's ANR diagnosis guidance](https://developer.android.com/topic/performance/issues/anr).

## Correction

- Re-captured the preserved v3.3.4/code 15 debug APK; runtime code and installer are unchanged.
- Replaced all three README screenshots with versioned `*-v2.webp` files from one fresh API34 run.
  The original text, system bars and screen contents remain visible; no error dialog was erased or painted over.
- Inspected all three full-resolution captures before encoding them. Public sample zone/preferences
  are explicitly selected for capture, then restored by test cleanup. No physical device was touched.
- Added foreground and Android ANR/crash-dialog checks before and after each Phone UI screenshot.
- Added real navigation taps in both themes and isolated host/emulator capture directories by run ID.
- Retained original failed evidence privately. File hashes and the capture run are recorded in
  [the asset record](../design/assets/readme-art-v1.json).

## Fresh verification

Android 14 / API34, standard-phone profile, automatic GPU renderer:

- Five native smoke tests passed, including both-theme screens, actual navigation taps, filter
  retention, settings recreation, compact widgets and system-bar flags.
- App foreground and no ANR/crash-dialog checks passed around every exported Phone UI screenshot.
- Emulator System UI was healthy at the before/after checks.
- Test APK SHA-256: `e0cd2066430aa844760c24a1a223a0d6063d9e0ba71968be747b8a7614087f17`.
- Preserved app APK SHA-256: `31742a4184f3caa44c67673ac2474f31e58799410c26d20655d20a0f03f60b16`.
- Capture run: `20261103T120521429875Z-smoke`.

Android 16 / API36, modern-phone profile, host GPU renderer:

- The same five native tests passed against the same app/test APK hashes.
- Capture-time foreground/dialog checks and before/after System UI health passed.
- The freshly cleared Android events buffer recorded **zero app/System UI ANR or crash events**
  during this run. Raw logs remain private test infrastructure.
- Capture run: `20261103T120730793185Z-smoke`.

These are bounded emulator checks, not a certification of every device or a retrospective diagnosis
of the old System UI ANR. The app prayer clock uses Malaysia time; the emulator system bar can use a
different system timezone. Fresh screenshots are screen captures, not generated UI mockups.
