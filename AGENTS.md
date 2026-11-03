# Working baseline

- Current release: Android **3.3.3/code 14**, adding paired Gregorian/Hijri widget dates. See `DESIGN_V333.md` and `VERIFICATION_V333.md`. Preserve saved Classic/Timeline choices and Hijri preferences; do not reset placed instances.
- The preserved Android baseline is **v3.3.0, version code 11**, package `com.example.waktusolatmalaysia`.
- Read `RESTORE_V330.md` before changing the restored project. Its source checkpoint and manifest allow recovery without relying on an APK decompilation.
- Preserve the existing installers in `deliverables/`; publish future builds under new versioned names.
- Keep JDK 21, minSdk 24, compile/targetSdk 34, Build Tools 34.0.0 and the Gradle wrapper unless the user requests a change.
- Historical generators under `tools/` and `app/build/implementation-scripts/` are reference material. Do not rerun them over finished source as a build or restore step.
- Preserve bookmarks, counters, reminder definitions, prayer caches and placed widget configurations. Do not uninstall or clear an installed app to perform an upgrade or test.
- Keep `C:\Tools\waktu-re\` strictly read-only.
- Emulator profiles are test infrastructure. Distinguish emulator System UI failures from app defects and use a physical reference for compass accuracy.
