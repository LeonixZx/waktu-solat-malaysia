"""Resize generated originals into platform assets; preserve source PNG alpha."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SOURCE = ROOT / "design/brand-v322"
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
logo = Image.open(SOURCE / "logo-original.png").convert("RGBA")
mosque = Image.open(SOURCE / "mosque-original.png").convert("RGBA")
(RES / "drawable-nodpi").mkdir(exist_ok=True)
logo.resize((512, 512), Image.Resampling.LANCZOS).save(RES / "drawable-nodpi/brand_mark_v322.png", optimize=True)
foreground = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
foreground.alpha_composite(logo.resize((310, 310), Image.Resampling.LANCZOS), (101, 101))
foreground.save(RES / "drawable-nodpi/brand_launcher_foreground_v322.png", optimize=True)
mosque.resize((600, 400), Image.Resampling.LANCZOS).save(RES / "drawable-nodpi/widget_mosque_v322.png", optimize=True)
assert (RES / "drawable-nodpi/widget_mosque_v322.png").stat().st_size <= 250_000
# Native drawable wrappers retain scalable fit and platform clipping.
for name in ["ic_brand", "ic_brand_toolbar", "ic_launcher"]:
    (RES / "drawable" / (name + ".xml")).write_text(f'<bitmap {NS} android:src="@drawable/brand_mark_v322" android:gravity="fill" android:filter="true"/>\n', encoding="utf-8")
(RES / "drawable/ic_launcher_foreground_v322.xml").write_text(f'<bitmap {NS} android:src="@drawable/brand_launcher_foreground_v322" android:gravity="fill" android:filter="true"/>\n', encoding="utf-8")
(RES / "drawable/ic_launcher_monochrome.xml").write_text(f'<bitmap {NS} android:src="@drawable/brand_launcher_foreground_v322" android:gravity="fill" android:filter="true"/>\n', encoding="utf-8")
for version in [26, 33]:
    folder = RES / f"mipmap-anydpi-v{version}"
    mono = '<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>' if version == 33 else ''
    (folder / "ic_launcher_v322.xml").write_text(f'<adaptive-icon {NS}><background android:drawable="@color/launcher_background"/><foreground android:drawable="@drawable/ic_launcher_foreground_v322"/>{mono}</adaptive-icon>\n', encoding="utf-8")
for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    folder = RES / ("mipmap-" + density)
    folder.mkdir(exist_ok=True)
    tile = Image.new("RGBA", (size, size), "#2B2119")
    art = logo.resize((round(size * .90), round(size * .90)), Image.Resampling.LANCZOS)
    tile.alpha_composite(art, ((size - art.width) // 2, (size - art.height) // 2))
    tile.save(folder / "ic_launcher_v322.png", optimize=True)
print("Generated-source derivatives and normal/round/themed launcher assets exported.")
