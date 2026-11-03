"""Export native picker preview derivatives and calculate worst backdrop contrast."""
from pathlib import Path
import json
import shutil
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / "app/build/verification/v322"
masters = ROOT / "design/widgets-v322"
masters.mkdir(exist_ok=True)
for provider, family in [("NextPrayerWidget", "next"), ("NextPrayerStripWidget", "strip"), ("DayPrayerWidget", "day"), ("ExpandedPrayerWidget", "expanded")]:
    for theme in ["light", "dark"]:
        source = QA / "previews" / f"{provider}-{theme}.png"
        nested = QA / "previews/widget-v321-previews" / source.name
        if nested.exists():
            source = nested
        shutil.copy2(source, masters / f"{family}-{theme}.png")
        if theme == "light":
            image = Image.open(source)
            if image.width > 900:
                image = image.resize((900, round(image.height * 900 / image.width)), Image.Resampling.LANCZOS)
            target = ROOT / f"app/src/main/res/drawable-nodpi/widget_preview_{family}.png"
            image.save(target, optimize=True)
            while target.stat().st_size > 250_000:
                image = image.resize((round(image.width * .9), round(image.height * .9)), Image.Resampling.LANCZOS)
                image.save(target, optimize=True)


def rgb(text):
    return tuple(int(text[i:i+2], 16) for i in [0, 2, 4])


def luminance(color):
    channels = [v / 255 for v in color]
    linear = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in channels]
    return .2126 * linear[0] + .7152 * linear[1] + .0722 * linear[2]


def contrast(a, b):
    a, b = sorted([a, b])
    return (b + .05) / (a + .05)


art = Image.open(ROOT / "app/src/main/res/drawable-nodpi/widget_mosque_v322.png").convert("RGBA")
report = {}
for theme, bg, inks in [
    ("Ivory", "F2E7D7", {"text": "28231E", "metadata": "62574D", "time": "7A491A"}),
    ("Noir", "3A2A20", {"text": "F7EEDD", "metadata": "C2B5A3", "time": "E8B86D"}),
]:
    base = rgb(bg)
    values = []
    for r, g, b, alpha in art.getdata():
        opacity = alpha / 255 * .10
        values.append(luminance(tuple(round(c * opacity + d * (1-opacity)) for c, d in zip((r, g, b), base))))
    for role, ink in inks.items():
        value = min(contrast(luminance(rgb(ink)), min(values)), contrast(luminance(rgb(ink)), max(values)))
        report[theme + " " + role] = round(value, 2)
        assert value >= 4.5, (theme, role, value)
(QA / "contrast.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
