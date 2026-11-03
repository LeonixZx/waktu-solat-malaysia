# Android 3.2.0 — widget verification

Verified on 2–3 November 2026, Asia/Kuala_Lumpur. Android package `com.example.waktusolatmalaysia`, version **3.2.0 / code 8**. The final debug APK is installed on the Xiaomi tablet as an in-place update. The PWA remains 3.1.0; all 53 recorded web source/public/Worker/configuration files match their starting SHA-256 hashes. Existing 3.1.1 artifacts remain intact. The decoded-APK reference workspace was not modified.

## Automated results

| Check | Result and scope |
|---|---|
| Native build | Debug APK, Android test APK, optimized unsigned release APK, lint and **41 JVM tests pass** |
| Lint | **0 errors, 178 warnings**; warnings remain, including existing deprecations/resource issues and intentional version-specific widget attributes and synchronous configuration commits |
| Widget environments | **9 passing instrumentation tests per environment** on API24, API30, API31, API34 emulators and the physical Xiaomi API36 tablet |
| Measured layout matrix | **576 combinations per environment**: 100/130/200% text × Malay/English × 12/24h × Ivory/Noir × four families × six bounds, from 120×72 to 480×700 logical units |
| Text contrast | Calculated native text contrast ranges from 5.00:1 to 15.93:1; Ivory selected-time ink is darkened separately on its tonal highlight |
| Fit assertions | Fully measured native text widths/heights, child containment, retained timetable purpose at sufficient space, small-space resize affordances and native 48dp controls |
| Tracking | Legacy Malay storage keys, canonical IDs, mark/unmark, duplicate intended-state actions, real immutable pending-intent delivery, stale-date rejection and disabled tracking |
| Configuration | Draft isolation, recreation, Cancel, instance-specific Save, independent zone and unchanged app default |
| Date/zone opening | Actual rendered surface opens its displayed 1 January 2027 schedule and independent zone while app default remains unchanged |
| Schedule model | Malaysia timezone, final prayer, missing today/tomorrow and December→January rollover; no guessed tomorrow Subuh |
| Deletion race | Deleted instance rejects queued rendering and later configuration writes; no test-only orphan configuration remains on the tablet |
| Final-build upgrade | Final APK upgraded three bound components from delivered 3.1.1: IDs/components, independent zones, themes, preferences and records retained; tracking disabled and metadata defaults migrated correctly |
| Existing tablet regression | **31 tests pass** across native navigation, schedules, readers, Ibadah, calendar, branding and sensor-event delivery |
| Current personal data | All 7 original app-preference keys and 15 original personal-library keys retained exactly, excluding volatile scheduler bookkeeping; reminder definitions and recovery backup retained. No new personal-record keys remain |
| Tablet settings | Original font scale, auto-rotation and rotation settings match. Precise location remains denied. Temporary app widget-host binding grant revoked |
| Exact installed artifact | Pulled installed tablet APK SHA-256 matches the delivered debug build |

The five-environment widget checks are separate from the 31-test physical regression. A matrix case is a measured layout assertion, not a screenshot of every combination. Date/rollover tests use labelled deterministic in-memory fixtures; installed previews and launcher widgets use actual cached JAKIM schedules. No fabricated schedule was written to production caches.

## Physical launcher observations

All four entries appeared in the Xiaomi widget picker, separately from the original publisher’s entries. They were placed through the launcher’s manual Widgets menu. Four launcher-owned instances remain: **15, 16, 17 and 18**. The pre-existing Google search widget and original app were untouched.

The launcher reported a 4×2 initial grid span for the strip and Daily Plus despite the requested 4×1/4×4 targets. These target spans are launcher-dependent. Rendering used the actual supplied dimensions. Daily Plus displayed its essential five times and settings action; auxiliary times collapsed when the supplied height was insufficient. Tracking remains off on all four placed instances until enabled by the user.

Direct physical AppWidgetHost screenshots show all four families in both Ivory and Noir. Separate screenshots record actual Xiaomi picker placement and configuration. The screenshot gallery labels app-host evidence versus launcher evidence; a host capture is not described as an actual launcher placement.

## Unresolved or manual checks

- **Direct Xiaomi resize-handle manipulation was not verified.** Multiple actual bounds and shape changes pass in native widget hosts, but the attempted launcher long-press sequence did not produce a confirmed drag-handle resize. The placement guide remains available. Launcher minimum grid spans may prevent a literal 4×1 strip.
- **Spoken TalkBack review and physical keyboard navigation remain manual checks.** Native descriptions, full date/zone summaries, next-prayer state, recorded/unrecorded action labels, focusable native buttons and text/control fit were checked programmatically. A complete spoken traversal was not certified.
- **Reboot survival is inconclusive.** The additional API34 soft-reboot run stalled. Recovering the emulator required a process restart; the reboot checkpoint became empty and the test package needed reinstalling. The after-reboot assertion could not be completed, so no reboot pass is claimed. The emulator boots/responds again. Upgrade survival and deletion/refresh lifecycle tests pass independently. The physical tablet was not rebooted, and a physical launcher restart was not performed.
- WorkManager/launcher delivery under prolonged real battery restrictions was not timed exhaustively. Offline rendering, cache-only receivers, unique per-zone work, boundaries/midnight and retained periodic fallback are implemented; the OS can delay updates.
- The unsigned release APK builds successfully. Runtime checks used the debug build; signing and release-store distribution remain release tasks.

This release changes widgets and their shared tracking/cache integration. It does not certify physical Kiblat accuracy; the earlier [3.1.1 verification](VERIFICATION_V311.md) remains the record for that separate feature.

## Evidence and reproduction

Evidence is in `app/build/verification/v32/`: `native-build.log`, `test-build.log`, `emulator-{24,30,31,34}-widgets.log`, `tablet-widgets.log`, `tablet-regression.log`, `upgrade-before.log`, `upgrade-after.log`, `reboot-before.log`, `reboot-after.log`, `reboot-recovery.json`, `data-retention.json`, `tablet-settings-restored.json`, `pwa-unchanged.json`, `tablet-final-state.json`, `widget-contrast.json` and direct screenshots. Personal snapshots remain local and are excluded from the delivery ZIP.

- Build with JDK21: `gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease`.
- `tools/verify_widgets_v32.py <adb-serial> <evidence-label>` runs the opt-in native widget host suite; use a test environment and restore its temporary binding grant afterward.
- `WidgetUpgradeV32Test` runs before under 3.1.1 and after under 3.2.0. It intentionally uses persistent host IDs and preserves the other preferences.
- `tools/finalize_widgets_v32.py` verifies the current tablet’s data/settings, exact installed APK, four bound widget configurations and unchanged PWA; it revokes only the temporary test-host grant.
- `DESIGN_WIDGETS_V32.md` contains the visual and implementation specification. No image-generation assets or new runtime permissions were introduced.
