# Android 3.3.2 verification — 3 November 2026

Signed debug APK: **3.3.2/code13**, `com.example.waktusolatmalaysia`.

SHA-256: `161cc38f7eab757a0a454867af59e07dbaa33aafa0f3ce0fd665b44f99ba3cd2`.

## Completed checks

| Check | Evidence/result |
|---|---|
| Debug, instrumentation and optimized unsigned release builds | Passed |
| JVM tests | 41 passed |
| Lint | 0 errors, 606 warnings; existing and dense-layout/font-resource warnings remain |
| Xiaomi/POCO 24069PC21G, Android16/API36, 1220×2712 at 480dpi | 25 native tests passed in 150.292s |
| Android7/API24 compatibility emulator, 1080×1920 at 420dpi | 3 preview/allocation tests passed; system UI healthy |
| Installed phone APK | Exact hash match with the debug artifact above |
| Real launcher | Direct home-screen capture shows the compact curved row, five complete names/times, icons, next-prayer footer and mosque |
| My widgets | Direct app-page capture shows the same compact design at the reported allocation |
| Native host versus preview | Pixel-identical drawing before preview scaling at 277.57007×137.90343 and 600×180dp |
| Phone settings | Font scale, rotation and all three animation settings unchanged; global stay-awake remains off |
| Earlier artifacts/PWA/reference | Packager checks all 30 existing APK/ZIP hashes, all 53 PWA baseline files and 10 inspected read-only reference files |

Both counted runtime runs used the exact production APK hash above. The API24 run used `swiftshader_indirect`, retaining the existing compatibility workaround. The phone suite covers the existing Classic/Timeline layout and tracking regressions, small allocations, 100/130/200% text, language/hour formats, rollover/missing tomorrow, configuration drafts, navigation, filters, system bars and launcher branding. Three new tests check paired host sizes, native drawing equality and placed-instance previews. Fixture dates/times stay inside tests and never enter production timetable caches.

## Direct observations

The phone's launcher reports width277.57007dp and height137.90343dp for Today's prayers; integer legacy bounds are 277×137. Both current placed instances use Timeline/Automatic. Direct launcher and My widgets captures show the corrected curve rather than the earlier plain band. The delivery includes a crop containing only our live widget, an unmodified own-app screen capture and native host fixture captures. The overview arranges captures; it does not redraw the widget. OEM padding/corner handling can still differ from the app preview's surrounding container.

An initial phone run locked during testing and is excluded. The successful run used a test-only listener that sets KEEP_SCREEN_ON on our test activities and restores their original window flags afterward. Global timeout/security settings were not changed. Xiaomi blocks ADB touch injection and secure-setting writes; no workaround disabled device security. USB installs of the app and separate test APK succeeded on this phone. Earlier tablet installation was rejected by its USB installer and is not counted as tablet runtime evidence.

## Data and placement preservation

No uninstall, clear-data, historical generator rerun or launcher reset was used. Fresh pre-upgrade snapshots and post-test reads confirm the five existing meaningful personal-library settings unchanged, with no new personal keys. The general preference file was absent/empty before and after. There were no stored bookmarks/tracker records/personal reminder definitions on this phone to validate as populated migration examples. Personal-task and pre-import backup files remain absent.

All 30 cached November WLY01 timetable rows and schedule metadata remain unchanged. A normal download refreshed only the upstream `serverTime`; cache byte hashes therefore differ and are not described as byte-identical. The private original snapshot remains under local `app/build/verification/v332-phone/` and is excluded from delivery/source archives.

The user confirmed manually removing and re-adding widgets while comparing designs. The initial observed ID46 and fresh pre-install IDs47/48 are consequently not retained placements. Current IDs69/70 are new instances, and their saved configurations match the currently placed widgets without test-ID leftovers. ID70 followed the launcher pin prompt during the placement check. Do not misreport this as proof that the same physical widget IDs survived the upgrade. Automated configuration/migration regression checks passed, including Classic/Timeline defaults and retained fields. Temporary test widget-binding permission was revoked; app/device settings were retained.

## Limits and reproduction

Direct launcher evidence covers this Xiaomi phone and today's five-prayer widget at its current allocation, not every OEM/grid or every family physically resized. API30/31 and the other named emulator profiles remain available but were not rerun for this patch. Spoken TalkBack traversal, prolonged reboot/Doze behavior and subjective motion remain manual checks. There is no new physical Kiblat accuracy claim. The unsigned release builds successfully; runtime checks use the signed debug artifact.

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease
python tools/android_emulator_matrix.py run --profiles legacy_api24 --suite preview
# Explicit USB serial; requires a private before-upgrade baseline:
python tools/test_phone_v332.py DEVICE_SERIAL
# Only when manual replacements were actually confirmed:
python tools/test_phone_v332.py DEVICE_SERIAL --verify-only --user-replaced-widgets
python tools/package_v332.py
```

Logs and private snapshots are local under `app/build/verification/v332-phone/`; build/API24 logs are under `app/build/verification/v332/` and `emulator-matrix/`. Public review captures, source checkpoint, APKs and checksums are in `deliverables/`. Earlier v3.3.0/v3.3.1 installers and source checkpoints remain intact.
