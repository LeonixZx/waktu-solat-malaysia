# Public source and personal data

The public repository is application source, not a backup of a device's personal app data.
Do not commit signing keys, account tokens, local SDK configuration, USB device identifiers,
phone-data snapshots or real-device screenshots containing personal information.

## App data and external requests

Bookmarks, preferences, counters, prayer tracking, downloaded text and reminder definitions are
stored locally. Export/import is user-initiated; keep exported personal files private. There are
no app accounts or account-sync backend in this project. Location permission supports Kiblat;
it is not a requirement to select an official prayer zone manually.

Fetching timetables/content, loading publisher thumbnails, streaming recitation or opening a
video/article contacts the corresponding external provider. Providers can receive normal request
metadata such as an IP address and the requested zone/content identifier. YouTube playback is
loaded after interaction. See `API_SOURCES.md` for providers. A personal PWA deployment can also
have hosting/platform request logs; configure that deployment's retention and access separately.

## Publishing policy

Only editable source, tests with public fixtures, reviewed documentation, original design sources,
required runtime assets and licence records belong in Git. Build outputs go in Releases/CI artifacts.
Private diagnostics, SDK paths, device snapshots, keys and credentials stay in ignored local folders.
Review screenshots and image metadata manually. Ignore rules do not remove already tracked files.

Use `python tools/privacy_audit.py --staged` after staging a change. CI additionally runs Gitleaks
against reachable history. Recovery-manifest SHA-256 rows have a narrow checksum-only allowlist;
other content in those files remains scanned. No scanner can prove the absence of every private datum.

Historical records are under `docs/history/`. Obsolete generators and previews were archived locally;
they are unnecessary for a public clone/build. Original installers and private recovery copies are retained.

On 3 November 2026, owner-specific Windows paths and test-device serials were removed from
all published branches/tags. Source/recovery/delivery archives were rebuilt with generic metadata.
Real-device notification screenshots were removed from Releases and delivery ZIPs. The APKs
were independently inspected for those known metadata strings and remain byte-identical.
Original archives/history are retained only in the owner's ignored local recovery area.

Run `python tools/privacy_audit.py` before publishing. For ZIPs use
`python tools/privacy_audit.py path/to/archive.zip`. CI runs the source check too. The check
does not establish that every conceivable secret or personal image has been detected; review
new assets and changes before publication.

Rewriting Git changes commit hashes. Existing collaborators should clone the cleaned repository
again rather than merge/push an old clone, which could restore removed history. Historical source
manifests describe the original local evidence; privacy-clean archives have new checksums.

Copies already downloaded by third parties cannot be recalled. GitHub may also retain cached
views of old commit IDs after a history rewrite. Removing those server-side caches may require
the owner to contact GitHub Support; repository editing alone cannot guarantee their removal.
The owner has submitted that review request; no server-side purge is claimed as completed.
See https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository.
