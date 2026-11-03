# Android 3.3.0 — phone navigation, controls, settings and widget fit

This Android update addresses the phone-specific issues reported after 3.2.2. It retains application ID `com.example.waktusolatmalaysia`, minSdk24, compile/target34, Build Tools34.0.0 and JDK21. Version **3.3.0 / code11**. The PWA stays at 3.1.0.

## Navigation and shortcuts

- The bottom navigation has a bounded 72dp rounded surface, 12dp side margins and 8dp vertical margins. Its transparent shell owns system-bar padding exactly once. The Material navigation view's separate automatic inset handler is replaced, preventing a second bottom inset from squeezing icon/label space.
- Five original monochrome 24-unit navigation glyphs use 2.1-unit strokes, consistent tinting, an explicit tonal active indicator and shorter Quran/Teroka/Simpan labels. Touch items remain at least 48dp. At text scaling above 150%, labels yield to icons with native item descriptions rather than being clipped or shrunk.
- The toolbar logo is explicitly 32×36dp instead of inheriting the 512px bitmap's intrinsic width.
- Home shortcuts share a 48dp tonal icon badge, reserved two-line labels, equal row heights and a 112dp minimum tile height. Their grid wraps using available width and text scale, including the longer personal-reminder label.
- Shared primary/tonal buttons use native text, 48dp minimum height, 12dp corners, explicit theme colours, no elevation, consistent padding and text-adjacent icons. General glyphs use the same theme-aware ink.

## Koleksi and Teroka

`ChoiceBar` replaces mismatched tab implementations with one native Material-button selection component. It measures actual labels/icons at the current text scale, gives choices equal widths, wraps into balanced rows, and exposes checked/selected state to accessibility services. Four collection categories can become two rows; three media/theme choices can become full-width rows when needed. No accessibility font shrinking is used.

Koleksi keeps the existing content IDs, saved/search/category state and tablet list/reader behavior. It adds a single outlined search field with one placeholder and consistent selected/saved styling. Teroka retains the selected media filter across recreation; its actions have a gap, and unavailable thumbnails use a tonal native icon placeholder rather than an empty white block.

## Floating settings

The top-right settings action, rail utility and sidebar utility open the same bounded end panel. The underlying hub stays in place. The rounded surface is at most 460dp wide, stays inside the visible window with 12dp margins, has a dimmed dismissible backdrop and an explicit 48dp close action. Entry slides/fades for 240/180ms; exit uses 180/150ms. Window animations are disabled when Android reports animators disabled. Rotation/recreation uses DialogFragment's normal restoration; duplicate openings are guarded by a stable fragment tag.

Grouped settings cover Appearance, Quran reading, Prayer/reminders and Data/app. There is one **System / Light / Dark** control, replacing the previous theme picker plus separate dark switch. Language, time format, published translation selection, translation visibility, Arabic sizing, location, reminders, widgets, backup and source information remain available. Links dismiss the panel before opening the chosen page. The legacy settings route uses the same grouped content.

Theme selection is persisted before AppCompat applies it. Locale wrapping carries the requested night configuration explicitly; System mode reads the device's actual night state instead of retaining a previously forced activity configuration. Dark resources cover primary/on-primary text, control surfaces, artwork scrims, image text and system-bar icon appearance. Automatic elevation overlays no longer unexpectedly lighten nested cards. This follows [Android's DayNight guidance](https://developer.android.com/develop/ui/views/theming/darktheme).

## Widgets and the resize message

The old normal next-prayer template needed more height than a phone launcher could allocate to a nominal 2×2 cell. Five-time templates could similarly fail strict text-fit checks in shallow cells. Zero-valued size options during initial binding could also be treated as real available space.

The renderer now retains the existing full templates first, followed by compact native alternatives:

- Next prayer: zone/date, prayer and 24sp absolute time with 8dp content padding.
- Timetable: five columns, a three-plus-two grid, or a narrow list using native 11–14sp text as designed for compact placements.
- A shallow strip keeps all five names/times and separate AM/PM labels; full zone/date remain in its spoken summary when visual metadata cannot fit.
- A compact missing-data state explains unavailable schedules rather than substituting a resize instruction.

The compact candidates retain the selected family's prayer information. Optional illustrations, metadata and tracking controls yield first; configuration is unchanged and the full layout returns at larger bounds. Truly unsupported allocations at extreme text scaling still retain a resize affordance rather than clipping or fabricating times.

Invalid/zero dimension entries are ignored, and legacy zero options use the provider's starting dimensions until a valid callback arrives. Existing Android12+ exact-size RemoteViews mappings, independent zone navigation, tracking keys/actions, schedules, caching and four provider components remain. No new background wakeups or permissions are added. See [Android's widget sizing guidance](https://developer.android.com/develop/ui/views/appwidgets/layouts).

## Compatibility

Personal preferences, bookmarks, counters, reminders, cache data and placed widget IDs are preserved. Backup schema1 is unchanged. Existing 3.2.2 APKs are retained; the original publisher's app and `C:\Tools\waktu-re\` remain untouched. This release reuses the original 3.2.2 generated imagery and adds only original native vectors/layouts/animations; no new image generation was needed.
