# Contributing

Contributions in Bahasa Melayu or English are welcome. A focused launcher fix, accessibility improvement, verified content reference, interface translation or reproducible bug report can all help.

Check existing [issues](https://github.com/LeonixZx/waktu-solat-malaysia/issues) and explain the user problem before a large change. See [Clone and build](CLONE_AND_BUILD.md) for setup; no private credentials or decoded reference app are needed. Keep discussion respectful, review the work rather than the person, and provide sources for religious-content changes. Do not generate religious translations or transliterations with AI.

## Pull requests

1. Fork, create a branch, and make a focused change.
2. Preserve application-data compatibility: saved IDs, legacy tracking keys, cached schedules, reminder definitions and widget instance settings.
3. Run relevant checks:

```sh
python tools/privacy_audit.py
python -m unittest discover -s tools/tests
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Use `.\gradlew.bat` on Windows. For PWA changes run `npm ci`, `npm test` and `npm run build` inside `web/`. Native instrumentation requires a chosen emulator/device; see [EMULATOR_TESTING.md](EMULATOR_TESTING.md). For UI changes, test narrow/wide layouts and enlarged text.

4. Explain the behavior, validation and limitations. Include emulator/synthetic-data captures for UI changes. Redact personal information before attaching images/logs.

Do not include keystores, tokens, SDK configuration, user exports, device serials, build output, personal screenshots or decoded-APK code/assets. Run the privacy checker after staging new files. A later deletion does not erase public Git history.

## Content and assets

Use original, public-domain or appropriately licensed material; record its source, licence, attribution and restrictions. Read [NOTICE.md](NOTICE.md), [ASSET_PROVENANCE.md](ASSET_PROVENANCE.md) and [the rights register](docs/CONTENT_RIGHTS.md). Public API access does not itself grant redistribution rights. Retain Quran/hadith IDs and actual translation-language labels.

Keep generated-art prompts and originals. Use real app captures for features; label illustrative art/test data. Avoid another app's visual identity or secrets.

Report vulnerabilities privately through [SECURITY.md](SECURITY.md). Your project-authored contribution is offered under GPL v3; third-party material keeps its own terms.
