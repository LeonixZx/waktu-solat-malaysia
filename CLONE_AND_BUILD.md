# Clone, build and keep your work

This repository is a complete editable Android/PWA project. It does not need the owner's computer, decoded APK, private API keys or a system Gradle installation.

## Get the source

```sh
git clone https://github.com/LeonixZx/waktu-solat-malaysia.git
cd waktu-solat-malaysia
```

Or choose **Code → Download ZIP** on GitHub, extract it, and open that folder. Fork first if you want to publish your own changes. Release tags are saved app versions; `main` also includes ongoing documentation/tooling improvements.

## Android prerequisites

| Tool | Required version |
| --- | --- |
| JDK | **21** (Gradle runs on 21; Java bytecode targets 17) |
| Android SDK Platform | **34** |
| Android SDK Build Tools | **34.0.0** |
| Android SDK Platform Tools | Installed for ADB/device work |
| Gradle | Included **8.9 wrapper**, checksum verified |
| Android Gradle Plugin | **8.5.2**, declared in the root build |

Minimum Android is **7.0 / API 24**; compile/target SDK remain 34. The application ID is `com.example.waktusolatmalaysia`.

### Android Studio

1. Install Android Studio and JDK 21. In **SDK Manager**, install Platform 34, Build Tools 34.0.0 and platform-tools. Enable **Show Package Details** to choose that Build Tools version.
2. Open the cloned **root folder**, containing `settings.gradle`.
3. Under **Settings → Build, Execution, Deployment → Build Tools → Gradle**, select your JDK 21 as the **Gradle JDK**, and sync.
4. Select the `app` module and debug variant. Use **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or run on your selected emulator/device.

### Command line

Set `JAVA_HOME` to your installed JDK 21. Set `ANDROID_HOME` to your SDK directory, or copy [local.properties.example](local.properties.example) to `local.properties` and enter your SDK path using forward slashes. Those machine-specific files stay ignored.

If you use SDK command-line tools, install the components with:

```sh
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
sdkmanager --licenses
```

Windows PowerShell, from the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

macOS/Linux:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The first build downloads Gradle and Maven dependencies. The APK is **`app/build/outputs/apk/debug/app-debug.apk`**. Debug signing happens locally without secrets from this repository. Copy it to your phone, or install with `adb -s YOUR_DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk` after authorizing USB debugging. Always select your intended device; do not clear app data.

Optional diagnosis (Python 3.10+): `python tools/doctor.py`. It reports missing components without dumping environment variables, profile paths or device serials.

### Build through GitHub without installing an SDK

1. Fork this repository into your GitHub account.
2. Open **Actions**, enable workflows in your fork if prompted, and select **Build and verify**.
3. Choose **Run workflow**, then the branch you want to build.
4. When all checks pass, open the run and download **android-debug** from **Artifacts**. Extract the ZIP and install the APK. `SHA256SUMS.txt` is included.

Artifacts require GitHub sign-in and expire after 14 days. Public versioned APK downloads remain in Releases. A fresh CI debug key differs between runners; use your own securely stored signing key for a consistent distribution/update channel. No signing secret is needed for the default workflow.

## Signing and existing installations

For an optimized **unsigned** release APK, run `:app:assembleRelease`; its output is `app/build/outputs/apk/release/app-release-unsigned.apk`. It cannot install before signing. Use Android Studio's **Generate Signed Bundle / APK** with **your own private keystore** for a signed distribution build. Never commit the key, credentials or key configuration.

Android upgrades require the same application ID and compatible signing certificate. An APK built with a new debug key may report a signature conflict with the downloaded development APK. Export personal data first and keep the old installation intact; do not uninstall/clear it as a build troubleshooting step. For a separate fork installation, change `applicationId` in your fork and accept that it has separate app data and widget instances.

To continue updating existing installations after moving computers, securely back up the compatible private signing key. The original debug key is normally at `%USERPROFILE%\.android\debug.keystore` on Windows or `~/.android/debug.keystore` on macOS/Linux, outside this project. Source recovery cannot reconstruct a lost key. Keep phone-data exports separately through the app's Backup screen.

## Web app

Use Node.js 24 and the committed dependency lockfile:

```sh
cd web
npm ci
npm test
npm run build
npm run worker:dev -- --port 8787
```

Open **http://127.0.0.1:8787**. This serves the built app and allowlisted API adapter. For Vite hot reload, keep the Worker running and use `npm run dev` in a second terminal. Browser end-to-end tests use `npm run test:e2e` with the built Worker running; install Playwright browsers with `npx playwright install chromium` first.

For your own deployment, review `web/wrangler.jsonc`, authenticate to **your** Cloudflare account, configure your domain, then run `npm run deploy`. No live production account or credentials are included. Never place secret credentials in `VITE_*` variables: those become public client code.

## Troubleshooting

| Problem | Fix |
| --- | --- |
| SDK location not found | Set `ANDROID_HOME`, or create `local.properties` as above. |
| JDK/toolchain error | Check `java -version` and Gradle JDK; this project requires JDK 21. |
| Platform/Build Tools missing | Install Platform 34 and Build Tools 34.0.0, then accept SDK licences locally. |
| `./gradlew` permission denied | Run `chmod +x gradlew` on macOS/Linux. |
| Dependency download fails | Check internet/proxy access to Google Maven, Maven Central and Gradle; the initial build is online. |
| APK won't install | Use a signed debug APK, check Android 7+, and inspect signature conflicts before changing existing data. |
| Notification/azan silent | Follow [the installation and alerts guide](docs/INSTALL.md#notifications-and-azan). |

## Recovery and maintenance

Generated build folders, caches, dependencies, keys and phone snapshots are intentionally excluded. Original artwork, prompts, tests and licence records remain editable. Historical one-time source generators have been removed from the active tree; building uses the finished source and wrapper.

See [RESTORE_V330.md](RESTORE_V330.md) for the preserved baseline, [historical verification](docs/history/) for evidence, [EMULATOR_TESTING.md](EMULATOR_TESTING.md) for opt-in device tests, and [CONTRIBUTING.md](CONTRIBUTING.md) before publishing changes. Before deleting a local project, verify your latest remote commit and separately back up keys and personal data.

Build reference: [Android command-line build documentation](https://developer.android.com/build/building-cmdline). Workflow downloads: [GitHub artifact documentation](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).
