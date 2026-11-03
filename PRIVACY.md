# Public source and personal data

The public repository is application source, not a backup of a device's personal app data.
Do not commit signing keys, account tokens, local SDK configuration, USB device identifiers,
phone-data snapshots or real-device screenshots containing personal information.

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
See https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository.
