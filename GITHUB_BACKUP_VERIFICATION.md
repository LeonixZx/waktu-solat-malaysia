# GitHub source backup

Public repository: https://github.com/LeonixZx/waktu-solat-malaysia

The initial Git history preserves the verified **v3.3.3** source archive as its own commit/tag,
then records v3.3.4's alert fixes and repository documentation. Android/PWA code, wrapper,
lockfile, shared content, original project assets, tests and tools are included. Personal test
snapshots, signing keys, credentials, caches, dependency installations, reference APKs and the
decoded workspace are excluded. Repository licence/provenance records distinguish project code
from third-party content.

The publishing task verifies a fresh independent clone and builds Android/PWA using only that
clone plus installed tools and downloadable dependencies. Its machine-readable log/report is
published with the release. GitHub Actions repeats Android unit/lint/build and PWA unit/build
checks for future commits and pull requests. A source checkout needs JDK21/SDK34 and Node.js;
it does not need a system Gradle or the original local project directory.

Confirmed: an unauthenticated public clone of commit
`PRIVACY_REWRITTEN_SOURCE_COMMIT` built Android debug/unit/lint from a separate temporary
directory with JDK21 and SDK34, without the original source/build folders. All 46 Android JVM
tests passed. In that same independent clone, `npm ci`, PWA tests and `npm run build` passed.
The later documentation/CI fix does not change application source. The first cloud Android job
exposed a deprecated default `tools` package in the SDK setup action; the workflow explicitly
selects `platform-tools` and installs SDK34/Build Tools34.0.0. Cloud results are linked in the
final release verification.

Versioned APKs, SHA-256 sums and source/recovery archives are distributed as GitHub Release
assets rather than bloating Git history with compiled outputs. The public source archive contains
the editable project; anyone can clone or fork it subject to the applicable licences.

GitHub does **not** store the owner's private debug/production signing keys, Android SDK/JDK,
phone app data, OneDrive account or deployment credentials. Keep signing keys and personal
exports backed up separately. To update existing debug installations from a new computer,
restore the compatible private debug keystore; a fresh auto-generated key will differ.

Do not delete this original folder until the latest remote tag/release and the independent clone
verification have completed. The final response and release verification report identify the
confirmed URL and checks.
