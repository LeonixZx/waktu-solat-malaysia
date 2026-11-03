# Install Waktu Solat Malaysia

## Android phone or tablet

1. Open the [v3.3.4 development release](https://github.com/LeonixZx/waktu-solat-malaysia/releases/tag/v3.3.4).
2. Download **Waktu-Solat-Malaysia-v3.3.4-debug.apk**. Choose the debug file for testing; the unsigned release file is for developers.
3. Open the download and allow installation from the browser/file manager if Android asks. You can switch that permission off afterward.
4. Launch the gold crescent-and-arch app, choose your state and prayer zone, and check the timetable.

Android **7.0 or newer** is required. There is no Play Store listing or hosted public web deployment. No app account is required.

### Verify the download

Compare SHA-256 against [the release checksums](https://github.com/LeonixZx/waktu-solat-malaysia/releases/download/v3.3.4/Waktu-Solat-Malaysia-v3.3.4-public-SHA256SUMS.txt). For this exact debug APK:

```text
31742a4184f3caa44c67673ac2474f31e58799410c26d20655d20a0f03f60b16
```

Windows: `Get-FileHash ./Waktu-Solat-Malaysia-v3.3.4-debug.apk -Algorithm SHA256`.

## Notifications and azan

In the timetable, open a prayer's reminder control and choose **Notification** or **Azan**. Allow notifications on Android 13+, and allow **Alarms & reminders** in Android's app settings when offered for timely delivery. Without exact-alarm permission, delivery can be approximate.

Check notification channels, Android alarm volume, and device battery/background restrictions. The schedule screen includes notification and five-second azan tests plus permission/channel status. Force-stopping an app prevents scheduled work until it is opened again; OEM battery restrictions can also affect delivery.

## Home-screen widgets

Long-press an empty launcher area → **Widgets** → the app's gold-logo entry. Choose next prayer, the strip, today's prayers or Daily Plus. Configure its zone, style and appearance, then save. **Ibadah → Widgets** also provides pinning where supported and manages existing instances.

Launcher cell sizes vary. Resize if you need more information. Previews use sample data; installed widgets show their configured schedule. No missing official time is fabricated.

## Upgrades and your records

Install a newer APK over the existing app only when its signing certificate is compatible. Normal upgrades retain bookmarks, tracking, reminders and placed widgets. Export personal data through Backup before changing installation/signing channels. Do not clear/uninstall an app containing records merely to dismiss an installation error.

To build or modify it, follow [Clone and build](../CLONE_AND_BUILD.md). Project code is GPL v3; third-party content has separate terms and [unresolved redistribution checks](CONTENT_RIGHTS.md).
