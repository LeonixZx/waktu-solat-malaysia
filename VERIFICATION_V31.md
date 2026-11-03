# v3.1.0 verification · 2 November 2026

This records the initial v3.1.0 delivery. The later physical-reference session and Android 3.1.1 location-refresh repair are recorded in [VERIFICATION_V311.md](VERIFICATION_V311.md), which supersedes the initial physical-test status below. The PWA and broad v3.1.0 layout results remain the baseline.

The updated debug app is installed on the Xiaomi tablet (`DEVICE_SERIAL`, API 36). Native Android and the PWA implement the Ibadah redesign, sectioned religious readers and explicit Kiblat confidence states. **Physical Kiblat direction accuracy remains unverified:** no independent reference or case-removal comparison has been supplied. Software and sensor-event passes do not certify the physical direction.

## Automated results

| Area | Result and scope |
|---|---|
| Android build | Debug and optimized unsigned release APKs; Gradle wrapper with JDK 21; SDK/build tools unchanged |
| Native unit tests | 41 passing: prayer parsing/rollovers, reminders/offsets, calendar UTC export, all bundled reading sections, known Kiblat bearings and confidence gates |
| Android lint | Build passes, zero errors; 163 warnings remain, mostly unused resources, internationalization heuristics, dependency notices and small secondary text. This is not a warning-free lint result |
| Native palette contrast | Declared primary, secondary and prayer text against background/surface/soft-gold, plus white on primary: all at least 5.01:1 in light and dark themes (`native-contrast.json`) |
| Physical tablet suite | 31 passing tests: existing native/navigation regression, all 36 readings, search, bookmark/recreation, Rumi/translation labels, Tasbih validation/undo/reset/tracking, reminder create/edit/delete, calendar rollover/selected date, logo and live sensor events |
| Emulator features | 10 passing Ibadah, branding, calendar and reading-state tests on API 34 |
| Android layout matrix | 320, 360, 600, 800, 840 and 1280 dp × 100%, 130%, 200% text; 1280×800 landscape, 800×600 resized window and 840×400 short window: 21 passing configurations |
| Layout assertions | Eight screens: collections, Worship, Tasbih, reminders, calendar, widget guide, Kiblat and Quran. Reminder dialog opens at each configuration. Collection rows fill the list width and have visible title space; text bounds and pane rules are checked |
| Actual multi-window | Android split-screen mode entered through Recents and asserted through `isInMultiWindowMode`; the same Ibadah layout checks pass |
| Physical layouts | Direct portrait/landscape captures and a dark-theme pass; category rows, actions and reader panes checked after the final repairs |
| Widgets | Real AppWidgetHost binds all three providers, resizes them and checks independent zones at normal and 200% text. Light/dark/system RemoteViews are exercised by the tablet suite. Temporary host IDs/bind permission removed |
| Notification denial | Real Android permission dialog denied; new reminder remains disabled. Emulator notification permission and flags restored exactly |
| Web build | TypeScript/Vite build, 17 unit tests, 15 Playwright scenarios, Cloudflare Worker deployment dry run; no public deployment |
| Packaged PWA | Extracted delivery zip serves root/deep links, service worker, manifest and eight catalog entries through its bundled Worker; invalid parameters, arbitrary API routes and POST requests are rejected |
| Web layout/accessibility | Collections at 320–1920 px and 100/130/200% text; no horizontal page overflow; keyboard focus, axe WCAG checks, dark theme and reduced-motion setting |
| Web reading/state | All 36 readings, supplied sections, active saved filters, offline reading, reload/resize scroll retention and selected reading retention |
| Web reliability | Unknown browser north reference never receives verified-alignment status; calendar provisional-date notice and unavailable event-year state; existing backup/offline/download scenarios remain passing |
| Personal data | Fresh before/after snapshots retain the original app preference fields, personal fields, widget settings, reminder definitions and pre-import recovery state. No historical backup was imported to reset current data |

The earlier v3 system-level Doze/reboot, full download-queue and offline-native results remain documented in [VERIFICATION_V3.md](VERIFICATION_V3.md). Those services were not changed in this release and those particular system experiments were not rerun here.

## Kiblat evidence and limitations

The calculation checks cover known bearings, invalid coordinates and the undefined bearing at the Kaaba. Synthetic Android matrix tests cover sixteen natural-heading/display-rotation combinations, plus accelerometer/magnetometer orientation. State tests cover one-time declination correction, circular 359°/0° smoothing, future/previous-boot location timestamps, stale heading/location, low sensor accuracy, tilt, uncertainty and missing heading/sensors. These prove software behavior under controlled inputs.

On the real tablet, absolute-orientation sensor events arrive and the diagnostic heading changes. The portrait capture shows a measured heading and tilt while still acquiring location; it correctly avoids an alignment claim. No physical Qibla deviation in degrees is reported because a trusted marker and controlled positioning were not available. Magnetic-case presence is unknown. Neither the cover nor device location was changed by automation.

To complete the physical comparison, use a verified Qibla marker or independently established reference, obtain a fresh location, place the tablet flat, and point its **current screen's top edge** along the reference. Record bearing, heading, quality and signed deviation in portrait and landscape, first without the case and then with it. Repeat away from metal. The app's ±5° band is an approximate alignment aid, not a guarantee of sensor accuracy. The [official Selangor explanation](https://www.muftiselangor.gov.my/2025/11/11/17-taudhih-al-falak-ketepatan-aplikasi-telefon-bimbit-dalam-menentukan-arah-kiblat/) discusses magnetic interference and provides the eFalak reference.

## Launcher identity

Package resolution confirms our launcher is `com.example.waktusolatmalaysia/.views.MainActivity`, separate from `com.murad.waktusolat/.views.MainActivity`. Current launcher inspection shows our gold crescent/arch on ivory alongside the original brown mosque icon. No green shortcut was visible during that inspection, so the earlier green shortcut cannot be attributed conclusively from its colour alone. Android themed-icon recolouring remains controlled by the launcher.

Our normal/round/adaptive/monochrome resources were checked. The previous adaptive background followed the application theme and could turn black; it is now constant ivory. Foreground padding was reduced within the safe bounds. The original app, launcher data and system theme were left untouched.

## Direct captures

Recent device captures are under `app/build/verification/v31/tablet-previews/tablet-verification/`. These include:

- [Ibadah hub](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-worship.png), [tablet list and reader](app/build/verification/v31/tablet-previews/tablet-verification/tablet-landscape-collection.png), [dark collection](app/build/verification/v31/tablet-previews/tablet-verification/tablet-dark-collection.png).
- [Doa](app/build/verification/v31/tablet-previews/tablet-verification/tablet-v31-doa.png), [Wirid](app/build/verification/v31/tablet-previews/tablet-verification/tablet-v31-wirid.png), [Niat](app/build/verification/v31/tablet-previews/tablet-verification/tablet-v31-niat.png), [Hadis](app/build/verification/v31/tablet-previews/tablet-verification/tablet-v31-hadith.png).
- [Tasbih](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-tasbih.png), [reminders](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-tasks.png), [reminder form](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-task-form.png), [calendar](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-calendar.png), [widget guide](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-widgets.png), [Kiblat](app/build/verification/v31/tablet-previews/tablet-verification/tablet-portrait-qibla.png).
- [Web Wirid reader](app/build/verification/v31/web-wirid.png), [320 px / 200% text](app/build/verification/v31/web-collection-320-text2.png), [web Kiblat](app/build/verification/v31/web-qibla.png).

Screenshots are native display captures. The test helper uses shell `screencap` and checks current display dimensions, avoiding stale UiAutomation screenshot bounds after tablet rotation. Initial screenshots exposed a real collapsed collection-row bug; explicit RecyclerView/FrameLayout widths fixed it, and stronger row/title assertions now cover the matrix.

## Reproduce and package

Canonical logs and private before/after snapshots are in `app/build/verification/v31/`. Key logs are `native-build.log`, `tablet-suite.log`, `android-layouts.json`, `actual-split-screen.log`, `widget-host-text*.log`, `notification-denial.log`, `data-retention.json` and `web-*.log`.

`tools/verify_ibadah_v31.py` runs the emulator matrix; `tools/verify_ibadah_system_v31.py` runs emulator features, actual split screen, widget host and notification denial; `tools/verify_tablet_v31.py` captures orientations and compares fresh data. ADB serials are explicit. Geometry, font, rotation and temporary permission settings are restored. `tools/package_v31.py` checks passing evidence and versions before creating APK/PWA artifacts and SHA-256 records. Personal snapshots are excluded from the PWA package.

Manual TalkBack listening/Arabic pronunciation, native keyboard use with actual hardware, a real Windows browser-profile installation, prolonged OEM launcher/battery behavior and the physical Kiblat reference comparison remain unverified. The MIUI launcher previously did not display an automatic widget pin prompt; the app includes manual placement instructions and the emulator host verifies actual binding/resizing. Publisher signing and production Cloudflare account/domain setup remain release steps. `C:\Tools\waktu-re\` stays read-only.
