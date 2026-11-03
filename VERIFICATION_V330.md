# Android 3.3.0 — phone UI and widget fit verification

Verified **3 November 2026**, Asia/Kuala_Lumpur. Package `com.example.waktusolatmalaysia`, version **3.3.0 / code11**. The final debug APK is installed as an in-place update on the Xiaomi tablet. The PWA remains 3.1.0 with all 53 recorded files unchanged.

## Automated coverage

| Check | Result / scope |
|---|---|
| Build | Debug APK, test APK and optimized unsigned release APK build successfully; JDK21, minSdk24, compile/target34 and Build Tools34.0.0 |
| JVM / lint | **41 JVM tests pass; 0 lint errors, 286 warnings**. Remaining warnings include existing resource/deprecation issues and intentional version-specific widget attributes |
| Main native suites | **17 passing instrumentation methods per environment** on API24 emulator, API34 phone emulator at 320dp, and physical Xiaomi API36 tablet |
| Widget matrix | **576 measured layouts per environment**, covering 100/130/200% text, Malay/English, 12/24h, Ivory/Noir, four families and six bounds |
| Compact phone widgets | **44 additional allocation/language/time-format cases per environment**. These include next cards at 130×110 and 150×130, strips at 250×70/82 and 280×90, and five-time layouts at 250×130, 280×150 and 180×190 logical units. All retain real native prayer text rather than a resize instruction |
| Selection controls | Twelve extra collection-choice width/text-scale cases per environment: widths280/320/360/600, scales100/130/200%. Checks include selected state, measured text, containment and 48dp targets |
| Phone profiles | Three further UI methods pass in each of **360dp/130%, 360dp/200%, landscape720dp/100%**. These verify both theme appearances, equal shortcut heights, filter retention, settings recreation and native theme resources |
| Existing settings/readers | **Five regression methods pass** for preference persistence, theme recreation, settings/navigation reachability, collection search/selection restoration and all **36 supplied offline readings** |
| Theme modes | Light, Dark and return-to-System behavior checked. The final tablet check reproduces its system-dark state correctly after leaving forced-light/dark modes |
| Settings sheet | Opens over the current hub, keeps the route unchanged, survives recreation, restores theme selection, has one theme control and closes through its explicit action |
| Navigation and tiles | Bottom safe area is applied once; native nav height remains usable. Shortcut heights are equal, including the long personal-reminder label at 200% text |
| Tracking/configuration | Existing widget checks pass for legacy keys, intended-state actions, duplicate delivery, stale-date rejection, independent zones, draft cancellation/recreation, deletion races and displayed date/zone navigation |
| Schedule boundaries | Existing unavailable-today/tomorrow, final prayer and year-rollover fixtures pass; no substituted future Subuh |
| Control contrast | Ten calculated normal text/state colour pairs range from **4.78:1 to 15.56:1**, including inactive/selected controls and both navigation appearances |

A matrix case is a measured layout assertion, not a screenshot of every combination. Repeated methods in different environments are not counted as unique test implementations. Three main suites total 51 method executions; the additional phone-profile and reader/settings runs add 14.

The larger-text check initially caught unequal shortcut heights, and the physical tablet caught a System-mode mismatch not reproduced by the emulator. Both were corrected, then their affected checks and the main suites rerun. Earlier failing development logs remain local; the packaged report refers to the passing verified logs.

## Direct visual evidence

The review gallery separates:

- **API34 emulator captures** at 320dp/100%, 360dp/130%, 360dp/200% and landscape720dp/100%, in light and dark appearances.
- **Physical Xiaomi tablet captures**, including the bounded floating settings panel and existing adaptive rail/list layouts.
- **Native widget-host captures**, all four families in both appearances. These are native host evidence, not claims that another phone's launcher was physically tested.

Screens show the smaller toolbar mark, rounded floating bottom navigation with explicit active state, wrapping collection/media controls, single outlined search placeholder, equal shortcuts, dark artwork treatment and grouped settings sheet. The gallery's screen images are direct captures; the overview sheet only scales/arranges them.

The mosque artwork and gold Android logo remain the original 3.2.2 assets. Live widget information stays native text. Compact widget assertions use labelled deterministic fixtures; actual app/placed widgets use validated schedule caches. No fixture timetable was written into production caches.

## Upgrade and restoration

Fresh before/after snapshots match all **7 app preference keys, 15 personal-library keys and 4 placed widget configurations**, excluding volatile scheduler bookkeeping. Reminder definitions and recovery backup are unchanged. No new personal-record keys remain. Existing widget IDs **15, 16, 17 and 18** are retained without recreation or orphan configurations.

The installed tablet APK hash matches the delivered debug build. Original tablet font scale, rotation values and location permissions are retained; precise location remains denied. Temporary widget-host binding grants are revoked. Emulator display overrides/text scale are restored after the additional profiles.

Old delivered APK/ZIP hashes are verified before packaging. The original publisher's installed app and `C:\Tools\waktu-re\` were not modified. Personal snapshots and reference APKs are excluded from the delivery archive.

## Practical limits / remaining manual checks

- The user's other physical phone and its OEM launcher were not connected. The phone evidence is an Android emulator; direct launcher placement/resizing on that other phone remains a manual check.
- Very small allocations combined with extreme text scaling can still require a resize affordance. The renderer retains essential prayer information at the verified normal phone sizes and does not shrink accessibility-scaled fonts or invent times.
- Spoken TalkBack traversal, physical keyboard navigation, actual split-screen gestures and subjective animation/frame-time review are not certified. Native descriptions, selected states, measured controls and available-window profiles were checked.
- Reboot/launcher-restart survival and prolonged battery-restricted update delivery were not rerun; their earlier unresolved checks remain unresolved.
- The unsigned optimized release builds; runtime checks use the debug build. Production signing and release-runtime certification remain release tasks.
- No Kiblat calculation/sensor behavior, Quran text, religious translations or PWA behavior changed in this update.

## Evidence and reproduction

`app/build/verification/v330/` contains `native-build.log`, `tablet-verified.log`, `phone-verified.log`, `api24-verified.log`, `phone360-130.log`, `phone360-200.log`, `landscape720-100.log`, `phone-regression.log`, `phone-window-matrix.json`, `ui-contrast.json`, data-retention/final-state reports and direct captures.

1. Build with JDK21: `gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease`.
2. Run `python tools/verify_widgets_v330.py <adb-serial> <evidence-label>` on the selected environment. It executes `WidgetV32Test`, `WidgetAppearanceV321Test`, `LauncherBrandTest` and `PhoneUiV330Test`; its temporary binding grant is revoked in a finally block.
3. `python tools/phone_matrix_v330.py` runs the additional emulator profiles and five regression methods, restoring display overrides and font scale in a finally block.
4. `python tools/finalize_widgets_v330.py` verifies the current tablet's exact APK, data/settings, four widget configurations and unchanged PWA.
5. `python tools/package_v330.py` validates passing evidence and old hashes, then creates the two APKs, gallery, overview, manifest, ZIP and checksums.

See `DESIGN_V330.md` for the changes and primary Android guidance. No new generated imagery, runtime permission or dependency was introduced.
