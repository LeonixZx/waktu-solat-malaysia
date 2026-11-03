"""Capture upgrade baseline and copy original generated artwork into the project."""
from pathlib import Path
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / "app/build/verification/v322"
QA.mkdir(parents=True, exist_ok=True)
ADB = r"C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PKG = "com.example.waktusolatmalaysia"


def adb(*args):
    return subprocess.run([ADB, "-s", "DEVICE_SERIAL", *args], capture_output=True, check=True).stdout


for name in ["waktu_solat", "personal_library", "widget_config"]:
    target = QA / f"before-{name}.xml"
    if not target.exists():
        target.write_bytes(adb("exec-out", "run-as", PKG, "cat", f"shared_prefs/{name}.xml"))
for name in ["personal_tasks.json", "backup-before-import.json"]:
    target = QA / f"before-{name}"
    if not target.exists():
        target.write_bytes(adb("exec-out", "run-as", PKG, "cat", f"files/{name}"))
settings = QA / "tablet-settings-before.json"
if not settings.exists():
    settings.write_text(json.dumps({key: adb("shell", "settings", "get", "system", key).decode().strip() for key in ["font_scale", "accelerometer_rotation", "user_rotation"]}, indent=2), encoding="utf-8")
shutil.copy2(ROOT / "app/build/verification/v321/web-before.json", QA / "web-before.json")
for package, name in [(PKG, "own-package-before"), ("com.murad.waktusolat", "original-package-before")]:
    target = QA / (name + ".txt")
    if not target.exists():
        target.write_bytes(adb("shell", "dumpsys", "package", package))
(QA / "launcher-before.png").write_bytes(adb("exec-out", "screencap", "-p"))
source = Path(r"C:/Users/USER/.codex/generated_images/01a1993c-6642-7d21-a1df-33915dfff90f")
assets = ROOT / "design/brand-v322"
assets.mkdir(parents=True, exist_ok=True)
for src, dst in [
    ("exec-b3b66df8-cac2-4b59-b52d-0888ded1a602.png", "mosque-original.png"),
    ("exec-ef41418a-b0b1-4b19-a3d5-68006766ec56.png", "logo-first-draft.png"),
    ("exec-f2147307-c102-4313-868a-ea80deaa6d29.png", "logo-original.png"),
]:
    shutil.copy2(source / src, assets / dst)
print("Fresh tablet baseline and original generated assets saved.")
