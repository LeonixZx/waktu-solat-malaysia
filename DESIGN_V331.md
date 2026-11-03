# Android 3.3.1 — optional Timeline widgets

This widget-only update adds an original interpretation of the compact bands and curved five-prayer layouts observed in the decoded reference. The reference was read-only; its images, fonts, XML and smali were not copied into the app. Our gold mark, Jakarta fonts, existing generated mosque artwork and licensed source records remain in use.

## Presentation

- **Next prayer:** a five-time overview with a clear next-prayer footer; a curve on larger placements and a compact 3+2 grid at smaller widths.
- **Prayer strip:** six entries including Syuruk, thin column dividers, location/date metadata and next-prayer emphasis. A denser candidate fits shallow launcher allocations.
- **Today's prayers:** a low rectangle with five native labels/times and circular glyphs arranged along a decorative arc.
- **Daily Plus:** a larger timeline, optional Hijri/auxiliary times and optional tracking. Its five tracking targets remain 48dp and align in a separate row.

Ivory and Noir retain the app's identity, a subtle 10% mosque background and rounded surfaces. The arc, rings, border and dividers are original vector/shape resources. All names, dates and times remain native text. The widget shows absolute start times and adds no countdown refreshes, network calls or permissions.

Candidate selection uses measured text at the supplied window bounds. Tracking takes priority over optional metadata. When the timeline cannot fit, the verified Classic candidates retain the same widget family's information. Reading order and content descriptions retain the full location/date even when visible metadata is shortened.

## Upgrade and configuration

All four provider components, instance IDs, zones, appearances, stored prayer records and update mechanisms remain stable. Existing configurations without `layoutStyle` resolve to **Classic**. A new empty configuration defaults to **Timeline**. The optional field uses the existing V2 configuration and preserves unknown fields.

In **Ibadah → Widgets → My widgets → Settings**, choose **Timeline / Garis masa** to change an existing widget. Choose **Classic / Klasik** to return to the v3.3.0 presentation. Changes remain drafts until Save; cancellation and recreation preserve their existing behavior. No reinstall or app-data reset is required to compare designs.

The main app navigation, readers, prayer calculations, Kiblat, content catalog and PWA are unchanged. Android version is 3.3.1/code 12; application ID and SDK/JDK requirements remain unchanged. Original v3.3.0 installers and the verified source archive remain available.

## Preview accuracy and validation

Launcher preview derivatives come from the native renderer at each provider's preferred dimensions with tracking off, matching the new-widget default. Separate captures show tracking enabled and larger square placements. Fixed timetable data is confined to labelled tests and static launcher previews, and never enters the production timetable cache.

See `VERIFICATION_V331.md` for the completed checks, artifact hashes, environment distinctions and remaining manual checks.
