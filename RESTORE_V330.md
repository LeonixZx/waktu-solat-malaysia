# Verified Android v3.3.0 baseline — 3 November 2026

The preserved working installer is `deliverables/Waktu-Solat-Malaysia-v3.3.0-debug.apk`, package `com.example.waktusolatmalaysia`, version 3.3.0/code 11.

SHA-256: `6378c1691e8874745e88b46cd35ad209083039f5ff0648c6a41cb9993a7f8e31`.

## Findings and restoration

- All 26 APK/ZIP artifacts still match the existing delivery checksum record. The working installer and delivery archive were not deleted or overwritten. The debug APK's signature verifies.
- A recent rollback had already removed the experimental widget resources and restored the production renderer. Recent edits remained in `PrayerWidgets.java` and its appearance test; modification dates alone did not establish whether the rollback was correct.
- A fresh build in a separate output folder compiled the source without using the old app build outputs or the Gradle build cache. Every packaged non-DEX entry matches the working APK, including layouts, icons, artwork, fonts, assets, manifest and resource table. All differing DEX files have identical disassembly. Their binary differences are compiler provenance/hash metadata, rather than different executable instructions.
- Consequently, no further production-source rollback was necessary. The normal `app/build/outputs/apk/` debug and unsigned release paths were explicitly restored from the preserved v3.3.0 installers. These output copies are byte-for-byte originals; future source rebuilds may have different compiler metadata/checksums.
- The PWA's 53 baseline files match their previous hashes. Its version remains 3.1.0.
- The wrapper, dependency configuration, application ID, SDK levels, JDK 21 configuration and local SDK path retain the working setup. The additional emulator profiles, runner and system-bar diagnostic were added by this chat for the user's authorized emulator testing. They remain test infrastructure and do not change the app UI or runtime.
- Historical resource generators were not rerun. `C:\Tools\waktu-re\` was not modified.

## Verification

The isolated build passed `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintDebug` and `assembleRelease`: 41 unit tests, zero lint errors and the existing 286 lint warnings. The release APK is unsigned; this task does not certify a production signing configuration.

All **18 API34 emulator regression tests passed** against the exact preserved APK, with both native assertions and the post-test System UI health check passing. They cover all four widget families, layout/text-scale cases, tracking actions, migration, launcher previews/branding, compact phone allocations, navigation, filters, settings and light/dark system bars. The fresh-source test APK was used. This is native widget-host evidence, not a new certification of every OEM launcher's behavior.

Logs and direct captures are under `app/build/verification/emulator-matrix/standard_phone/`; a copy of the result and regression log is retained in the recovery directory. The earlier Android16 emulator System UI ANR remains an environment limitation, not a passing compatibility result.

The USB device reported model `2405CRPFDG` and did not have our application package installed. This audit did not install, uninstall, clear data, import old preferences or change that device's settings. It does not claim to have restored app data on that device. The preserved APK can be installed manually.

## Recovery checkpoint

The pre-audit project source is backed up at `.recovery/20261103-062354/current-workspace-source.zip`, with per-file hashes in `before-files.json`. This preserves the state encountered before restoration.

The verified source checkpoint and its checksum are listed in `recovery/V330_BASELINE.json`. It contains source, configuration, documentation, tooling and original design sources; it excludes build caches, dependency installations, reference APKs and private device snapshots.

Run the read-only baseline check before future work:

```powershell
python tools/check_v330_baseline.py
```

For recovery, first preserve any newer work, then extract the verified source ZIP into this project. Preserve `deliverables/` and local app data. Build using the included wrapper and JDK 21. Do not run the historical generators as a restoration step.

The detailed fresh-build log, APK-entry comparisons, DEX disassembly comparisons and signature check are retained under `.recovery/20261103-062354/`.
