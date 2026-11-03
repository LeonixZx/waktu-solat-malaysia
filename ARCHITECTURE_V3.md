# v3 implementation notes

For the current v3.1 reader layout, structured reading models, Kiblat quality pipeline and launcher changes, see [IMPLEMENTATION_V31.md](docs/history/IMPLEMENTATION_V31.md). The 680-unit pane threshold is measured inside the available navigation content area; global navigation breakpoints are unchanged. Current results are in [VERIFICATION_V31.md](docs/history/VERIFICATION_V31.md).

## Design and navigation

`shared/design-tokens.json` defines the palette, spacing 4/8/12/16/24/32, corners 12/20/28 and breakpoints 600/840/1200. Normal text uses dark brown or readable muted brown; text-bearing gold was darkened after automated contrast checks. Gold decoration remains separate from text contrast. Android uses ConstraintLayout, Material Components, NavigationView and RecyclerView; the PWA uses CSS Grid and TanStack Virtual. Short CSS transitions respect reduced motion; the native layout avoids ornamental animation.

The shell exposes exactly one primary navigation surface: bottom tabs, rail, or permanent sidebar. No modal drawer or hamburger remains. Brand identity sits above the side menu, with Settings pinned below it. Phones expose Settings in the root toolbar; child screens retain Up navigation. The rail/sidebar are flat, with rounded tonal selection. Width and available height (after system insets) determine the Android shell; short windows fall back to bottom tabs. Both platforms show the same five hubs, with Downloads under Quran and worship tools inside Worship. Hub selection returns to its hub instead of restoring an unrelated page from Home's old navigation stack. Cards clip artwork and grow with text. Narrow/large-text calendars use a day list. Arabic reading is selectable and direction-aware. The compact native recitation bar remains above navigation/insets.

`ui/Artwork` decodes bundled images off the main thread using measured pixel bounds and a bounded bitmap cache. The dashboard timer changes the visible countdown/clock text; prayer-row styling changes only when the next prayer changes. Quran lists recycle their rows instead of inflating entire long surahs.

## Data, scheduling and migration

`PrayerRepository` retains v2 `prayer_times/{zone}_{month}.json` files and validates dates, zone, all eight times and chronological order. Concurrent cache reads/writes share a lock. `ContentRepository` retains `quran/surah_{number}.json`; old Malay caches still work and acquire the English edition when requested. New downloads validate aligned Arabic/Malay/English/audio IDs before atomic writes. Per-surah generations protect text deletion from late responses.

`PrayerMode` maps each old enabled flag to Notification or Adhan using the old global setting; disabled flags remain Off. Existing `personal_library` IDs and tracker keys stay unchanged. `TaskStore` uses atomic JSON and a shared lock. `TaskPlanner` evaluates the anchor date's weekday before applying the signed offset. No planner synthesizes times. `ReminderScheduler` cancels superseded PendingIntents and persists a new alarm generation. Receivers re-check generation, zone, enabled state and delivered occurrence. Personal tasks use their own notification channel.

WorkManager prefetches each app/widget zone's needed month range around yesterday through the next nine days, covering midnight offsets and month transitions. Failed network refresh retains honest cached data and retries. Application startup plans cached alarms even while network work is constrained. Broadcast receivers do no network I/O. Widgets refresh on schedule updates, configuration changes, boundaries and platform update callbacks, without second-by-second background timers.

`BackupBundle` schema 1 includes preferences, bookmarks, reading, tracker and reminders. Import validates types/ranges, previews counts and writes a recovery snapshot before applying. Merge keeps existing preferences; replace restores the file's supported fields. Imported task definitions are disabled; replace also resets prayer alerts. Neither format restores OS grants or widget IDs. Android and browser backup exchange is covered by tests.

## Web and API adapter

React + TypeScript + Vite; Workbox precaches shell/fonts/optimized artwork/bundled reading collections. IndexedDB separates content caches from personal state. Queue changes use read/write transactions so additions and completions do not overwrite each other. Update prompts require an explicit reload and are disabled during active downloads. Reading is never interrupted automatically.

Cloudflare Workers static assets serve SPA deep links. API routes are GET-only:

| Route | Validation / provider |
|---|---|
| `/api/prayers?zone=WLY01&month=2026-11` | Known official zone and bounded year/month; fixed JAKIM endpoint |
| `/api/quran/1` | Integer surah 1–114; fixed four-edition AlQuran Cloud endpoint |
| `/api/catalog` | Reviewed repository catalog |

There is no arbitrary URL proxy, account, CMS or unfiltered aggregation. Provider requests have time/size limits. Worker logs contain service, outcome and latency; native prayer fetch logs contain outcome and elapsed milliseconds, without coordinates or personal tracking records.

YouTube is initialized only after interaction, uses official player controls/fullscreen, pauses when hidden and provides an original-source fallback. Embedding, DRM and client-identification restrictions remain controlled by the provider. Curated catalog revisions require reviewing attribution, language, topic, URL, video ID and thumbnail; channel/live status is not inferred from a static entry.

## Release configuration

The default target is the checked-in Cloudflare Worker configuration. `npm run deploy` builds assets and uploads after your own account is selected. No production domain/account secrets are included. Android's optimized release APK is unsigned. Keep the installed development ID for local upgrades; changing it creates a separate installation and requires a deliberate data-transfer/release decision.
