"""Read-only verification of the preserved v3.3.0 production files and installers."""
from pathlib import Path
import hashlib
import json
import sys

root = Path(__file__).resolve().parents[1]
baseline = json.loads((root / 'recovery/V330_BASELINE.json').read_text(encoding='utf-8'))
changed = []
missing = []
added = []
for name, digest in baseline['files'].items():
    path = root / name
    if not path.is_file():
        missing.append(name)
    elif hashlib.sha256(path.read_bytes()).hexdigest() != digest:
        changed.append(name)
for folder in baseline.get('directories', []):
    for path in (root / folder).rglob('*'):
        if path.is_file() and '__pycache__' not in path.parts:
            name = path.relative_to(root).as_posix()
            if name not in baseline['files']:
                added.append(name)
for label, names in [('Missing', missing), ('Changed', changed), ('Added', added)]:
    if names:
        print(label + ':')
        for name in names:
            print('  ' + name)
if not missing and not changed and not added:
    print(f"PASS: {len(baseline['files'])} baseline files and installers match v3.3.0.")
else:
    print('The source checkpoint is listed in recovery/V330_BASELINE.json. No files were modified.')
sys.exit(1 if missing or changed or added else 0)
