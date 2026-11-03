# Android emulator test workflow

Dedicated Waktu Solat Malaysia AVDs are configured in `testing/android-emulators.json`. The emulator runner only targets `emulator-*` serials and verifies their AVD identity before installing or testing. It does not target the USB tablet, clear app data, wipe AVD data, force-overwrite existing AVDs or modify another project's emulator.

| Profile | AVD | Android/API | Native display | Available width |
|---|---|---|---|---|
| Small phone | `WSM_Phone_Small_API34` | Android14 / 34 | 720×1600, 360dpi | 320dp |
| Standard phone | `WSM_Phone_Standard_API34` | Android14 / 34 | 1080×2400, 420dpi | about411dp |
| Large tablet | `WSM_Tablet_Large_API34` | Android14 / 34 | 2560×1600, 320dpi, landscape | 1280dp |
| Modern phone | `WSM_Phone_Modern_API36` | Android16 / 36 | 1080×2400, 420dpi | about411dp |

Existing dedicated compatibility AVDs `WSM_Widget_API24`, `WSM_Widget_API30` and `WSM_Widget_API31` remain available for minimum-version and widget-platform checks. `scanflow_test` is not used by this new runner.

They are selectable as `legacy_api24`, `legacy_api30` and `legacy_api31`. Their existing configurations are checked and retained, rather than rewritten or project-marked by this new workflow.

On this Windows host, the API24 profile selects the `swiftshader_indirect` compatibility renderer. The automatic renderer caused an Android system display-process crash; the same tests passed with the compatibility renderer. This is a launch argument, not an edit to that legacy AVD's configuration. `--gpu` can explicitly override a profile's renderer when diagnosing the host environment.

The four new profiles use Google APIs x86_64 images, 4 CPU cores, 4GB RAM, automatic GPU selection and a 4GB data partition each. They share installed SDK system images. The SDK and JDK paths match this workspace's Android setup. Android16's system image is added for testing; the app's compile/target SDK34 and Build Tools34.0.0 remain unchanged.

## Commands

Build the current app and test APKs first:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
.\gradlew.bat assembleDebug assembleDebugAndroidTest
python tools\android_emulator_matrix.py create
python tools\android_emulator_matrix.py run --profiles small_phone standard_phone large_tablet modern_phone
```

The default five-method smoke suite runs the four `PhoneUiV330Test` methods plus `SystemBarAppearanceTest`: native choices across widths/text scales, both-theme screen captures and filter retention, the floating settings panel with theme recreation/System return, compact widget allocations in both languages/time formats, and focused light/dark system-icon flags. Navigation-icon appearance is asserted only on API26+, where that platform feature exists. The suite uses real native views and installed APKs, not browser mockups.

For the 29-method UI/widget suite (the previous 18 plus four Timeline, three preview and four date/location checks):

```powershell
python tools\android_emulator_matrix.py run --profiles small_phone large_tablet modern_phone --suite full
python tools\android_emulator_matrix.py run --profiles legacy_api24 legacy_api30 legacy_api31 --suite full
```

For only the four Timeline migration/layout/interaction checks:

```powershell
python tools\android_emulator_matrix.py run --profiles standard_phone legacy_api24 --suite timeline
```

Timeline checks cover Classic defaults for existing instances, Timeline defaults for new widgets, all four families in both appearances/languages/hour formats, preferred and compact dimensions, text scaling, missing-tomorrow/year rollover, native-host tracking taps and the layout configuration's Cancel/Save behavior. Test-created widget IDs and preference changes are cleaned up afterward.

For the three v3.3.2 preview/allocation checks:

```powershell
python tools\android_emulator_matrix.py run --profiles standard_phone legacy_api24 --suite preview
```

These verify paired host dimensions, pixel-identical native-host/preview drawing before display scaling, and the actual placed-instance bounds in My widgets/configuration. The fixture includes the measured Xiaomi phone allocation of 277.57007×137.90343dp. Physical-phone testing is separate: `tools/test_phone_v332.py` requires an explicit USB serial and an existing private baseline; it never changes global timeout/security settings. Its test listener keeps only our own test activities awake and restores their window flags. Record manual widget removal/readdition separately rather than claiming the original IDs survived.

For v3.3.3's four date/location tests, use `--suite dates`. They check full named Hijri months at compact/spacious bounds, both styles of Daily Plus, the saved Hijri toggle, Malaysia midnight, after-Isyak dates, unavailable/invalid Hijri data and location names across all four families/styles. `tools/test_phone_v333.py <USB-serial>` records a fresh private baseline on its first run, upgrades without clearing data, runs the full 29 tests and compares original widget-instance configurations and personal settings afterward.

Profiles run sequentially by default to bound memory usage. Two separate commands may run in parallel using different even emulator ports, for example `--port 5560` and `--port 5562`. The runner refuses a port already listed by ADB. Processes start hidden and close in a finally block; temporary widget-binding permission is revoked. New AVDs are project-marked to prevent accidental takeover of an existing unmarked device.

Run sequentially for release evidence. The runner checks for Android ANR/crash dialogs before and after testing, so passing native-view assertions cannot hide a broken emulator System UI. During startup only, it may select **Wait** on a confirmed System UI ANR dialog; an unhealthy post-test state fails the run. The `bars` suite adds a focused light/dark system-icon check and requires the activity to own focus.

System animations are disabled during automated runs to avoid transient captures and are restored in the cleanup block. These runs do not certify animation smoothness; review motion separately with animations enabled.

The runner writes logs, crash buffers, direct screenshots, reported API/display metrics and the exact tested APK SHA-256 under `app/build/verification/emulator-matrix/<profile>/`. Its result starts as pending and becomes completed/failed; `suite`, `tests`, `nativeAssertionsPassed`, `systemUiHealthy` and `passed` distinguish coverage and health. Cleanup waits for the owned ADB serial to disappear before starting the next profile. Re-running retains emulator data rather than pretending every run is a fresh install. Use an intentionally separate profile when a clean-install scenario is required.

For manual interactive testing, select one of these named AVDs in Android Studio's Device Manager and launch it there. Stop it before using the automated runner with the same profile. The automated runner deliberately runs without a visible emulator window.

## Release testing policy

- Phone UI changes: small/standard phones, 100/130/200% text, light/dark/System, portrait/landscape and keyboard/inset behavior. `tools/phone_matrix_v330.py` records the existing adjustable-window/text-scale checks; its serial is explicit and emulator-only in practice.
- Adaptive navigation/readers: large tablet plus resized/foldable-style available-window widths; inspect list/detail transitions, settings bounds and sidebar/rail/bottom navigation. A resized viewport is not described as a real foldable hinge test.
- Widget changes: API24, API30, API31 and a modern API, using the full native host suite. Add launcher placement, resize, rotation and lifecycle checks separately.
- Feature changes: add relevant reader, backup, offline, reminder/alarm and download regression classes rather than assuming a UI smoke pass covers them. Update the configuration's class lists/count expectations when suites change.
- Keep the USB tablet as a complementary physical check. Stock Android emulators do not reproduce Samsung/Xiaomi/Oppo skins, real magnetic interference, physical Qibla accuracy or every vendor's background restriction. Label physical observations and emulator results separately.

Use the installed Android SDK/AVD tooling; no system Gradle is needed. Device definitions follow [Android's AVD guidance](https://developer.android.com/studio/run/managing-avds), and lifecycle/ADB usage follows [Android's command-line emulator documentation](https://developer.android.com/studio/run/emulator-commandline). Do not modify `C:\Tools\waktu-re\` as part of testing.
