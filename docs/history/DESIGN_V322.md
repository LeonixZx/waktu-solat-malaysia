# Android 3.2.2 — mosque backdrops and generated identity

This update adds original mosque artwork to all four Android prayer-widget families and replaces the Android launcher identity with a generated gold arch, crescent and clock emblem. Android remains native; the PWA stays at 3.1.0.

## Widget artwork

- Original wide architectural illustration generated with the **built-in imagegen tool**, with a transparent background. No publisher artwork was supplied or copied.
- A 600×400 PNG derivative, approximately 194 KB, is shared by the four families. The original 1536×1024 PNG is retained in `design/brand-v322/mosque-original.png`.
- Native `ImageView` backdrop at **10% opacity**, fitted to the lower-right area behind native reading text. The artwork has a 12dp inner margin and remains inside the rounded surface in software previews and launcher rendering.
- A content-sized native `RelativeLayout` sizes the artwork from the content's bounds. Bitmap dimensions cannot expand the widget or change candidate selection.
- The backdrop has no accessibility focus or click action. Native prayer names, absolute times, action targets and full spoken summaries remain unchanged.
- Ivory metadata uses `#62574D` and primary time ink uses `#7A491A` to preserve contrast over the image. Noir keeps its existing warm text and gold. Selected cells retain their opaque tonal highlight.
- Native-rendered picker previews are refreshed to match, with labelled example data. Both appearance masters are kept in `design/widgets-v322/`; shipping preview derivatives are at most 250 KB each.

## Android logo

- Original **built-in imagegen** generation followed by one imagegen cleanup edit, using only our own generated draft as its input. Gold emblem on an espresso `#2B2119` launcher field; no lettering or green fill.
- The full-resolution transparent original and draft remain in `design/brand-v322/`. Exact prompts, image roles and tool mode are recorded in `design/brand-v322/prompts.json`.
- `brand_mark_v322.png` is a 512px transparent derivative used by native app branding and decorative widget marks. No religious text is generated.
- `brand_launcher_foreground_v322.png` includes platform safety padding for the expanded Android adaptive foreground layer. Padding is exported into the derivative rather than expressed as a fixed dp inset that changes with output size.
- Normal and round launcher references use the new `ic_launcher_v322` resource name. Android 26+ adaptive and Android 33+ monochrome variants share the same silhouette. Legacy density-specific PNGs cover Android 24–25.
- Themed icons can be recoloured by Android; the ordinary icon uses the specified espresso/gold colours. The original publisher's installed app and launcher settings are left alone.

## Scope and retained behavior

Package `com.example.waktusolatmalaysia`, version **3.2.2 / code 10**. JDK21, minSdk24, compile/target34 and Build Tools34.0.0 remain. All four provider components and existing widget configuration schema are retained; no new permission, dependency or background update mechanism is added.

Prayer tracking, independent zones, configuration drafts, unavailable-data states, per-zone work, caches and backup schema1 remain as in 3.2.1. Previous APKs, design masters and verification records are preserved. `C:\Tools\waktu-re\` remains read-only and was not used for asset generation.
