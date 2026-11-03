# Android 3.3.1 widget verification — 3 November 2026

The signed debug APK is `com.example.waktusolatmalaysia`, version **3.3.1/code 12**. JDK21, minSdk24, compile/targetSdk34, Build Tools34.0.0 and the Gradle wrapper remain unchanged. PWA3.1.0 is unchanged.

Debug APK SHA-256: `909af7126c00448856461cad0f5c13eedfacea16808e3bd316b78c6f470b72dd`.

## Completed checks

| Check | Result |
|---|---|
| Debug APK, test APK and optimized unsigned release build | Passed |
| JVM unit tests | 41 passed |
| Lint | 0 errors, 542 warnings |
| Android14/API34 standard-phone emulator | 22 native tests passed |
| Android7/API24 compatibility emulator | 4 Timeline tests passed |
| Post-test Android UI health | Passed on both successful runs |
| Debug signature and SDK/version metadata | Verified |
| Existing delivery archives/APKs | All 26 original checksums match |
| PWA baseline | All 53 files unchanged |
| Reference workspace | Inspected layouts/provider XML retain their hashes; no writes performed |
| Baseline production files | No files deleted; changes limited to widgets/configuration, previews and version metadata |

Both successful native runs used the exact APK hash above. API34 reported 1080×2400 at 420dpi. API24's actual display metrics and graphics mode are retained in its result JSON. Static launcher previews were regenerated before these final runs.

The existing lint warning count increases mainly because the dense widget layouts intentionally contain compact metadata text and API/font-fallback resources. This release does not claim to resolve the project's pre-existing lint warnings. Native font-scaling and measured-fit checks remain active; text is not shrunk programmatically to bypass them.

## Coverage and direct visual review

The combined suite includes existing Classic widget regression tests and the four new `WidgetTimelineV331Test` methods. It checks:

- All four providers and both appearances, with Malay/English and 12/24-hour times. The strip includes Syuruk at its preferred allocation.
- Preferred default bounds, compact phone allocations and 100%, 130% and 200% font scaling. Additional Classic layout combinations cover six bounds from 120×72 through 480×700.
- Native text measurement, no ellipsis/height clipping, family-preserving fallbacks and 48dp tracking targets.
- New-widget Timeline defaults, existing-widget Classic defaults and retention of unknown configuration fields, zones and appearances.
- Configuration recreation, Cancel isolation and Save of the selected layout; default app zone remains unchanged.
- Actual mark/unmark taps in a bound `AppWidgetHostView`, plus existing idempotence, stale-date, deletion and legacy tracking-key checks.
- Year rollover, missing tomorrow data and date/zone schedule navigation.
- Launcher branding/previews, main phone navigation, collection/media filters, settings recreation and light/dark system-bar appearance.

Direct captures are native Android view exports. The review gallery includes the four default-size families in Ivory/Noir, a larger next-prayer curve, 12-hour English examples, tracking enabled, and picker previews with tracking off. The overview sheet only arranges those captures; it does not redraw the interface.

Fixture schedules use the labelled test date 3 November 2026. Fixtures are confined to tests and static launcher illustrations and are never written into production timetable caches. Live widgets use the shared validated cache and retain existing update/permission behavior.

## Environment findings and practical limits

The API24 emulator's automatic graphics backend crashed Android's native `android.display`/system process. That attempt is retained as a failure. Rerunning the same APK with `swiftshader_indirect` passed all four checks; the legacy test profile now selects that compatibility launch argument. The earlier development runs and deliberately superseded captures are not counted as final passes.

This release was tested in native widget hosts, rather than by physically placing the new widgets on every OEM launcher. Direct placement/resizing on the owner's other phone, spoken TalkBack traversal, subjective animation smoothness and prolonged reboot/Doze delivery remain manual checks. Very small allocations combined with large accessibility text can use Classic or a resize affordance when essential text cannot fit. Saved layout settings remain intact.

The connected physical device was not installed to, uninstalled, cleared or reconfigured during this widget update. No new physical Kiblat accuracy, location, calendar or religious-content claim is made. The unsigned release builds successfully; runtime verification uses the signed debug APK, and production signing remains a release step.

## Upgrade, recovery and reproduction

Install the debug APK over the existing development app. In **Ibadah → Widgets → My widgets → Settings**, select **Timeline / Garis masa** and Save for each existing widget you want to change. Select **Classic / Klasik** to keep the v3.3.0 layout. New empty configurations default to Timeline, with tracking off.

All provider components, IDs, stored prayer records, bookmarks, counters, reminders and caches retain their existing contracts. No preference/database reset or historical generator rerun was used. The original v3.3.0 installers and `.recovery/20261103-062354/verified-v330-source.zip` remain intact. A separate v3.3.1 source archive and delivery checksums accompany this build.

Reproduce using the wrapper with JDK21:

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease
python tools/android_emulator_matrix.py run --profiles standard_phone --suite full
python tools/android_emulator_matrix.py run --profiles legacy_api24 --suite timeline
python tools/package_v331.py
```

The packager requires both passing native results against the current APK and verifies old artifact/PWA/reference hashes before creating new versioned outputs. Logs, captures and scope checks are under `app/build/verification/v331/` and `app/build/verification/emulator-matrix/`; shipped review files and hashes are under `deliverables/v3.3.1/`.
