# Public repository review — 3 November 2026

## Scope and changes

The active source tree is for building and maintaining the app. APKs/recovery downloads belong in
Releases, while private keys, phone data, diagnostics and local tool installations stay outside Git.
This review did not change Android/PWA runtime code, package/version, personal-data formats or APKs.

- Archived 50 obsolete generator/packaging, preview and v2 diagnostic files locally before removing
  them from the active tree; approximately 3.7MB. No original installer or recovery archive was deleted.
- Organized 23 historical documents under `docs/history/` and repaired local documentation links.
- Removed two redundant public QA delivery bundles from Releases after verifying exact local
  backup copies. Kept all APKs and source/recovery archives; refreshed public checksum lists.
- Added install/build/troubleshooting/contribution/security guides, issue forms and a PR template.
- Added original decorative README art and real emulator screenshots with public sample data.
- Added staged-byte privacy checks and five regression tests, broader ignore rules, full-history
  Gitleaks scanning and a narrow recovery-checksum allowlist.
- Pinned workflow actions to reviewed commit IDs; kept CI permissions read-only and publisher keys
  absent. Added manual workflow dispatch and a checksummed debug artifact with 14-day retention.
- Enabled GitHub private vulnerability reporting, Dependabot alerts/security updates, and monthly
  dependency update configuration. Secret scanning and push protection were already enabled.

## Evidence and boundaries

- Reachable-history Gitleaks scan passed. Its original 12 findings were reviewed: SHA-256 source-file
  checksums in the two recovery manifests, not credentials. Only matching checksum rows are allowlisted.
- Reviewed source/index scans passed; no known owner profile paths, hardcoded USB identifiers,
  credentials, private-data files or generated output were found in the candidate publication.
- Reviewed raster metadata contained no EXIF/location/contact data. Visible screenshots use emulator
  fixtures; real-device captures stay private.
- All 42 existing local APK/ZIP artifacts matched their stored checksums.
- A separate exported checkout built the Android debug APK with the included wrapper, without
  build-cache reuse: 46 JVM tests passed; lint had 0 errors and 605 existing warnings. PWA `npm ci`,
  all 17 unit tests and its production/service-worker build passed. Five privacy-tool tests passed.
- [GitHub Actions verification of commit 9a22741](https://github.com/LeonixZx/waktu-solat-malaysia/actions/runs/41629851493)
  passed privacy, Android and PWA jobs. The uploaded `android-debug` APK was downloaded, its
  included checksum matched, and Android `apksigner verify` passed. No test APK was installed
  over a user's existing installation for this repository-only review.
- `npm audit --omit=dev` reported no production dependency advisories at review time. The full audit
  reported 10 low/moderate development-tool chain findings (no high/critical). They involve the PWA
  build chain; the audit proposes a major plugin change. No forced downgrade/major dependency change
  was made as part of documentation cleanup. Dependabot is enabled for follow-up review.

No scan proves that every possible private datum has been detected. Public copies downloaded
previously cannot be recalled. GitHub's cached pre-cleanup commit views remain subject to the owner's
pending Support review; this report does not claim a server-side purge.

Content rights remain separate: see [CONTENT_RIGHTS.md](CONTENT_RIGHTS.md). Inherited devotional
compilations and azan recordings still require redistribution permission or verified replacements.

Local detailed logs and backup manifests remain in ignored build/recovery folders. To repeat the
public checks, use the commands in [CONTRIBUTING.md](../CONTRIBUTING.md) and the GitHub workflow.
