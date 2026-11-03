# v3.0.2 verification · 2 November 2026

The debug app is installed on the connected Xiaomi tablet (`DEVICE_SERIAL`, Android 16/API 36). The reference app and C:\Tools\waktu-re\ were not modified. This is local development verification; signing and public hosting remain release configuration.

The 3.0.1 fix removes the redundant drawer/menu modal, adds a separate Settings entry and direct Quran download access, and flattens the rail/sidebar. The 3.0.2 follow-up fixes Settings incorrectly selecting Saved. Builds, unit/browser checks, current-data retention and targeted navigation/selection tests were rerun. The broader functional and width/text-size results below are the v3.0.1 baseline. Widget host, offline Android, Doze and reboot rows retain the passing v3 baseline results; those services were unchanged. See [NAVIGATION_UX.md](NAVIGATION_UX.md).

## Passing checks

| Area | Result |
|---|---|
| Native build | Debug APK, optimized unsigned release APK, lint (zero errors), 32 JVM tests |
| Native functional regression | v3.0.1 baseline: 19 tests. v3.0.2: three targeted navigation/selection tests at phone, 600 dp rail, and 1280 dp sidebar sizes, including Saved → Settings, recreation and Back |
| Adaptive native layout | v3.0.1 baseline: 15 width/text-size configurations plus short landscape. Selection follow-up tested at phone/rail/sidebar sizes |
| Native menu taps | Actual bottom-tab, rail and sidebar taps; exactly one primary menu asserted |
| Physical tablet | v3.0.1 baseline: 20 tests. v3.0.2: three selection/navigation tests plus direct screenshots in each orientation |
| Upgrade | Fresh snapshots retain all current app/personal preference fields across reinstall/testing, including the current counter; volatile scheduler fields excluded |
| Widgets | Actual AppWidgetHost binds/resizes all three providers; independent zones do not change the app default; light/dark/system RemoteViews render |
| Reminders | Planner offset/weekday/midnight tests, stale generation rejection, edit/delete cancellation, actual AlarmManager delivery in forced deep Doze |
| Reboot | Real emulator reboot restored a future task from cached official times with a new planner generation; no missed backlog |
| Text downloads | Android selected-surah queue resume, aligned Malay/English text and deletion with bookmarks retained; browser transactional queue and late-response deletion tests |
| Portable data | Android export consumed by browser import; browser backup validated on Android; preview, merge/replace, invalid input rejection and disabled imported tasks |
| Web | TypeScript/Vite production build, 12 unit tests, 9 Playwright scenarios, Worker deployment dry run |
| Web layout/accessibility | 320–1920 px routes without page overflow; 200% text across seven routes; axe WCAG A/AA checks; keyboard focus, reduced motion and dark theme |
| Offline PWA | Service worker control, install manifest, deep-link reload, cached Quran and bundled collections after network disconnection |
| Offline Android | Network disconnected; cached prayers/readings remain usable and an uncached date shows no invented times |
| Live adapter | Genuine JAKIM November 2026 month (30 days) and Al-Fatihah four-edition response |

## Measurements

- Earlier v3 cold debug activity launches on the tablet: 1770 ms, 1743 ms, 1852 ms; median 1770 ms. These are development measurements, not release startup guarantees.
- The physical in-app sample records 300 frames while opening/scrolling the Quran list, 0 beyond Android's reported frame deadline, longest 52.395058 ms; total PSS 128619 KB and native allocated heap 55236448 bytes. The raw report is in `app/build/verification/v3/tablet-probe.json`. Deadline interpretation follows [Android FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics#DEADLINE); this debug/instrumented sample is not a release benchmark.
- All six runtime WebP illustrations total 450,570 bytes versus 5,977,879 bytes for the old three raster illustrations: **92.5% less raster data**. Each derivative is under 90 KB. Original generated PNGs remain outside the runtime bundles.
- Native surah/ayah holders reuse their child views, stable IDs and click controls. Artwork decoding uses measured bounds off the main thread with a bounded cache. Unchanged dashboard labels are not reset on each countdown tick.

## Evidence and repeatability

Canonical logs and captures are under `app/build/verification/v3/`: `final-verified-build.log`, `native-regression-verified.log`, `android-*-text*.log`, `widget-host.log`, `native-download-queue.log`, `native-doze-delivery.log`, `reboot-result.json`, `tablet-probe.json`, `tablet-upgrade.json`, `pwa-*-delivery-final.log` and `delivery-metrics.json`.

The 3.0.1 logs use the `navigation-` prefix: `navigation-build.log`, `navigation-release.log`, `navigation-native-regression.log`, `navigation-tablet-suite.log`, `navigation-layouts.json`, `navigation-data-retention.json` and `navigation-web-*.log`. Device rotation and emulator display/font settings were restored. The debug-only in-app probe is absent from release. The historical v2 snapshot has a different tasbih counter; it is retained as historical evidence, never imported or used to reset the current count.

Selection previews: [Settings on the tablet](app/build/verification/v3/settings-selection-tablet-landscape.png).

Useful previews: [tablet portrait](app/build/verification/v3/navigation-tablet-portrait.png), [tablet landscape](app/build/verification/v3/navigation-tablet-landscape.png), [web desktop](app/build/verification/v3/nav-home-1280x900.png), [web phone](app/build/verification/v3/nav-home-360x800.png).

## Release checks still requiring human/platform review

Manual TalkBack listening/Arabic pronunciation, native keyboard navigation with real hardware, Windows installation through the user's browser profile, and prolonged OEM battery/launcher behavior remain release QA. Automatic widget pinning did not show a launcher prompt on this MIUI tablet; the app provides a manual launcher-widget guide. Actual binding/resizing is verified in the emulator widget host. Full 114-surah network downloads and current live-program playback depend on upstream availability; selected downloads, queue persistence and unavailable-media behavior are covered. Publisher signing, domain/account setup and content distribution approval are not configured by this local build.
