# Ibadah and Kiblat · v3.1.0

The Android application and PWA now organize Ibadah into Bacaan, Amalan, Panduan and home-screen widgets. The original gold crescent-and-arch identity remains; the installed development application ID stays `com.example.waktusolatmalaysia`. No original-app data or files under `C:\Tools\waktu-re\` are modified.

## Collections

`LibraryFragment` measures the actual navigation content area. Two panes require 680 dp of width, 480 dp of height and font scale no greater than 1.3. Outer margins are 20 dp, the gap is 24 dp and the list is 240–320 dp. Reading content is bounded to 680 dp. Narrow/short/large-text windows open a separate inline reader; Back reveals the existing list. Quran uses the same adaptive foundation.

Categories stay above both panes. Search, the explicit saved filter and result/retry/empty states remain with the list. Selection, category-specific filters, list positions and the current reader survive recreation. Collection rows explicitly fill the list width so weighted titles cannot collapse beside their icons. The PWA uses ResizeObserver, session-restored selection/filters and separate per-reading scroll positions.

`Reading.Section` and its TypeScript equivalent preserve Arabic, supplied Rumi, local and English translations, and actual translation language. Existing `kind:index` IDs and legacy navigation arguments remain supported. No backup migration is needed for saved IDs. The 36 entries comprise five doa, fourteen wirid, five niat and twelve hadith. Wirid's source `arabic_translation` field is displayed as Rumi, separately from `malay_translation`. Missing English is stated explicitly; Malay and Indonesian are labelled. No religious text or translation is generated.

## Kiblat

`QiblaMath` calculates a great-circle bearing clockwise from true north and rejects invalid coordinates or an undefined bearing at the Kaaba. `QiblaOrientation` remaps Android sensor coordinates for all four display rotations, including landscape-natural devices. `QiblaState` keeps bearing, measured heading, declination, sensor source, freshness, accuracy, uncertainty and tilt separate.

The sensor order is rotation vector, geomagnetic rotation vector, then accelerometer/magnetometer. Game rotation vector is excluded. Magnetic declination is applied once, after circular heading smoothing with a 150 ms time constant. Updating location preserves the last measured heading. A resumed screen reacquires heading rather than trusting an old sample.

Acquiring location/heading, unavailable, stale, low-confidence and ready states have distinct guidance. Alignment is approximate within ±5° and only appears with location age ≤120 seconds, heading age ≤2 seconds, medium/high sensor accuracy, tilt ≤25° and reported uncertainty ≤10° when supplied. The interface shows true bearing, location accuracy/age and sensor diagnostics, explains calibration and magnetic-case/metal interference, and offers a user-opened eFalak link.

The PWA retains browser orientation as a diagnostic. It does not label an unverified magnetic/browser heading as true-north alignment; its bearing diagram stays north-up when a reliable reference is unavailable.

Implementation references: [Android sensor coordinates](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview), [Jabatan Mufti Selangor's explanation and eFalak reference](https://www.muftiselangor.gov.my/2025/11/11/17-taudhih-al-falak-ketepatan-aplikasi-telefon-bimbit-dalam-menentukan-arah-kiblat/).

## Other repairs

- Tasbih validates custom goals from 1–10,000 without dismissing an invalid form. Long counts fit the dial. Undo, reset confirmation and tracking remain local.
- Reminder forms use stable IDs, wrap offset controls in narrow/large-text windows and keep validation visible. Denied notification permission leaves a new definition disabled. Existing planner generation and anchor-day semantics are retained.
- Calendar labels distinguish official, approximate and partially available Hijri dates. Missing schedules are never invented; export remains UTC iCalendar.
- Widget previews are bounded and large-text widgets use a compact next-prayer layout. Headers grow instead of clipping the zone. Independent widget zones do not change the app's default zone.
- The adaptive launcher background is a constant ivory, independent of the app's night theme. Foreground art is larger within the safe bounds; normal, round and themed resources retain the gold arch/crescent identity. Android may recolour a themed monochrome icon according to system settings.

See [VERIFICATION_V31.md](VERIFICATION_V31.md) for direct captures, reproducible checks and physical validation still requiring a trusted direction reference.
