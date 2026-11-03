# Project tools

Building the app needs only the Gradle wrapper; these are optional development tools.

| Tool | Purpose |
| --- | --- |
| `doctor.py` | Read-only JDK/SDK/wrapper check; `--web` also checks Node.js. Python 3.10+, standard library only. |
| `privacy_audit.py` | Reject private/generated files, known credential patterns and local profile/device identifiers. Use `--staged` to scan exact index bytes; ZIP arguments scan archive contents. |
| `tests/` | Privacy boundary/index regression checks; run `python -m unittest discover -s tools/tests`. |
| `android_emulator_matrix.py` | Opt-in dedicated Windows AVD runner; see [emulator documentation](../EMULATOR_TESTING.md). |
| `local_android.py` | Resolve the local SDK/ADB without embedded owner paths/serials. |
| `refresh-prayer-zones.ps1` | Refresh the public official zone catalog. |
| `prepare_fonts.py` | Regenerate renamed WSM Sans derivatives; requires FontTools. Finished fonts/licenses are already committed. |
| `export_brand_v322.py`, `widget_previews_v333.py` | Optional asset export tools using Pillow/native captures. Runtime assets are already committed. |

The `verify_*`, `test_phone_*`, `phone_matrix_*`, `launcher_widgets_*` and measurement scripts are **session-specific opt-in QA utilities**, not installation steps. Inspect arguments/environment requirements first. Some need private before/after baselines, capture screenshots, toggle device settings or reboot a selected test environment. Keep their outputs private under ignored build folders; do not publish unredacted diagnostic files. Native instrumentation lives under `app/src/androidTest/`; the debug-only local TabletProbe is excluded from release APKs.

One-time source generators, historical packaging scripts, obsolete v2 tests and superseded widget images were preserved in the owner's ignored `.local-archive/` and removed from the active tree. Release tags/archives retain historical source. Never use old generators as a build/restore step.

The reusable web-brand export is `web/tools/brand.mjs`. No tool needs to write to the reference decoded-APK workspace.
