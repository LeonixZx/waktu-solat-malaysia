# Restore this project from GitHub

This repository contains the native Android source, PWA source, shared data, Gradle wrapper,
dependency lockfile, tests, tooling, original design assets and licence/provenance records.
The decoded reference APK/workspace is not needed to build it.

## Android

1. Clone `https://github.com/LeonixZx/waktu-solat-malaysia.git` or download its source ZIP.
2. Install **JDK21** and Android SDK **platform34**, **Build Tools34.0.0** and platform-tools.
   Android Studio's SDK Manager can install these components.
3. Set `JAVA_HOME` to your JDK21. Configure `ANDROID_HOME`, or create `local.properties`
   containing `sdk.dir=YOUR_SDK_DIRECTORY` using forward slashes. See `local.properties.example`.
4. Run the included wrapper; a system Gradle installation is unnecessary:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

On macOS/Linux use `./gradlew assembleDebug testDebugUnitTest lintDebug`.
The first build needs internet to download Gradle and Maven dependencies.
The APK is `app/build/outputs/apk/debug/app-debug.apk`.
Open the repository root in Android Studio to continue editing normally.

Application ID: `com.example.waktusolatmalaysia`; minSdk24, compile/targetSdk34.
An unsigned optimized APK can be built with `assembleRelease`; public production distribution
needs a private production signing configuration. Signing keys are intentionally not in GitHub.

## Existing installations and private data

GitHub backs up development source, not a phone's bookmarks/settings/tracking. Export those
from the app's Backup screen separately. No accounts or exported personal backups are committed.

Android updates require a compatible signing certificate. A new computer generates a different
debug key unless you restore your existing private debug keystore. On the original Windows PC
that key normally lives at `%USERPROFILE%\.android\debug.keystore`, outside this project.
Keep a separate secure backup before changing computers. Never upload keystores/passwords to
this public repository. Deleting this project folder does not itself delete that profile key.
Do not uninstall or clear the existing app to work around a signature mismatch without first
exporting personal data. Source recovery cannot reconstruct a lost private signing key.

## PWA

Install Node.js24 LTS (or a compatible newer runtime), then:

```powershell
cd web
npm ci
npm test
npm run build
npm run worker:dev -- --port 8787
```

Open `http://127.0.0.1:8787`. The app can be installed through a supporting browser.
Cloudflare production account/domain configuration is separate; no production credentials are
stored here. `web/package-lock.json` is included for reproducible dependency installation.

## Versions, recovery and downloads

- Tags identify saved source versions. `git checkout v3.3.3` restores the previous widget release.
- GitHub Releases carry versioned APKs, source/recovery archives and SHA-256 checksums.
- Debug APKs are development/test downloads. Unsigned APKs cannot be installed before signing.
- The preserved v3.3.0 recovery source/installers remain separate assets; see `RESTORE_V330.md`.
- Generated caches, `node_modules`, `local.properties`, private test snapshots and old APK input
  files are excluded. They are not required to edit/build the project.
- Historical generator scripts are references. Do not rerun them over the finished app.

Before deleting a local copy, verify the latest commit/tag exists remotely and keep the private
signing keys and any app-data exports backed up separately. See `GITHUB_BACKUP_VERIFICATION.md`
for the independent clone/build verification performed for the initial public backup.
