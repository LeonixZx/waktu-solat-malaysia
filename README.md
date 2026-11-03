# Waktu Solat Malaysia · Android 3.3.3 / PWA 3.1.0

Native Android and a responsive, installable web app share an original ivory/gold identity, published religious content and portable local data. Development application ID remains `com.example.waktusolatmalaysia` so upgrades retain the existing app's data and the reference publisher's app stays separate.

Android 3.3.3 pairs Gregorian and JAKIM-supplied Hijri dates on Today's prayers (Timeline) and improves Hijri labels on Daily Plus (both styles). Compact date metadata keeps the curved phone layout intact. See [DESIGN_V333.md](DESIGN_V333.md) and [VERIFICATION_V333.md](VERIFICATION_V333.md). Version 3.3.2 fixed actual-size widget previews; version 3.3.1 introduced Timeline widgets. Saved layout/zone/theme preferences remain intact. Previous APKs and the [verified v3.3.0 recovery checkpoint](RESTORE_V330.md) remain available. Original generated imagery and [prompts](design/brand-v322/prompts.json) are retained.

## Run Android

JDK 21, minSdk 24, compile/target SDK 34, Build Tools 34.0.0, AGP 8.5.2 and the complete Gradle 8.9 wrapper. No system Gradle is required. Java bytecode targets 17 with core library desugaring.

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot'
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

The SDK is configured at `C:\Users\USER\AppData\Local\Android\Sdk`. The installable development build is `app/build/outputs/apk/debug/app-debug.apk`. The optimized release APK requires publisher signing. Packaged copies are in `deliverables/` after verification.

Dedicated small-phone, standard-phone, large-tablet and Android16 emulator profiles are available for broader Android testing. See [EMULATOR_TESTING.md](EMULATOR_TESTING.md) for the repeatable emulator-only runner and [testing/android-emulators.json](testing/android-emulators.json) for their configuration. Physical tablet checks complement this matrix.

## Run the Windows/browser PWA

```powershell
cd web
npm ci
npm run build
npm run worker:dev -- --port 8787
```

Open **http://127.0.0.1:8787**. In Edge or Chrome choose Install app from the browser menu/address bar. Loopback is a secure context; production needs HTTPS. The Worker serves the built SPA plus validated, allowlisted APIs. `npm run dev` provides Vite hot reload while the Worker runs. For production run `npm run deploy` after selecting your own Cloudflare account/domain. No production account is configured or published by this build.

## Features

- Home, Quran, Worship, Explore and Saved appear in exactly one primary menu: bottom tabs below 600 logical units, an 88-unit rail at 600–1199, or a 240-unit sidebar at 1200+. Short windows below 480 units of available height use bottom tabs. Settings lives in the rail/sidebar footer or phone toolbar. There is no duplicate drawer/hamburger. Quran and collections use two panes when their actual content area reaches 680 units, has at least 480 units of height and text scaling is at most 130%. Reading width stays bounded. See [IMPLEMENTATION_V31.md](IMPLEMENTATION_V31.md) and [NAVIGATION_UX.md](NAVIGATION_UX.md).
- Original crescent/arch/clock mark, adaptive/monochrome Android icons, PWA maskable icons, six optimized WebP illustrations, readable ivory/gold and dark themes, Malay/English controls, selectable Arabic and labelled published translations.
- Official JAKIM schedules for 60 Malaysian zones, eight timetable entries, a grouped five-prayer dashboard, genuine next-prayer countdown, date navigation and validated zone/month caches. No missing prayer time is estimated. All prayer calculations use Asia/Kuala_Lumpur.
- Four native home widgets: next prayer, wide prayer strip, today’s five obligatory prayers and Daily Plus. Choose app/independent zone and Automatic/Ivory/Noir appearance. Daily Plus optionally shares recorded/unrecorded prayer tracking with the app. Native measured layouts respect text size; unsupported bounds show a resize cue. Tap opens the displayed date/zone without changing the app default. Add widgets / My widgets provides previews, configuration and manual launcher guidance.
- Per-prayer Off / Notification / Adhan modes, migrated from v2 flags. Personal reminders support eight anchors, ±360-minute offsets, weekday selection and next-occurrence previews. Weekdays belong to the anchor prayer's date. Generation identifiers invalidate edited/deleted alarms. Connected background refresh runs approximately every 12 hours. Reboot/time changes plan from disk, outside network receivers. Exact-alarm denial uses approximate delivery and is explained in the interface.
- Virtualized Quran surahs and ayahs; Basmeih Malay / Sahih International English selection; bookmarks and reading position; streamed Alafasy recitation; resumable selected/full text queues, storage reporting and deletion. Pausing retains the queue. Deleted text cannot be recreated by an older in-flight response. Android uses WorkManager; browser queues resume when reopened/online. Audio is streamed.
- Searchable doa, wirid, niat and twelve Sahih al-Bukhari readings, with supplied/published translations. Indonesian hadith translation is explicitly labelled. Persistent tasbih, goals, prayer/fasting records, location/sensor-based Kiblat, dual-date calendar and UTC iCalendar export.
- Reviewed TV JAKIM / Mufti Wilayah / Yaqeen catalog. Genuine publisher thumbnails, click-to-load official YouTube playback, provider links and unavailable states. TV JAKIM's live-channel link is available; the catalog makes no claim that a broadcast is currently live. Articles open their original publisher pages. Hidden video playback pauses. No private publisher credentials or gated services are used.
- Account-free JSON export/import with preview, merge/replace, stable content IDs and a pre-import recovery copy. Imported personal reminders are disabled. Platform permissions and widget instance IDs are excluded. Android/Web backups use the same versioned contract.
- PWA offline launch, cached prayer months, downloaded Quran, bundled collections, saved items and tracking through IndexedDB/Workbox. Web provides bearing guidance and calendar export; scheduled background notifications/adhan remain native Android features.

## Source and checks

`app/src/main` is the Java/XML app; `web/src` is React/TypeScript/Vite; `web/worker` is the read-only adapter; `shared/` contains versioned tokens, catalog, language map, backup fields and genuine test fixtures. The generated art originals and prompts are under `design/assets/`, and brand SVG masters are under `design/brand/`.

```powershell
cd web
npm test
npm run test:e2e
```

Browser tests require the built Worker at port 8787. Android instrumentation requires its separate test APK. Always specify the intended ADB serial. `tools/verify_android_v3.py` changes **only emulator-5554**, restoring emulator size/density/font settings in `finally`; its widget test grants/revokes a temporary emulator bind permission. The system tests are also explicitly emulator-only.

Current results and remaining physical checks are in [VERIFICATION_V31.md](VERIFICATION_V31.md). [VERIFICATION_V3.md](VERIFICATION_V3.md) records the earlier baseline. Also see [ARCHITECTURE_V3.md](ARCHITECTURE_V3.md), [API_SOURCES.md](API_SOURCES.md) and [ASSET_PROVENANCE.md](ASSET_PROVENANCE.md). Public release still needs publisher signing and deployment configuration. Physical Kiblat accuracy is not certified by synthetic sensor tests; a trusted-reference comparison is still required. Manual TalkBack listening and long-running OEM battery behavior require final release QA.

`C:\Tools\waktu-re\` is strictly read-only and is not a build dependency. Existing app preferences, saved IDs, tracking and prayer caches are preserved in place; installation never clears app data.
