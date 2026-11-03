# Android 3.2.1 — reference-led widget refinement

The installed Tonton Waktu Solat Malaysia widget family was inspected read-only. Its compact timetable, gold strip, square and wide cards use tighter information density and recognisable prayer icons. The original app, its preferences and `C:\Tools\waktu-re\` were not modified. No reference artwork or publisher preview was shipped in our app.

The main defects in 3.2.0 were visible in the Xiaomi picker and launcher: the picker fell back to the app logo, and coloured surfaces stretched across oversized launcher cells while content remained at the top. This update addresses both.

| Family | New presentation |
|---|---|
| Next prayer | Compact next-prayer card at small bounds; a fuller square with five supporting times when space permits. Supporting times follow the displayed date, including validated tomorrow data |
| Prayer strip | A genuinely shallow five-prayer timetable with an explicit next-prayer cue. Compact 12sp names / 16sp times retain native text scaling. In 12-hour mode, a separate 10sp AM/PM line prevents squeezed or clipped times |
| Today’s prayers | Five icon-led columns with a clear next-prayer highlight and absolute next time; grouped/narrow alternatives remain available |
| Daily Plus | Three-plus-two grid in square allocations; five columns in wide allocations. Optional auxiliary times and five 48dp tracking controls fit into columns when possible; vertical fallback remains available |

All four keep the existing component names, instance IDs and configuration schema. The strip retains next-prayer information while adding today’s full five-time timetable. Its surface opens the date it displays without changing the app’s default zone.

## Visual and layout changes

- Visible surfaces wrap their content inside a transparent launcher allocation. Extra launcher height exposes wallpaper instead of a large empty coloured block.
- Warm Ivory and espresso/gold Noir gradients, restrained gold outlines, native sun/cloud/moon vectors, existing typefaces and original crescent/arch branding.
- Native labels and times remain real text; vectors supply decorative prayer icons. The app does not draw live reading text into bitmaps.
- Real picker images are exported from the native renderer using labelled `Contoh` sample data. They are launcher marketing previews, not live schedules. The four source images and both appearance masters are retained in `design/widgets-v321/`.
- Static `previewImage` metadata provides a compatible fallback across launchers. Runtime-only layouts with empty default values are no longer used as picker previews. [Android widget preview guidance](https://developer.android.com/develop/ui/views/appwidgets/previews)
- The compact strip’s minimum-height hint is 70dp. Xiaomi now advertises it as 2×1 in its picker. Its default two-cell bounds were checked explicitly at 280, 300 and 320 logical units in Malay/English and both time formats. Requested spans remain launcher-dependent.
- Layout choice still measures actual platform text and available dimensions. Native text is never shrunk to bypass accessibility scaling; unsupported allocations show a resize cue. Decorative icons yield after functional tracking layouts have been considered.
- Fixed one-line text uses `maxLines=1` rather than horizontal scrolling, which also makes centred text render correctly in offscreen preview capture.

## Compatibility and scope

Android application ID remains `com.example.waktusolatmalaysia`; version **3.2.1 / code 9**. JDK21, minSdk24, compile/target34 and Build Tools34.0.0 are unchanged. The PWA remains 3.1.0. No new runtime permission or dependency was added.

Tracking continues to use the existing canonical-to-Malay legacy keys and idempotent intended-state actions. The 48dp controls, stale-date guard, configuration drafts, cached schedules, per-zone work and backup schema1 remain. Existing v3.2.0 artifacts are retained.

The reference app is inspiration for proportions and density. These are original layouts, glyphs and backgrounds with our own branding. Native preview assets are labelled examples; installed widgets use validated cached official schedules.
