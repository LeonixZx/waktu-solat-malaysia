"""Capture existing launcher widgets without placing, deleting or resizing them."""
from pathlib import Path
import io
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / "app/build/verification/v322"
OUT = ROOT / "deliverables/v3.2.2/screenshots"
OUT.mkdir(parents=True, exist_ok=True)
ADB = r"C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe"


def adb(*args):
    return subprocess.run([ADB, "-s", "DEVICE_SERIAL", *args], capture_output=True, check=True).stdout


def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/v322-capture.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/v322-capture.xml"))


def page(tree):
    for node in tree.iter("node"):
        match = re.search(r"Page (\d+)\. Total pages: (\d+)", node.get("content-desc", ""))
        if match:
            return int(match[1])
    raise AssertionError("Launcher page indicator unavailable")


def go(target):
    for attempt in range(8):
        tree = dump()
        current = page(tree)
        if current == target:
            return tree
        start, end = (1350, 250) if current < target else (250, 1350)
        adb("shell", "input", "swipe", str(start), "1900", str(end), "1900", "350")
        time.sleep(.6)
    raise AssertionError("Could not reach launcher page " + str(target))


adb("shell", "input", "keyevent", "KEYCODE_HOME")
time.sleep(.8)
captured = {}
for number in [3, 4, 5]:
    tree = go(number)
    raw = adb("exec-out", "screencap", "-p")
    (OUT / f"launcher-page-{number}.png").write_bytes(raw)
    (QA / f"launcher-page-{number}.xml").write_bytes(ET.tostring(tree))
    screen = Image.open(io.BytesIO(raw))
    for node in tree.iter("node"):
        if not node.get("resource-id", "").endswith(":id/widget_surface"):
            continue
        ids = {child.get("resource-id", "").split("/")[-1] for child in node.iter("node")}
        family = "next" if "widget_time" in ids else "expanded" if "widget_settings" in ids else "day" if "widget_icon_1" in ids else "strip"
        bounds = tuple(map(int, re.findall(r"\d+", node.get("bounds"))))
        assert len(bounds) == 4
        screen.crop(bounds).save(OUT / f"launcher-{family}.png", optimize=True)
        captured[family] = {"page": number, "bounds": bounds}
    print("Captured launcher page " + str(number), flush=True)
assert set(captured) == {"next", "strip", "day", "expanded"}, captured
(QA / "launcher-captures.json").write_text(__import__("json").dumps(captured, indent=2), encoding="utf-8")
go(3)
print("All four existing launcher instances captured; left on page 3.")
