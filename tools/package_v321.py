"""Package Android 3.2.1 without personal snapshots or publisher assets."""
from pathlib import Path
import hashlib
import html
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / "app/build/verification/v321"
OUT = ROOT / "deliverables"
REVIEW = OUT / "v3.2.1"


def read(path):
    return path.read_text(encoding="utf-8-sig", errors="replace")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


assert "BUILD SUCCESSFUL" in read(QA / "native-build.log")
for label in ["tablet-widgets", "emulator-widgets", "api24-widgets"]:
    assert "OK (12 tests)" in read(QA / f"{label}.log"), label
assert all(v["retained"] for v in json.loads(read(QA / "data-retention.json")).values())
assert json.loads(read(QA / "pwa-unchanged.json"))["unchanged"]
assert json.loads(read(QA / "tablet-settings-restored.json"))["restored"]
final = json.loads(read(QA / "tablet-final-state.json"))
assert final["version"] == "3.2.1" and final["versionCode"] == 9
assert final["personalDataRetained"] and not final["orphanConfigurations"]
assert final["temporaryWidgetBindGrantRevoked"] and final["settingsRestored"]
units = 0
for path in (ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"):
    result = ET.parse(path).getroot().attrib
    units += int(result["tests"])
    assert int(result.get("failures", "0")) == int(result.get("errors", "0")) == 0
assert units == 41
lint = read(ROOT / "app/build/reports/lint-results-debug.txt")
assert "0 errors, 206 warnings" in lint
for item in json.loads(read(OUT / "SHA256SUMS.json")):
    assert sha(OUT / item["file"]) == item["sha256"], "Older artifact changed: " + item["file"]

artifacts = []
aapt = r"C:/Users/USER/AppData/Local/Android/Sdk/build-tools/34.0.0/aapt.exe"
for variant, relative in [
    ("debug", "debug/app-debug.apk"),
    ("release-unsigned", "release/app-release-unsigned.apk"),
]:
    source = ROOT / "app/build/outputs/apk" / relative
    meta = subprocess.run([aapt, "dump", "badging", str(source)], capture_output=True, check=True).stdout.decode("utf-8", "replace")
    for value in ["name='com.example.waktusolatmalaysia'", "versionName='3.2.1'", "versionCode='9'", "sdkVersion:'24'", "targetSdkVersion:'34'"]:
        assert value in meta, value
    target = OUT / f"Waktu-Solat-Malaysia-v3.2.1-{variant}.apk"
    shutil.copy2(source, target)
    artifacts.append(target)
assert sha(artifacts[0]) == final["installedDebugSha256"]

for name in ["WIDGETS_V321.md", "VERIFICATION_V321.md"]:
    shutil.copy2(ROOT / name, REVIEW / name)
host_target = REVIEW / "screenshots/host"
host_target.mkdir(parents=True, exist_ok=True)
host_source = QA / "tablet-widgets-screenshots/tablet-verification"
for provider in ["NextPrayerWidget", "NextPrayerStripWidget", "DayPrayerWidget", "ExpandedPrayerWidget"]:
    for theme in ["light", "dark"]:
        name = f"v32-{provider}-{theme}.png"
        shutil.copy2(host_source / name, host_target / name)
fixture_target = REVIEW / "native-preview-masters"
fixture_target.mkdir(exist_ok=True)
for source in (ROOT / "design/widgets-v321").glob("*.png"):
    shutil.copy2(source, fixture_target / source.name)

# Assemble a contact sheet from direct launcher screenshots; no widget content is redrawn.
cards = [
    ("Next prayer", "launcher-card-3-240-887.png"),
    ("Prayer strip", "launcher-card-4-240-298.png"),
    ("Today's prayers", "launcher-card-4-240-887.png"),
    ("Daily Plus", "launcher-card-5-240-298.png"),
]
font_path = Path(r"C:/Windows/Fonts/segoeui.ttf")
title_font = ImageFont.truetype(str(font_path), 32)
label_font = ImageFont.truetype(str(font_path), 23)
small_font = ImageFont.truetype(str(font_path), 18)
canvas_width = 1040
rendered = []
for title, name in cards:
    card = Image.open(REVIEW / "screenshots" / name).convert("RGB")
    max_width = 520 if title == "Next prayer" else 960
    if card.width > max_width:
        card = card.resize((max_width, round(card.height * max_width / card.width)), Image.Resampling.LANCZOS)
    rendered.append((title, card))
canvas_height = 136 + sum(card.height + 64 for _, card in rendered) + 48
canvas = Image.new("RGB", (canvas_width, canvas_height), "#FCF9F3")
draw = ImageDraw.Draw(canvas)
draw.text((40, 24), "Waktu Solat Malaysia · 3.2.1", font=title_font, fill="#28231E")
draw.text((40, 72), "Actual Xiaomi launcher captures · Noir appearance", font=small_font, fill="#6F6459")
y = 124
for title, card in rendered:
    draw.text((40, y), title, font=label_font, fill="#28231E")
    canvas.paste(card, (40, y + 38))
    y += card.height + 64
draw.text((40, canvas_height - 36), "Ivory and Noir host captures are included in the gallery.", font=small_font, fill="#6F6459")
canvas.save(REVIEW / "widget-preview.png", optimize=True)

gallery = []
for title, name in cards:
    gallery.append(f'<figure><img src="screenshots/{name}" alt="{html.escape(title)} on Xiaomi launcher"><figcaption>{html.escape(title)} — actual launcher crop</figcaption></figure>')
for source in sorted(host_target.glob("*.png")):
    label = source.stem.replace("v32-", "").replace("-", " · ")
    gallery.append(f'<figure><img src="screenshots/host/{source.name}" alt="{html.escape(label)} native host"><figcaption>{html.escape(label)} — physical tablet AppWidgetHost, not launcher placement</figcaption></figure>')
(REVIEW / "index.html").write_text(
    '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
    '<title>Widget 3.2.1 verification gallery</title><style>body{margin:24px auto;padding:0 20px;max-width:1120px;font:16px system-ui;background:#fcf9f3;color:#28231e}figure{margin:24px 0;padding:16px;background:#eee6d9;border-radius:20px}img{max-width:100%;height:auto;display:block}figcaption{margin-top:12px}a{color:#805020}</style>'
    '<h1>Waktu Solat Malaysia 3.2.1</h1><p>Direct launcher captures first; native host captures in both appearances follow. Full launcher pages remain in the screenshots folder. Native preview masters use labelled example data.</p>'
    '<p><a href="VERIFICATION_V321.md">Verification and remaining manual checks</a> · <a href="WIDGETS_V321.md">Design specification</a></p>'
    + "\n".join(gallery) + '</html>\n', encoding="utf-8")
metadata = {
    "androidVersion": "3.2.1", "versionCode": 9,
    "applicationId": "com.example.waktusolatmalaysia", "pwaVersion": "3.1.0",
    "unitTests": units, "lintErrors": 0, "lintWarnings": 206,
    "widgetTestsPerEnvironment": 12,
    "environments": ["API24 emulator", "API34 emulator", "Xiaomi API36 tablet"],
    "measuredLayoutCasesPerEnvironment": 576, "additionalCompactStripCasesPerEnvironment": 12,
    "physicalPlacedWidgetIds": final["placedWidgetIds"], "personalDataRetained": True,
    "widgetConfigurationsRetained": 4, "pwaSourceFilesUnchanged": 53,
    "manualChecks": ["Xiaomi drag-handle resizing", "Spoken TalkBack / physical keyboard traversal", "Reboot / launcher restart", "Prolonged battery-restricted delivery"],
    "artifacts": [{"file": p.name, "bytes": p.stat().st_size, "sha256": sha(p)} for p in artifacts],
}
(REVIEW / "artifact-manifest.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
archive = OUT / "Waktu-Solat-Malaysia-v3.2.1-delivery.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as bundle:
    for path in artifacts:
        bundle.write(path, path.name)
    for path in sorted(REVIEW.rglob("*")):
        if path.is_file():
            bundle.write(path, "review/" + path.relative_to(REVIEW).as_posix())
with zipfile.ZipFile(archive) as bundle:
    assert bundle.testzip() is None
    assert not any(any(term in name for term in ["personal_library", "before-import", "reference-read-only.apk"]) for name in bundle.namelist())
artifacts.append(archive)
records = [{"file": p.name, "bytes": p.stat().st_size, "sha256": sha(p)} for p in sorted(OUT.iterdir()) if p.suffix in [".apk", ".zip"]]
(OUT / "SHA256SUMS.json").write_text(json.dumps(records, indent=2) + "\n", encoding="utf-8")
(OUT / "Waktu-Solat-Malaysia-v3.2.1-SHA256SUMS.txt").write_text("".join(sha(p) + "  " + p.name + "\n" for p in artifacts), encoding="utf-8")
(QA / "delivery-metrics.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
print(json.dumps([r for r in records if "v3.2.1" in r["file"]], indent=2))
