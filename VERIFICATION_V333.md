# Android 3.3.3 — date and location verification

3 November 2026. Signed debug package `com.example.waktusolatmalaysia`, **3.3.3/code14**. JDK21, minSdk24, compile/targetSdk34 and Build Tools34.0.0 are unchanged.

Final debug SHA-256: `e3999eba299256d8c1bd6ff12913a35c7d83e6c6e5079f52a9f29b3a18d601c8`.

## Completed checks

| Check | Result |
|---|---|
| Debug, instrumentation and optimized unsigned release builds | Passed |
| JVM tests | 46 passed |
| Lint | 0 errors, 606 existing/resource-layout warnings |
| Connected Xiaomi/POCO 24069PC21G, Android16/API36, 1220×2712 at 480dpi | 29 native regression tests passed |
| Android7/API24 compatibility emulator, 1080×1920 at 420dpi | 4 date/location tests passed; system UI healthy |
| Actual compact allocation | 277.57007×137.90343dp; five-prayer curve retained with two date lines |
| Preview exports | All four shipping picker images below 250KB, from our native renderer |
| Earlier artifacts | All 34 prior APK/archive hashes verified before packaging |
| PWA and read-only reference | All 53 PWA baseline files and 10 inspected reference hashes unchanged |

Both counted runtime runs use the exact final APK hash above. The API24 run retains its established `swiftshader_indirect` compatibility renderer. Superseded numeric-date and pre-location builds are excluded from these release results. The native suite includes existing Classic/Timeline tracking, configuration, scaling, navigation, rollover and preview regressions plus four date/location methods. New unit tests cover supplied-date formatting, invalid dates, Hijri year boundaries, readable primary localities and compact place names.

One compatibility-emulator attempt timed out during boot before tests began; a clean restart without wiping data passed. The emulator runner now continues its owned-process cleanup even when an unresponsive ADB cleanup command times out. One phone run was interrupted by another app opening and became stuck inside Android's `Instrumentation.startActivitySync`. A debugger thread trace isolated the platform/test-launch wait; the interrupted test process was stopped and the run repeated with the phone idle. These attempts are excluded from passing totals. No device security settings were changed.

## Date and location behavior

Today's prayers (Timeline) shows **`3 Nov 2026`** and **`23 Jamadil Awal 1448`** on separate lines even at the measured compact phone size. Tests include the longer Malay/English sixth-month names, both appearances and compact/spacious bounds. Daily Plus shows a named Hijri month in both styles, respecting the saved Hijri toggle. No numeric Hijri month fallback remains. Gregorian and Hijri dates use the same official JAKIM daily row in Malaysia time; tests cover after Isyak, midnight and unavailable/invalid supplied Hijri data. No Islamic date is estimated or converted.

All four widget families and both styles use location names at preferred and compact bounds. Tests assert no visible `WLY01` text. Headers, footers and My widgets summaries use human-readable names; official zone codes remain internal and available in the zone picker. A shorter locality label can represent the first area of a multi-district zone; full area names remain in accessibility descriptions. Native compact location text is 9sp in Timeline and scales with Android accessibility settings.

Review captures distinguish native fixtures, the installed instance preview and direct launcher evidence. Static picker illustrations are labelled fixtures, not live dates, and are never written into production caches. Source captures and hashes are in `design/widgets-v333/`.

## Upgrade and limits

The phone was upgraded normally without uninstalling, clearing app data, resetting the launcher or recreating widgets. Fresh snapshots confirm original instances69/70 retain their saved configurations; current configuration keys match live instances without test leftovers. The five existing meaningful personal-library preferences remain unchanged. The general preference file remains absent/empty. This phone has no populated bookmarks, tracker records or personal reminder definitions, so it is not a populated migration fixture for those features.

All original rows and schedule metadata in the three cached November prayer months remain unchanged; normal downloads may refresh only upstream `serverTime`. Missing personal-task and pre-import backup files remain absent. Private snapshots stay local under `app/build/verification/v333-phone/` and are excluded from archives. Font scale, rotation, animation settings and widget-binding permission retain their original state. Test window keep-awake flags are restored; no global timeout/security setting is changed.

Other OEM grids, minimum allocations combined with very large accessibility text, spoken TalkBack traversal and prolonged reboot/Doze behavior retain their documented manual limits. Measured candidates prioritize prayer information when optional metadata cannot fit. This change makes no new physical Kiblat accuracy claim. Runtime checks use the signed debug APK; the optimized release APK is unsigned.

Reproduce with the Gradle wrapper, `testDebugUnitTest`, `lintDebug` and `assembleRelease`; use `tools/android_emulator_matrix.py run --profiles legacy_api24 --suite dates` and the explicit-serial `tools/test_phone_v333.py`. The packager validates current APK/test results, preservation evidence and previous hashes before publishing new files. See `deliverables/v3.3.3/` for the gallery and manifest, and `deliverables/Waktu-Solat-Malaysia-v3.3.3-SHA256SUMS.txt` for artifact hashes.
