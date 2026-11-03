"""Focused navigation/layout regression. Only emulator-5554 is resized; settings restored."""
from pathlib import Path
import json, re, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/build/verification/v3'
ADB = 'C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = 'emulator-5554'

def adb(*args):
    result = subprocess.run([ADB, '-s', SERIAL, *map(str, args)], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', 'replace'))
    return result.stdout.decode('utf-8', 'replace')

def check(label, classes):
    result = adb('shell', 'am', 'instrument', '-w', '-e', 'capture', label, '-e', 'class',
                 ','.join('com.example.waktusolatmalaysia.' + name for name in classes),
                 'com.example.waktusolatmalaysia.test/androidx.test.runner.AndroidJUnitRunner')
    (OUT / (label + '.log')).write_text(result, encoding='utf-8')
    if 'OK (' not in result:
        raise RuntimeError(label + '\n' + result[-3000:])
    print(label + ': PASS', flush=True)

if __name__ == '__main__':
    original = {key: adb('shell', 'wm', key) for key in ['size', 'density']}
    font = adb('shell', 'settings', 'get', 'system', 'font_scale').strip()
    results = []
    try:
        adb('shell', 'wm', 'density', 160)
        for scale in [1, 1.3, 2]:
            adb('shell', 'settings', 'put', 'system', 'font_scale', scale)
            for width in [320, 360, 600, 840, 1280]:
                adb('shell', 'wm', 'size', f'{width}x1000')
                label = f'navigation-android-{width}-text{scale}'
                classes = ['AdaptiveProbeTest']
                if scale == 1 and width in [320, 600, 1280]:
                    classes.append('HubTouchTest')
                previous = OUT / (label + '.log')
                if '--resume' in sys.argv and previous.exists() and 'OK (' in previous.read_text(encoding='utf-8'):
                    print(label + ': previously passed', flush=True)
                else:
                    check(label, classes)
                results.append({'width': width, 'height': 1000, 'fontScale': scale, 'passed': True})
        adb('shell', 'settings', 'put', 'system', 'font_scale', 1)
        adb('shell', 'wm', 'size', '840x400')
        check('navigation-android-short-landscape', ['HubTouchTest', 'AdaptiveProbeTest'])
        results.append({'width': 840, 'height': 400, 'fontScale': 1, 'passed': True})
        (OUT / 'navigation-layouts.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
    finally:
        for key, output in original.items():
            override = re.search(r'Override ' + key + r':\s*(\S+)', output)
            adb('shell', 'wm', key, override.group(1) if override else 'reset')
        if font == 'null':
            adb('shell', 'settings', 'delete', 'system', 'font_scale')
        else:
            adb('shell', 'settings', 'put', 'system', 'font_scale', font)
        adb('pull', '/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-verification',
            str(OUT / 'navigation-emulator-previews'))
