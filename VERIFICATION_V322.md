# Android 3.2.2 — artwork and launcher verification

Verified on **3 November 2026**, Asia/Kuala_Lumpur. Package `com.example.waktusolatmalaysia`, version **3.2.2 / code 10**. The final debug APK is installed on the connected Xiaomi tablet. The PWA remains 3.1.0; its 53 recorded files are unchanged.

## Automated results

| Check | Result |
|---|---|
| Build | Debug APK, Android test APK and optimized unsigned release APK build successfully with JDK21, minSdk24, compile/target34 and Build Tools34.0.0 |
| JVM / lint | **41 JVM tests pass; 0 lint errors, 212 warnings**. Existing warnings and version-specific resources remain |
| Instrumentation | **13 passing tests each** on API24 emulator, API34 emulator and the physical Xiaomi API36 tablet |
| Responsive matrix | **576 measured layouts per environment** across four families, six bounds, 100/130/200% text, Malay/English and both hour formats/appearances |
| Compact strip | Twelve additional 280/300/320×130 cases per environment retain all five native times and correct AM/PM labels |
| Artwork integration | Native previews verify the 10% decorative opacity, accessibility exclusion, no focus/click action and backdrop containment. Content-sized surfaces and layout-purpose assertions pass |
| Launcher identity | Gold/espresso normal icon renders in light and dark configurations on all three APIs. Gold geometry remains clear of mask edges; actual Xiaomi app-menu and home-screen icons were also inspected |
| Tracking / settings | Existing widget tests pass for intended-state actions, duplicate delivery, midnight stale-date rejection, legacy storage keys, independent zones, draft cancellation/recreation and deletion races |
| Schedule boundaries | Existing missing-today/tomorrow, final-prayer and year-rollover fixtures pass; no fabricated future Subuh |
| Contrast | Conservative compositing of every backdrop pixel over the least favourable gradient endpoint gives **4.97:1 or better** for normal text roles. Opaque selected-cell time colours retain the previous passing contrast |
| Data retention | Exact match for all **7 app preference keys, 15 personal-library keys and 4 widget configurations**. Reminder definitions and recovery backup match; no extra personal keys remain |
| Device state | Original font scale/rotation settings restored; precise location still denied; temporary host-binding permission revoked; four original launcher widget IDs retained without orphan configurations |
| Artifact identity | Installed tablet APK SHA-256 equals the delivered final debug APK |

Instrumentation durations: API24 **128.723s**, API34 **395.6s**, physical tablet **190.319s**. These are whole-suite durations, not application startup or scrolling benchmarks. The emulators and tablet ran independently in parallel. The temporary API24 emulator was closed after its completed run; the pre-existing API34 emulator remains available.

A matrix case is a measured native layout assertion, not a screenshot of every combination. Eight physical native-host screenshots cover both appearances. Four separate actual Xiaomi launcher captures show the existing widget instances using cached real schedules. Fixture picker masters remain labelled `Contoh`; no fixture schedule was written into production caches.

## Direct launcher observations

All four existing instances **15, 16, 17 and 18** survive the normal package upgrade with their exact configurations. Mosque artwork is visible behind the time text, respects rounded card margins, and does not add a tap target or expand the surface. Tracking remains off on the user's instances unless enabled by the user.

The green home-screen shortcut was traced to **our app**: its App info screen showed version 3.2.2 after the upgrade. The actual app-menu and dock icons had already changed to gold/espresso, while this older home shortcut retained its stored artwork. Only that shortcut was removed through the launcher's Remove action and replaced at the same grid position from the current app-menu entry. The green shortcut now visibly uses the new gold mark. No app was uninstalled and no application or launcher data was cleared.

The original publisher's app, its existing shortcut and other launcher items were retained. Its public resources were not used to create the new imagery. `C:\Tools\waktu-re\` was not modified or used for asset generation. Original generated images, their alpha channels and exact prompts are retained in `design/brand-v322/`.

## Remaining manual checks

- Actual Xiaomi drag-handle widget resizing, spoken TalkBack traversal and physical keyboard navigation are not certified in this update. Native layout, descriptions and 48dp control checks pass.
- No reboot, launcher restart or prolonged battery-restriction test was performed; prior unresolved lifecycle checks remain unresolved.
- Actual wallpaper-tinted monochrome launcher mode was not toggled. The updated monochrome resource is present; Android controls themed-icon recolouring.
- API30 and API31 were not rerun for this patch. Earlier 3.2.0 results and broader app regressions remain historical evidence, not new passes.
- Runtime checks use the debug APK. The optimized release APK is unsigned and needs production signing.

No Kiblat or PWA behavior changed. This update does not extend their earlier verification claims.

## Files and reproduction

`app/build/verification/v322/` contains `native-build.log`, `tablet-widgets.log`, `emulator-widgets.log`, `api24-widgets.log`, `contrast.json`, `data-retention.json`, `tablet-settings-restored.json`, `tablet-final-state.json`, `pwa-unchanged.json`, native previews and launcher evidence. Personal snapshots stay local and are excluded from the delivery ZIP.

- Build: `gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease` with JDK21.
- Test: `python tools/verify_widgets_v322.py <adb-serial> <evidence-label>`; the runner revokes its temporary binding grant in a finally block.
- Validate retained state: `python tools/finalize_widgets_v322.py`.
- Export generated-source derivatives: `python tools/export_brand_v322.py`; native layouts: `python tools/widget_resources_v322.py`. Native preview masters are captured by `WidgetAppearanceV321Test` and packaged with `tools/widget_previews_v322.py`.
- Package: `python tools/package_v322.py`, which validates build/tests, installed APK identity and old delivery hashes before creating the artifacts.

See `DESIGN_V322.md` and `design/brand-v322/prompts.json` for design, source paths, exact prompts and **built-in imagegen** mode. No API-key CLI fallback was used.
