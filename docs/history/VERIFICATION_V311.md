# Android 3.1.1 · location refresh and physical follow-up

The physical Kiblat session exposed a reproducible refresh bug: accepting a usable cached location cancelled the live location subscription. Returning to a still-fresh screen also failed to resubscribe. After the initial timeout, a later fix required another button press. Android 3.1.1 fixes these paths and is installed as an in-place upgrade (application ID unchanged, version code 7).

Kiblat now keeps LocationManager updates active while visible, restores them on every resume, and removes them on pause. Cached data can display immediately without cancelling live updates. After a 45-second initial wait, the interface explains that updates continue; a later fix updates the screen automatically. The existing two-minute location-age gate, heading freshness, calibration guidance, bearing mathematics and heading offsets are unchanged.

## Verification

| Check | Result |
|---|---|
| Reproducer against delivered 3.1.0 | Fails to receive the new bearing after displaying a cached fix; the expected failure is recorded |
| Emulator LocationManager regression | Three passes: cached → multiple live fixes; pause/resume with a fresh previous fix; new fix after the 45-second initial wait, without another tap |
| Mock-location isolation | Test providers and mock-location AppOp used only on emulator-5554, then removed/restored; never used on the tablet |
| Native builds | Debug, optimized unsigned release, 41 JVM tests and lint with zero errors; warnings remain |
| Tablet regression | 31 passing functional/navigation/reader/widget/branding tests; portrait and landscape layout checks and direct captures |
| Large text | Targeted 320 dp / 200% text layout check passes; broader unchanged-layout baseline is the 21-configuration v3.1.0 matrix |
| Upgrade data | Fresh snapshots from before this patch retain app/personal preferences, counters, bookmarks, widget settings, reminder definitions and recovery state; internal scheduler generations excluded |
| Permissions/settings | The temporary precise-location grant was revoked. Original approximate-only permission and flags match; temporary portrait lock and subsequent test rotation settings restored |
| Web | Unchanged PWA 3.1.0; its 17 unit tests, 15 browser scenarios and packaged-Worker smoke check remain the passing baseline |

Evidence is under `app/build/verification/v311/`: `native-build.log`, `location-baseline-failure.log`, `location-updates.log`, `location-restored.json`, `tablet-suite.log`, `tablet-portrait.log`, `tablet-landscape.log`, `android-320-text200.log`, `data-retention.json` and `display-restored.json`. `tools/verify_qibla_location_v311.py` is explicitly emulator-only; `tools/verify_tablet_v311.py` uses fresh current-data snapshots.

## Physical observations — no accuracy pass claimed

The user reports an independently verified Kiblat marker is available and that completely removing the case appeared to improve the direction. The case was confirmed fully removed, rather than merely opened.

One portrait series recorded bearing 291.1°, heading 233.3°, tilt 3° and fresh location ages 3–17 seconds (network provider, reported accuracy ±100 m). The user reported the tablet top faced the marker, but uncertainty about the physical-edge instructions remained. This series does **not** establish a passing physical comparison. Earlier captures made before the edge explanation are excluded from physical-error conclusions.

A separate sensor diagnostic found magnetic-heading means of 252.14° from rotation vector and 252.22° from geomagnetic rotation vector, agreeing within 0.09°. The accelerometer/magnetometer path reported a different mean and a minimum accuracy status of unreliable during the sample. That minimum may include startup samples; it does not diagnose a hardware fault. Agreement between fused sensor paths is not a verification against true north.

After the user reported recalibrating, the next capture had location age 155 seconds. The app correctly showed stale data and suppressed alignment. This measurement is excluded from an accuracy pass and led to the live-update repair above. No arbitrary heading offset was introduced to make the marker comparison appear to pass.

A complete, controlled portrait/landscape and case-on/case-off reference comparison was not obtained. **Physical Kiblat accuracy remains unverified.** The patch fixes location delivery; it does not certify the tablet's compass or the reference setup. Use the established prayer-direction marker as the reference while this remains unresolved. The [Jabatan Mufti Selangor explanation](https://www.muftiselangor.gov.my/2025/11/11/17-taudhih-al-falak-ketepatan-aplikasi-telefon-bimbit-dalam-menentukan-arah-kiblat/) discusses compass interference and the official eFalak alternative.

Raw observations, screenshots and the sensor-path diagnostic are in `app/build/verification/v31/physical-qibla-*`. No latitude/longitude was saved in those diagnostic reports. Exact original/restored permissions and rotation settings are recorded alongside them.

## Deliverables

`deliverables/Waktu-Solat-Malaysia-v3.1.1-debug.apk` is the installed development build; the matching release APK is unsigned and requires publisher signing. The PWA package stays `Waktu-Solat-Malaysia-v3.1.0-PWA.zip`. SHA-256 records are in `deliverables/SHA256SUMS.json`. The earlier artifacts are retained. Manual TalkBack/hardware-keyboard checks and production signing/hosting remain release work, as documented in the initial verification report. `C:\Tools\waktu-re\` was not modified.
