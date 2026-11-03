# Android widgets — v3.2.0 design and implementation

Android-only release: `com.example.waktusolatmalaysia`, version code 8. JDK 21, minSdk 24, compile/target 34, Build Tools 34.0.0. PWA remains 3.1.0. The reference workspace is read-only and was not needed for this release.

## Widget family

| Launcher entry | Preferred placement | Rendering |
|---|---|---|
| Solat seterusnya / Next prayer | 2×2 | Zone, displayed date, next obligatory prayer and 32sp absolute time |
| Bar waktu solat / Prayer strip | 4×1 where the launcher supports it | Single metadata line above a side-by-side prayer and time |
| Waktu hari ini / Today’s prayers | 4×2 | Five-column band, wrapped 3+2 band, or narrow vertical list |
| Harian Plus / Daily Plus | 4×4 | Five prayers, 48dp settings action, optional Hijri/auxiliary times, optional five tracking controls |

The original three provider class names are retained. `NextPrayerStripWidget` is a fourth component. `WidgetProviderDescriptor` explicitly maps components to families; the old provider array and integer methods remain compatibility adapters only.

## Visual rules

- Ivory: `#FCF9F3` background, `#28231E` text, `#6F6459` metadata, `#966025` accent, `#F6EBD8` next-prayer highlight. Selected timetable text uses `#8C581E` to keep normal text above 4.5:1 on that highlight.
- Noir: `#171410` background, `#F7EEDD` text, `#C2B5A3` metadata, `#E8B86D` accent, `#3D3020` highlight.
- Automatic follows Android’s system appearance, independently of an app-only theme override.
- Platform widget background radius on API31+, 24dp below API31; 12dp internal highlights. Padding 12dp; gaps 4/8/12/16dp. A very small unavailable-space affordance uses a centered 48dp resize icon with a full spoken description.
- Native scalable text: 32sp next time, 18sp timetable times, 14sp prayer names, 12sp metadata. Jakarta regular/semibold on API26+, system sans fallback on API24/25. No baked-in text, raster artwork, source timestamps, countdown wakeups or permanent refresh buttons.
- Next prayer has a tonal background, a visible chevron and an explicit accessibility description. After the final prayer, the next-prayer family shows tomorrow’s date/time only when tomorrow’s validated data exists. Daily widgets retain today’s timetable.

## Fit selection and previews

API31+ uses the launcher’s supplied `OPTION_APPWIDGET_SIZES` and a `SizeF → RemoteViews` map. Earlier Android uses the supplied portrait/landscape bounds. Each candidate is inflated using framework-only views and measured with the platform’s actual font sizing and font metrics. Single-line text must fit its measured width; child text and total height must fit. There is no font-scale threshold that converts the daily widget into a next-prayer widget.

Next card/strip and daily band/grid/list retain their family’s purpose. Daily Plus tries tracking with optional metadata, then shorter zone metadata, then fewer optional fields. If all five tracking controls cannot fit, it uses the information layout and a resize-to-track cue. If essential content cannot fit, it displays “Besarkan widget / Resize widget”; the saved options remain intact. A tiny icon-only resize state avoids clipping even at 200% text in 120×72 supplied bounds.

Add widgets and configuration previews use the installed widget renderer and real cached schedules. Configuration previews use the instance’s supplied dimensions, bounded by the available preview width. Controls in previews have no pending intents and are disabled. Preview creation is posted after parent layout, avoiding empty first-frame previews. No schedules are fabricated for previews. Deterministic test fixtures are confined to tests.

## Configuration and interactions

The five app hubs remain unchanged. The widget screen has local Add widgets / My widgets tabs. Four Add entries provide native previews, launcher pin requests and a manual placement guide. My widgets enumerates bound instances and offers per-instance settings and queued schedule refresh.

Configuration uses a bounded Material form inside ConstraintLayout with system/cutout insets. Follow-app zone or a searchable state/zone picker, Automatic/Ivory/Noir, and Daily Plus options are drafts until Save. Draft state survives recreation; Cancel writes nothing. Language and hour format follow the app. Save validates that the instance and zone still exist, and changes only that instance.

Pin requests report a request, never immediate success. An immutable callback compares the bound component’s new IDs against the request’s initial ID set before showing success. The setup guide remains available when a launcher does not show a pin prompt. Xiaomi manual picker placement was exercised separately from the app-host tests.

Main surface taps open their displayed date and zone in Schedule without changing the app default. Daily information cells have no separate tap actions. Daily Plus has an independent 48dp settings action and, when enabled and fitted, five separate 48dp recorded/unrecorded actions.

## Tracking and compatibility

`PrayerTrackingStore` shares the existing `personal_library` preferences with the app tracker. IDs map explicitly: `fajr → Subuh`, `dhuhr → Zohor`, `asr → Asar`, `maghrib → Maghrib`, `isha → Isyak`. Keys remain `prayed_<Malaysia-date>_<Malay-name>`. English labels cannot create alternate keys. Backup schema 1 is unchanged.

Immutable pending intents carry widget ID, Malaysia date, canonical prayer ID and intended boolean state. The receiver serializes validation/writes, verifies the live Daily Plus instance and tracking setting, rejects stale-date actions, and uses idempotent set operations. Duplicate deliveries do not toggle twice. The app observes the same records; tracking widgets refresh after writes/imports. Tracking defaults off and does not grant prayer validity or mark anything automatically.

`WidgetConfigV2` preserves unknown legacy fields, zones, names and themes. `system/light/dark` retain their meaning as Automatic/Ivory/Noir. Legacy Daily Plus enables Hijri and auxiliary fields; tracking stays off. IDs/components are never recreated for migration. Package replacement refreshes cached layouts. Existing refresh pending intents remain supported. Widget restore remaps configuration when Android supplies replacement IDs. Migration/config writes share a deletion lock and revalidate the bound component, preventing a queued renderer from recreating a removed instance’s settings.

## Refresh and offline behavior

`WidgetRenderModel` loads only validated disk data; no broadcast receiver performs HTTP. `WidgetScheduleRefreshWorker` uses unique work per zone with connected-network constraints. A shared zone/month single-flight in `PrayerRepository` coalesces concurrent downloads across callers. Successful schedule downloads refresh cached widgets, including downloads started from app screens.

The approximately 12-hour prefetch, normal 30-minute widget fallback, prayer boundaries and Malaysia midnight remain. Reboot, clock/timezone changes, configuration changes, imports and language/hour/zone/record changes refresh widgets. Removing the last widget for a zone cancels only that zone’s widget work. Cache files and app reminder jobs are retained; no unused boundary alarm is kept when no widgets remain.

Cached monthly schedules remain usable offline. Missing today and missing tomorrow are distinct; tomorrow’s missing Subuh is never replaced with today’s time. No new location, notification or exact-alarm permission is requested.

## Implementation locations

- `app/src/main/java/com/example/waktusolatmalaysia/widgets/`: descriptors, immutable config/model, rendering, providers, configuration and actions.
- `preferences/PrayerTrackingStore.java`, `fragments/TasbihFragment.java`: shared records and visible app synchronization.
- `services/WidgetScheduleRefreshWorker.java`, `repo/PrayerRepository.java`: zone work and fetch coalescing.
- `app/src/main/res/layout/widget_*`, `layout-v26/widget_*`, `drawable/widget_*`, `xml/widget_*_info.xml`: native layouts, font fallback, tokens, icons and launcher metadata.
- `WidgetV32Test`, `WidgetUpgradeV32Test`: measured layouts, actions, configuration, date/zone navigation and actual upgrade coverage.

References: [Android widget layouts](https://developer.android.com/develop/ui/views/appwidgets/layouts), [Android widget update guidance](https://developer.android.com/develop/ui/views/appwidgets/advanced).
