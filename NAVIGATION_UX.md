# Navigation cleanup · 3.0.2

This records the earlier navigation repair. v3.1 retains this single-menu behavior and adds content-area-based Ibadah/Quran readers; see [IMPLEMENTATION_V31.md](IMPLEMENTATION_V31.md) and [VERIFICATION_V31.md](VERIFICATION_V31.md) for current checks.

The 3.0.2 follow-up corrects a selection bug: Settings, Backup and About no longer select Saved. Only the separate Settings utility is highlighted on those screens, including after activity recreation. Returning to Saved selects Saved again. Android menu selection is driven by one route mapping and permits no selected hub on utility screens; the web follows the same rule. Targeted checks cover phone, rail and sidebar selection, recreation, Back, and the live tablet in both orientations. All nine web scenarios pass.

The tablet showed two menus because the adaptive rail/sidebar and the old modal drawer were both retained. The toolbar could still open the drawer over the permanent navigation. The Android drawer and web menu modal have now been removed.

## One primary menu

| Available width | Primary navigation |
|---|---|
| Below 600 | Five bottom tabs |
| 600–1199 | 88-unit rail with icons and labels |
| 1200+ | 240-unit permanent sidebar |

Windows with less than 480 units of available height use bottom tabs. The Android measurement excludes system-bar/cutout insets. The same five destinations remain in the same order: Home, Quran, Worship, Explore, Saved.

Branding is above the side menu, Settings is below it, and the middle menu contains only the five hubs. On phones Settings is in the root toolbar. Android child screens show an Up arrow. The rail/sidebar use flat surfaces and rounded tonal selection rather than elevated panels.

Downloads is directly available in Quran beside Continue reading. Schedule/zone controls stay on Home; prayer-linked reminders, calendar, widgets and devotional tools stay in Worship. Backup, display, language and guide options are reachable from Settings. This keeps the global navigation short while retaining the features.

## Verified

- Android debug/release builds, lint, and 32 JVM tests.
- API 34 emulator: 19 functional tests; 320/360/600/840/1280 dp at 100/130/200% text; actual bottom-tab, rail, sidebar and Settings taps; 840×400 short-landscape fallback.
- Physical Xiaomi tablet: 20 functional tests; additional two-test navigation passes in portrait and landscape, with in-app screenshots. Rotation settings restored afterward.
- Fresh before/after reinstall checks retain all current application and personal preference fields, including the current tasbih count. Internal scheduler generations are excluded from this comparison. The older v2 snapshot has a different counter value and is retained as historical evidence; it is not imported or used to reset current data.
- PWA: TypeScript/Vite build, 12 unit tests and 8 browser scenarios covering responsive navigation, feature routes, Arabic readers, keyboard/accessibility, backups and offline relaunch.

Logs and direct captures are in `app/build/verification/v3/navigation-*` and `nav-home-*`. No app data was cleared and the reference workspace was not modified. Manual assistive-technology and long-running OEM testing remain part of release QA, as described in VERIFICATION_V3.md.
