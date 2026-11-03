# Project tools

- `refresh-prayer-zones.ps1`: refresh the public official zone catalog.
- `prepare_fonts.py`: build the renamed WSM Sans OFL derivatives.
- `package_v3.py`: package verified APK/PWA outputs and regenerate the verification report/checksums. It refuses to package failing checks and never includes personal snapshots.
- `verify_android_v3.py`: emulator-only widget host and 15 width/text-size configurations; restores size, density and font scale.
- `verify_navigation.py`: focused 15-configuration navigation matrix plus a short landscape window; restores emulator settings. `--resume` reuses successful case logs when retrying a failed case.
- `verify_tablet_navigation.py`: fresh personal-data snapshots, reinstall, actual rail/sidebar taps and in-app screenshots in portrait/landscape; restores tablet rotation settings and preserves the current counter value.
- `verify_android_system_v3.py`, `verify_offline_v3.py`, `verify_reboot_v3.py`: emulator-only notification/Doze, download, offline and reboot verification. Reboot fixtures are restored afterward.
- `verify_tablet_v3.py`: compare the owner's upgrade snapshots and run the debug-only in-app navigation/frame probe on the explicitly named tablet.
- `measure_tablet_v3.py`: three cold debug launches and memory capture. Scrolling is measured inside the probe because shell touch injection is inconsistent on MIUI.

The reusable brand export is `web/tools/brand.mjs`. One-time implementation generators were archived under `app/build/implementation-scripts/`; they are not part of the build and must not be rerun over the finished source. `premium_resources.py` records the earlier v2 resource preparation.

The tablet probe is included only in the debug variant:

```powershell
adb -s DEVICE_SERIAL shell am instrument -w com.example.waktusolatmalaysia/com.example.waktusolatmalaysia.debug.TabletProbe
```

Full AndroidX tests use the separate test APK. See `VERIFICATION_V3.md` for results and pending release review. All device tools require an explicit serial; no script writes to the reference workspace.
