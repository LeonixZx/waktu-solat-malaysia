# Working baseline

- Current release: Android **3.3.4/code 15**, repairing azan/notification delivery and adding our branded notification icon. See `VERIFICATION_V334.md`. Preserve saved modes, widgets and personal data.
- The preserved Android baseline is **v3.3.0, version code 11**, package `com.example.waktusolatmalaysia`.
- Read `RESTORE_V330.md` before changing the restored project. Its source checkpoint and manifest allow recovery without relying on an APK decompilation.
- Preserve the existing installers in `deliverables/`; publish future builds under new versioned names.
- Keep JDK 21, minSdk 24, compile/targetSdk 34, Build Tools 34.0.0 and the Gradle wrapper unless the user requests a change.
- Historical generators under `tools/` and `app/build/implementation-scripts/` are reference material. Do not rerun them over finished source as a build or restore step.
- Preserve bookmarks, counters, reminder definitions, prayer caches and placed widget configurations. Do not uninstall or clear an installed app to perform an upgrade or test.
- Keep `C:\Tools\waktu-re\` strictly read-only.
- Emulator profiles are test infrastructure. Distinguish emulator System UI failures from app defects and use a physical reference for compass accuracy.
- Public source: `https://github.com/LeonixZx/waktu-solat-malaysia`. See `CLONE_AND_BUILD.md`. Never commit signing keys, tokens, private device snapshots, build caches or the decoded/reference APK.
