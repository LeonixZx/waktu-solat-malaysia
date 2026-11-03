# Android 3.3.2 — compact widget layout and accurate previews

The connected Xiaomi/POCO phone allocates **277.57007×137.90343dp** to Today's prayers. Version 3.3.1 required 280dp before attempting the curved row, so it selected a compact band. Meanwhile My widgets rendered a 360×180dp example and configuration could render at its preview-panel width. These different inputs explained the mismatch.

## Layout

- Try measured curved-row candidates without the old 280dp gate. The new compact arc uses 8dp padding, 10sp prayer names, 13sp times, 24dp icons and a 16dp brand mark at normal text scaling.
- Keep the five-prayer curve, next-prayer footer, rounded gold border and subtle mosque. Omit duplicate brand/Hijri/footer metadata before losing prayer information. Shorten location/date when required.
- A shallow six-time micro-band extends the strip's fit range. Measured fit remains active for accessibility text and translated labels; no blanket font-scale conversion is added.
- Native text remains scalable. Prayer names and times are not rasterized or resized as artwork. Larger placements retain their more spacious candidate.

## Preview contract

`PrayerWidgets.presentationSize` selects a valid paired size reported by the host on Android12+, using compatible orientation bounds on older Android. My widgets and configuration use that size and saved instance style.

`WidgetPreviewView` first measures/layouts the RemoteViews result at the host's dimensions, then scales the finished non-interactive preview to fit the app panel. A narrower preview panel cannot select a different widget design. The preview is decorative; surrounding labels/actions provide accessibility.

Add widgets offers Compact/Spacious examples and explains that the launcher determines actual size. Static Android picker artwork remains an illustrative preferred-size image; it cannot predict every launcher's cell grid. My widgets shows the installed instance's shape.

## Compatibility

Android **3.3.2/code13**, application ID `com.example.waktusolatmalaysia`. Provider components, configuration fields, styles, zones, update jobs and tracking keys retain their contracts. No data reset is required. Classic remains available. Main app features, religious text, prayer calculations, PWA3.1.0, SDK/JDK and original imagery remain unchanged.

See `VERIFICATION_V332.md` for actual phone observations, native tests, preservation evidence and limits.
