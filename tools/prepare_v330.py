"""Capture upgrade baseline and copy original generated artwork into the project."""
from pathlib import Path
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
QA = ROOT / "app/build/verification/v330"
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
shutil.copy2(ROOT / "app/build/verification/v322/web-before.json", QA / "web-before.json")
for package, name in [(PKG, "own-package-before"), ("com.murad.waktusolat", "original-package-before")]:
    target = QA / (name + ".txt")
    if not target.exists():
        target.write_bytes(adb("shell", "dumpsys", "package", package))
(QA / "launcher-before.png").write_bytes(adb("exec-out", "screencap", "-p"))
