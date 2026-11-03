# Android 3.3.3 — paired calendar dates

Today's prayers in **Timeline** now shows Gregorian and Islamic dates together at the compact phone allocation of 277.57007×137.90343dp. Two native metadata lines read, for example, `3 Nov 2026` and **`23 Jamadil Awal 1448`**. The month is written in full, including on compact layouts; numeric Hijri notation is not used. The five-prayer curve, next-prayer footer, icons and mosque stay intact. Tighter internal gaps and a wider date column accommodate the extra line without reducing prayer type sizes.

**Daily Plus**, in both Timeline and Classic, also uses the named Islamic month and respects its existing Hijri visibility option. Candidate fitting never substitutes a numeric Hijri month. The narrow strip and Classic daily layouts retain their current information hierarchy; the update does not force every family to carry both dates.

All four families use readable location names in both styles. Compact metadata uses a smaller native label and, only for longer names, familiar place abbreviations such as `K. Terengganu`. A multi-district zone displays its primary locality; the full area remains available to accessibility. Zone codes stay internal for schedule lookup and navigation and remain available in the official zone picker. Widget headers, footers and My widgets summaries no longer display them.

`WidgetDates` formats the Hijri string supplied by the JAKIM timetable row. It performs no civil-to-Islamic conversion and never estimates missing data. Paired dates follow the displayed official daily row in Malaysia time, including after the final prayer; they refresh at Malaysia midnight rather than mixing tomorrow's Hijri date with today's Gregorian date. Invalid or missing Hijri values are omitted. Full date accessibility descriptions label Gregorian/Masihi and Hijri (JAKIM).

Android version **3.3.3/code14** retains the application ID, SDK/JDK, four providers, placed-widget configurations, styles, local data and update architecture. No new artwork, permissions or background wakeups are added. PWA3.1.0 is unchanged. The original v3.3.0 recovery checkpoint and all previous installers remain available.

Static launcher illustrations are refreshed from the native renderer with full Hijri month names and location names. Eight source captures and labelled fixture metadata are in `design/widgets-v333/`; four shipping derivatives stay under 250KB. `tools/widget_previews_v333.py` exports only these assets and does not generate app code or write timetable caches.
