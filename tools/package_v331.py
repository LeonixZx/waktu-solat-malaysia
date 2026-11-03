"""Package the independently verified optional widget release without changing old installers."""
from pathlib import Path
import hashlib
import html
import json
import re
import shutil
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / 'app/build/verification/v331'
OUT = ROOT / 'deliverables'
REVIEW = OUT / 'v3.3.1'
REVIEW.mkdir(exist_ok=True)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def read(path):
    return path.read_text(encoding='utf-8-sig')

source = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
digest = sha(source)
assert 'BUILD SUCCESSFUL' in read(QA / 'final-release-build.log')
checks = []
for profile, suite, count in [('standard_phone', 'full', 22), ('legacy_api24', 'timeline', 4)]:
    folder = ROOT / 'app/build/verification/emulator-matrix' / profile
    result = json.loads(read(folder / (suite + '-result.json')))
    assert result['passed'] and result['systemUiHealthy'] and result['nativeAssertionsPassed']
    assert result['tests'] == count and result['apkSha256'] == digest
    checks.append(result)
    for name in [suite + '.log', suite + '-result.json', suite + '-crash.txt', 'health-after.xml']:
        if (folder / name).exists():
            shutil.copy2(folder / name, QA / (profile + '-' + name))

old_records = json.loads(read(OUT / 'SHA256SUMS.json'))
for record in old_records:
    assert sha(OUT / record['file']) == record['sha256'], record['file']
reference = json.loads(read(QA / 'reference-read-only-hashes.json'))
assert all(sha(Path(name)) == value for name, value in reference.items())
web = json.loads(read(ROOT / 'app/build/verification/v330/web-before.json'))
assert all(sha(ROOT / name) == value for name, value in web.items())
units = 0
for path in (ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    attrs = ET.parse(path).getroot().attrib
    assert int(attrs.get('failures', 0)) == int(attrs.get('errors', 0)) == 0
    units += int(attrs['tests'])
assert units == 41
lint = re.search(r'0 errors, (\d+) warnings', read(ROOT / 'app/build/reports/lint-results-debug.txt'))
assert lint

screens = REVIEW / 'screenshots'
screens.mkdir(exist_ok=True)
captures = ROOT / 'app/build/verification/emulator-matrix/standard_phone/widget-v331'
for prefix in ['', 'picker-']:
    for component in ['NextPrayerWidget', 'NextPrayerStripWidget', 'DayPrayerWidget', 'ExpandedPrayerWidget']:
        for theme in ['light', 'dark']:
            name = prefix + component + '-' + theme + '.png'
            matches = list(captures.rglob(name))
            assert matches, name
            shutil.copy2(max(matches, key=lambda p: p.stat().st_mtime), screens / name)
for name in ['large-NextPrayerWidget-light.png', 'large-NextPrayerWidget-dark.png']:
    shutil.copy2(max(captures.rglob(name), key=lambda p: p.stat().st_mtime), screens / name)
for component in ['NextPrayerWidget', 'NextPrayerStripWidget', 'DayPrayerWidget', 'ExpandedPrayerWidget']:
    name = '12hour-' + component + '.png'
    shutil.copy2(max(captures.rglob(name), key=lambda p: p.stat().st_mtime), screens / name)

for name in ['DESIGN_V331.md', 'VERIFICATION_V331.md']:
    shutil.copy2(ROOT / name, REVIEW / name)
artifacts = []
for kind, relative in [('debug', 'debug/app-debug.apk'), ('release-unsigned', 'release/app-release-unsigned.apk')]:
    original = ROOT / 'app/build/outputs/apk' / relative
    target = OUT / ('Waktu-Solat-Malaysia-v3.3.1-' + kind + '.apk')
    assert not target.exists() or sha(target) == sha(original), 'Never overwrite a different delivered build'
    shutil.copy2(original, target)
    artifacts.append(target)

# An independent recovery source archive, excluding caches, other APKs and device snapshots.
source_files = [p for p in ROOT.iterdir() if p.is_file() and p.suffix not in ['.apk', '.apks']]
for folder in ['app/src', 'gradle', 'shared', 'web', 'tools', 'testing', 'design']:
    for path in (ROOT / folder).rglob('*'):
        if path.is_file() and not any(part in ['node_modules', 'dist', '.wrangler', '__pycache__', '.git', 'test-results', 'playwright-report'] for part in path.relative_to(ROOT).parts) and not path.name.endswith('.tsbuildinfo'):
            source_files.append(path)
source_files += [ROOT / 'app/build.gradle', ROOT / 'app/proguard-rules.pro']
source_files = sorted(set(source_files))
manifest = {
    'version': '3.3.1', 'versionCode': 12, 'applicationId': 'com.example.waktusolatmalaysia',
    'debugSha256': digest, 'unitTests': units, 'nativeTests': sum(x['tests'] for x in checks),
    'environments': checks, 'lintErrors': 0, 'lintWarnings': int(lint[1]),
    'pwaVersion': '3.1.0', 'pwaUnchangedFiles': len(web), 'referenceUnchangedFiles': len(reference),
    'sourceFiles': {p.relative_to(ROOT).as_posix(): sha(p) for p in source_files},
    'artifacts': [{'file': p.name, 'sha256': sha(p), 'bytes': p.stat().st_size} for p in artifacts],
    'limitations': ['Native widget host evidence; OEM launcher placement is a separate check',
                    'No new physical-device installation or reboot/background-delivery certification',
                    'Unsigned release builds; runtime verification uses the signed debug APK']}
source_archive = OUT / 'Waktu-Solat-Malaysia-v3.3.1-source.zip'
assert not source_archive.exists(), 'Preserve existing source checkpoints'
with zipfile.ZipFile(source_archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in source_files:
        archive.write(path, path.relative_to(ROOT).as_posix())
    archive.writestr('recovery/V331_SOURCE_FILES.json', json.dumps(manifest, indent=2))
with zipfile.ZipFile(source_archive) as archive:
    assert archive.testzip() is None
artifacts.append(source_archive)

font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 27)
small = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 18)
canvas = Image.new('RGB', (1200, 940), '#FCF9F3')
draw = ImageDraw.Draw(canvas)
draw.text((28, 18), 'Waktu Solat Malaysia 3.3.1 · Timeline widgets', fill='#28231E', font=font)
draw.text((28, 60), 'Native captures · labelled timetable fixture · Classic remains available', fill='#62574D', font=small)
for name, label, box in [
    ('DayPrayerWidget-light.png', 'Daily timeline · Ivory', (28, 125, 744, 350)),
    ('NextPrayerStripWidget-dark.png', 'Six-time strip · Noir', (28, 405, 744, 550)),
    ('NextPrayerWidget-light.png', 'Next prayer · 2×2', (28, 610, 335, 910)),
    ('ExpandedPrayerWidget-dark.png', 'Daily Plus · tracking enabled', (785, 125, 1172, 700))]:
    x1, y1, x2, y2 = box
    draw.text((x1, y1 - 30), label, fill='#62574D', font=small)
    im = Image.open(screens / name).convert('RGBA')
    im.thumbnail((x2-x1, y2-y1), Image.Resampling.LANCZOS)
    canvas.paste(im, (x1+(x2-x1-im.width)//2, y1), im)
canvas.save(REVIEW / 'widget-overview.png', optimize=True)

gallery = []
for path in sorted(screens.glob('*.png')):
    gallery.append('<figure><img loading="lazy" src="screenshots/' + path.name + '"><figcaption>' + html.escape(path.name) + '</figcaption></figure>')
(REVIEW / 'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Timeline widgets 3.3.1</title><style>body{max-width:1200px;margin:24px auto;padding:0 20px;background:#fcf9f3;color:#28231e;font:16px system-ui}figure{display:inline-block;vertical-align:top;width:min(95%,540px);margin:14px}img{width:100%;height:auto}figcaption{margin-top:8px}</style><h1>Timeline widgets · Android 3.3.1</h1><p>Direct native renderer captures using a fixed labelled timetable; no fixture data is written into app caches. Existing instances retain Classic until changed in widget settings.</p><p><a href="VERIFICATION_V331.md">Verification</a> · <a href="DESIGN_V331.md">Design</a></p>' + ''.join(gallery), encoding='utf-8')
(REVIEW / 'artifact-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
delivery = OUT / 'Waktu-Solat-Malaysia-v3.3.1-delivery.zip'
assert not delivery.exists(), 'Preserve existing deliveries'
with zipfile.ZipFile(delivery, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in artifacts[:2]:
        archive.write(path, path.name)
    for path in REVIEW.rglob('*'):
        if path.is_file(): archive.write(path, 'review/' + path.relative_to(REVIEW).as_posix())
with zipfile.ZipFile(delivery) as archive:
    assert archive.testzip() is None
artifacts.append(delivery)
records = [{'file': p.name, 'sha256': sha(p), 'bytes': p.stat().st_size}
           for p in sorted(OUT.iterdir()) if p.suffix in ['.apk', '.zip']]
(OUT / 'SHA256SUMS.json').write_text(json.dumps(records, indent=2), encoding='utf-8')
(OUT / 'Waktu-Solat-Malaysia-v3.3.1-SHA256SUMS.txt').write_text(''.join(sha(p) + '  ' + p.name + '\n' for p in artifacts), encoding='utf-8')
print(json.dumps({'unitTests': units, 'nativeTests': manifest['nativeTests'], 'debugSha256': digest,
                  'artifacts': [p.name for p in artifacts]}, indent=2))
