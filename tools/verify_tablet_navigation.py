"""Reinstall without clearing data; test portrait rail and landscape sidebar on the owner's tablet."""
from pathlib import Path
import json, subprocess, time, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/build/verification/v3'
ADB = 'C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = 'DEVICE_SERIAL'
PACKAGE = 'com.example.waktusolatmalaysia'

def command(*args):
    r = subprocess.run([ADB, '-s', SERIAL, *map(str, args)], capture_output=True)
    if r.returncode:
        raise RuntimeError(r.stderr.decode('utf-8', 'replace'))
    return r.stdout

def text(*args):
    return command(*args).decode('utf-8', 'replace')

def snapshot(stage):
    result = {}
    for name in ['waktu_solat', 'personal_library']:
        raw = command('exec-out', 'run-as', PACKAGE, 'cat', 'shared_prefs/' + name + '.xml')
        (OUT / f'navigation-{stage}-{name}.xml').write_bytes(raw)
        result[name] = {n.attrib['name']: ET.tostring(n, encoding='unicode').strip()
                        for n in ET.fromstring(raw)
                        if not n.attrib['name'].startswith('scheduled_')
                        and n.attrib['name'] not in ['alarm_generation', 'prefetch_day']}
    return result

if __name__ == '__main__':
    before = snapshot('before')
    print(text('install', '-r', ROOT / 'app/build/outputs/apk/debug/app-debug.apk'), flush=True)
    rotation = {key: text('shell', 'settings', 'get', 'system', key).strip()
                for key in ['accelerometer_rotation', 'user_rotation']}
    try:
        text('shell', 'settings', 'put', 'system', 'accelerometer_rotation', 0)
        for value, name in [(0, 'portrait'), (1, 'landscape')]:
            text('shell', 'settings', 'put', 'system', 'user_rotation', value)
            time.sleep(1)
            result = text('shell', 'am', 'instrument', '-w', '-e', 'class',
                          PACKAGE + '.HubTouchTest', PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner')
            (OUT / f'navigation-tablet-{name}.log').write_text(result, encoding='utf-8')
            if 'OK (3 tests)' not in result:
                raise RuntimeError(result[-3000:])
            text('pull', '/sdcard/Android/data/' + PACKAGE + '/files/tablet-verification/settings-selection.png',
                 OUT / f'settings-selection-tablet-{name}.png')
            probe = text('shell', 'am', 'instrument', '-w', PACKAGE + '/' + PACKAGE + '.debug.TabletProbe')
            (OUT / f'navigation-tablet-{name}-probe.log').write_text(probe, encoding='utf-8')
            if '"allHubsReachable":true' not in probe:
                raise RuntimeError(probe[-3000:])
            text('pull', '/sdcard/Android/data/' + PACKAGE + '/files/tablet-probe-home.png',
                 OUT / f'navigation-tablet-{name}.png')
            print(name + ': PASS', flush=True)
    finally:
        for key in ['user_rotation', 'accelerometer_rotation']:
            if rotation[key] == 'null':
                text('shell', 'settings', 'delete', 'system', key)
            else:
                text('shell', 'settings', 'put', 'system', key, rotation[key])
    after = snapshot('after')
    report = {name: {'originalKeys': len(data), 'retained': data == after[name],
                     'changedKeys': [k for k, v in data.items() if after[name].get(k) != v]}
              for name, data in before.items()}
    (OUT / 'navigation-data-retention.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    if not all(item['retained'] for item in report.values()):
        raise RuntimeError(str(report))
    print(json.dumps(report, indent=2), flush=True)
    text('shell', 'am', 'force-stop', PACKAGE)
    text('shell', 'am', 'start', '-n', PACKAGE + '/.views.MainActivity')
