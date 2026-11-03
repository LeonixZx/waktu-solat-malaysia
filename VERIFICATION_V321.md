# Android 3.2.1 — widget refinement verification

Verified on 3 November 2026, Asia/Kuala_Lumpur. Package `com.example.waktusolatmalaysia`, version **3.2.1 / code 9**. The final debug APK is installed on the Xiaomi tablet as an in-place update. The PWA remains 3.1.0; all 53 recorded web files are unchanged. Earlier delivered APKs and ZIPs are retained and checked against their recorded hashes during packaging.

## Results

| Check | Result |
|---|---|
| Build | Debug, test and optimized unsigned release APKs build successfully with JDK 21, minSdk 24, compile/target 34 and Build Tools 34.0.0 |
| JVM / lint | **41 JVM tests pass; 0 lint errors, 206 warnings**. Existing warnings and intentional version-specific widget resource attributes remain |
| Widget instrumentation | **12 tests pass on each** of API 24 emulator, API 34 emulator and physical Xiaomi API 36 tablet |
| Layout matrix | **576 measured combinations per environment**: 100/130/200% text, Malay/English, 12/24h, Ivory/Noir, four families, six bounds |
| Compact strip | Additional 280/300/320 × 130 logical-unit checks in both languages and time formats: all five names/times fit; native AM/PM labels appear correctly |
| Picker previews | All four providers expose real preview images; obsolete empty runtime previews are absent. Xiaomi visibly displays the four new previews |
| Content-sized surfaces | Native host assertions check wrapped surface height, child containment and retained widget purpose. Actual launcher captures show the resulting compact cards |
| Tracking / configuration | Existing nine-test widget suite passes: canonical legacy keys, intended-state actions, duplicate delivery, stale date rejection, disabled tracking, instance isolation, Cancel/recreation and independent zone navigation |
| Schedule boundaries | Existing fixture checks pass for missing today/tomorrow, final prayer and year rollover; unavailable tomorrow data never reuses today's Subuh |
| Data retention | All original **7 app preference keys, 15 personal-library keys and 4 widget configurations** match exactly. Reminder definitions and recovery backup are unchanged; no additional personal keys remain |
| Installed artifact | Pulled installed APK SHA-256 equals the final debug APK |
| Device restoration | Original font scale and rotation settings restored; precise location remains denied; temporary test-host binding grant revoked; no orphan widget configurations |
| Text contrast | Worst calculated native-text contrast is **4.72:1** across the two gradients; primary and selected time colours also exceed 4.5:1 |

A matrix case is a measured native layout assertion, not a screenshot of every combination. Eight separate physical AppWidgetHost captures show all four families in both appearances. Additional direct launcher screenshots show the user's four existing instances with actual cached schedules. Fixture previews are explicitly labelled examples and are separate from launcher/live data.

The final 12-test suite includes nine existing functional widget tests and three new appearance tests. API 30 and 31 passed the prior 3.2.0 suite; they were **not rerun for 3.2.1**. The earlier 31-test tablet regression and 3.1.1-to-3.2.0 migration results remain historical evidence, not new passes for this build.

## Actual launcher observations

Existing launcher instances **15, 16, 17 and 18** remain bound to the same four provider components, with their saved zones and appearances intact. No widget was deleted or recreated for the upgrade. Tracking remains disabled on the user's instances until explicitly enabled.

Xiaomi's picker now shows a **2×1 strip**, alongside the next-prayer card, daily band and Daily Plus. Requested grid spans remain launcher-dependent. The installed strip uses five compact time cells and a next-prayer header cue. The daily band adds prayer icons; Daily Plus shows the five main and three auxiliary times in the currently supplied wide allocation. The small next-prayer allocation retains its clear large absolute time; larger square host allocations additionally show supporting times.

The visible background follows content height inside the allocated launcher space. Extra height remains transparent rather than producing a large empty coloured panel. Direct launcher captures are in `screenshots/launcher-final-page-*.png`; the individual `launcher-card-*.png` files are crops of those captures. Host screenshots are labelled separately in `screenshots/host/`.

The Tonton app's public widget previews and provider metadata were inspected read-only to understand density and proportions. Its assets are **not included in the app or delivery bundle**. Our vectors, backgrounds, layouts and native-rendered previews are original. The original app, launcher data and `C:\Tools\waktu-re\` were not modified.

## Remaining manual checks

- Actual Xiaomi drag-handle resizing, spoken TalkBack traversal and physical keyboard navigation are not certified. Native accessibility descriptions, 48dp actions and measured text fit pass programmatic checks.
- No reboot or launcher restart was performed for this update. The inconclusive 3.2.0 reboot test remains unresolved; this report does not convert it into a pass.
- Long-duration battery restrictions and delayed background delivery were not exhaustively timed. Existing periodic, boundary and per-zone refresh logic is retained.
- Runtime tests used the debug APK. The release APK is unsigned and requires production signing before distribution.

No Kiblat or PWA behavior changed in this release. Their earlier verification limitations remain separate from these widget results.

## Evidence and reproduction

Local evidence is under `app/build/verification/v321/`: `native-build.log`, `tablet-widgets.log`, `emulator-widgets.log`, `api24-widgets.log`, `data-retention.json`, `tablet-settings-restored.json`, `tablet-final-state.json`, `pwa-unchanged.json`, `contrast.json` and direct screenshots. Personal snapshots and the read-only reference APK remain local and are excluded from the delivery bundle.

1. Build with JDK 21: `gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease`.
2. Run `python tools/verify_widgets_v321.py <adb-serial> <evidence-label>` on a test environment. This executes `WidgetV32Test` and `WidgetAppearanceV321Test`; restore any temporary test-host binding grant afterward.
3. `python tools/finalize_widgets_v321.py` validates the current tablet artifact, original data/settings, four existing configurations and unchanged PWA, then revokes the temporary grant.
4. `python tools/package_v321.py` validates build/test evidence, preserves old artifact hashes and creates the APKs, gallery, checksums and delivery ZIP.

See `WIDGETS_V321.md` for the visual specification. No image-generation artwork, new runtime permission or dependency was introduced.
