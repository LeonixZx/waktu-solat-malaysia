"""Dedicated WSM AVD creation and repeatable emulator-only Android instrumentation.

Never targets a physical device or modifies another AVD. No data wipes or forced AVD overwrites.
"""
from pathlib import Path
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
CONFIG = json.loads((ROOT / 'testing/android-emulators.json').read_text(encoding='utf-8'))
SDK = Path(CONFIG['sdk'])
ADB = SDK / 'platform-tools/adb.exe'
EMULATOR = SDK / 'emulator/emulator.exe'
MANAGER = SDK / 'cmdline-tools/cmdline-tools/bin/avdmanager.bat'
AVDS = Path(os.environ['USERPROFILE']) / '.android/avd'
QA = ROOT / 'app/build/verification/emulator-matrix'
QA.mkdir(parents=True, exist_ok=True)
ENV = dict(os.environ, JAVA_HOME=CONFIG['jdk'])


def command(args, *, timeout=60, check=True, input=None):
    result = subprocess.run([str(a) for a in args], input=input, capture_output=True,
                            text=True, encoding='utf-8', errors='replace', env=ENV, timeout=timeout)
    if check and result.returncode:
        raise RuntimeError(result.stdout[-2000:] + result.stderr[-2000:])
    return result.stdout


def adb(serial, *args, **options):
    assert serial.startswith('emulator-'), 'This runner only targets emulators'
    return command([ADB, '-s', serial, *args], **options)


def create(key):
    profile = CONFIG['profiles'][key]
    name = profile['avd']
    target = AVDS / (name + '.avd')
    if profile.get('managed') is False:
        assert name in CONFIG['existingCompatibilityAvds']
        config = target / 'config.ini'
        assert config.exists(), f'Missing compatibility AVD: {name}'
        assert f"android-{profile['api']}" in config.read_text(encoding='utf-8')
        print(f'Ready: {key} / {name} (existing configuration retained)', flush=True)
        return
    created = False
    if not (AVDS / (name + '.ini')).exists():
        image = f"system-images;android-{profile['api']};google_apis;x86_64"
        assert (SDK / f"system-images/android-{profile['api']}/google_apis/x86_64/package.xml").exists(), f'Install SDK image first: {image}'
        output = command([MANAGER, 'create', 'avd', '-n', name, '-k', image, '-d', profile['device']], input='no\n', timeout=120)
        (QA / f'{key}-create.log').write_text(output, encoding='utf-8')
        created = True
    config = target / 'config.ini'
    existing = {}
    for line in config.read_text(encoding='utf-8').splitlines():
        if '=' in line:
            k, v = line.split('=', 1); existing[k.strip()] = v.strip()
    marker = target / 'wsm-project.json'
    requested = {'workspace': str(ROOT), 'profile': key}
    if marker.exists():
        assert json.loads(marker.read_text(encoding='utf-8')) == requested
    else:
        # A just-created AVD is ours; never take over a pre-existing unmarked device.
        assert created, f'Unmarked AVD already exists: {name}'
    existing.update({
        'hw.lcd.width': str(profile['width']), 'hw.lcd.height': str(profile['height']),
        'hw.lcd.density': str(profile['density']), 'hw.initialOrientation': profile['orientation'],
        'hw.keyboard': 'yes' if profile['keyboard'] else 'no', 'hw.ramSize': '4096M',
        'hw.cpu.ncore': '4', 'hw.gpu.enabled': 'yes', 'hw.gpu.mode': 'auto',
        'disk.dataPartition.size': '4G', 'showDeviceFrame': 'no',
        'fastboot.forceColdBoot': 'yes', 'fastboot.forceFastBoot': 'no',
        'firstboot.saveToLocalSnapshot': 'no'})
    config.write_text('\n'.join(f'{k} = {v}' for k, v in existing.items()) + '\n', encoding='utf-8')
    marker.write_text(json.dumps(requested, indent=2) + '\n', encoding='utf-8')
    print(f"Ready: {key} / {name} / API{profile['api']} / {profile['width']*160/profile['density']:.0f}dp wide", flush=True)


def health(serial, evidence, stage, recover=False):
    """Reject system ANR dialogs even when native view assertions happen to pass."""
    for attempt in range(6 if recover else 1):
        adb(serial, 'shell', 'rm', '-f', '/sdcard/wsm-health.xml')
        dumped = adb(serial, 'shell', 'uiautomator', 'dump', '--compressed', '/sdcard/wsm-health.xml', timeout=45,check=False)
        xml = adb(serial, 'shell', 'cat', '/sdcard/wsm-health.xml',check=False)
        if '<hierarchy' not in xml:
            (evidence / f'health-{stage}-not-ready.txt').write_text(dumped+'\n'+xml,encoding='utf-8')
            if recover: time.sleep(5);continue
            return False
        (evidence / f'health-{stage}.xml').write_text(xml, encoding='utf-8')
        tree = ET.fromstring(xml)
        labels = [n.get('text', '') for n in tree.iter('node')]
        error = any("isn't responding" in s or 'keeps stopping' in s for s in labels)
        if not error: return True
        if recover and any('System UI' in s for s in labels):
            wait = next((n for n in tree.iter('node') if n.get('text') == 'Wait'), None)
            if wait is not None:
                x1,y1,x2,y2 = map(int,re.findall(r'\d+',wait.get('bounds')))
                adb(serial,'shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
                time.sleep(5)
                continue
        return False
    return False


def run_profile(key, suite, port, gpu):
    profile = CONFIG['profiles'][key]
    create(key)
    serial = f'emulator-{port}'
    devices = command([ADB, 'devices'])
    assert serial not in devices, f'Port already in use: {serial}'
    evidence = QA / key
    evidence.mkdir(exist_ok=True)
    started = datetime.now(timezone.utc).isoformat()
    initial = {'profile': profile, 'suite': suite, 'passed': False,
               'status': 'running', 'startedAt': started, 'physicalDeviceTouched': False}
    for name in ['result.json', f'{suite}-result.json']:
        (evidence / name).write_text(json.dumps(initial, indent=2) + '\n', encoding='utf-8')
    # Hide both the emulator and Windows helper process. Keep no task-specific global settings.
    start = subprocess.STARTUPINFO()
    start.dwFlags |= subprocess.STARTF_USESHOWWINDOW
    start.wShowWindow = 0
    with (evidence / 'boot.stdout.log').open('w', encoding='utf-8') as out, (evidence / 'boot.stderr.log').open('w', encoding='utf-8') as err:
        process = subprocess.Popen([str(EMULATOR), '-avd', profile['avd'], '-port', str(port),
            '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot-load', '-no-snapshot-save',
            '-gpu', gpu], stdout=out, stderr=err, env=ENV, startupinfo=start)
        animation_before = {}
        try:
            deadline = time.monotonic() + 240
            while True:
                if process.poll() is not None: raise RuntimeError(f'Emulator exited: {key}')
                try: booted = adb(serial, 'shell', 'getprop', 'sys.boot_completed', check=False, timeout=10).strip() == '1'
                except subprocess.TimeoutExpired: booted = False
                if booted: break
                assert time.monotonic() < deadline, f'Boot timed out: {key}'
                time.sleep(2)
            # Verify serial ownership before any installation/settings/test operation.
            assert profile['avd'] in adb(serial, 'emu', 'avd', 'name')
            adb(serial, 'shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
            adb(serial, 'shell', 'wm', 'dismiss-keyguard', check=False)
            for setting in ['window_animation_scale','transition_animation_scale','animator_duration_scale']:
                animation_before[setting]=adb(serial,'shell','settings','get','global',setting).strip()
                adb(serial,'shell','settings','put','global',setting,'0')
            time.sleep(10)
            assert health(serial,evidence,'before',recover=True), f'Unhealthy emulator System UI: {key}'
            for relative in ['debug/app-debug.apk', 'androidTest/debug/app-debug-androidTest.apk']:
                path = ROOT / 'app/build/outputs/apk' / relative
                assert path.exists(), 'Build the app/test APKs first'
                assert 'Success' in adb(serial, 'install', '-r', path, timeout=120)
            app = CONFIG['applicationId']
            if suite in ('full', 'timeline', 'preview'): adb(serial, 'shell', 'appwidget', 'grantbind', '--package', app, '--user', '0')
            adb(serial, 'logcat', '-c')
            classes = ['com.example.waktusolatmalaysia.WidgetDatesV333Test'] if suite == 'dates' else ['com.example.waktusolatmalaysia.WidgetPreviewV332Test'] if suite == 'preview' else ['com.example.waktusolatmalaysia.WidgetTimelineV331Test'] if suite == 'timeline' else ['com.example.waktusolatmalaysia.SystemBarAppearanceTest'] if suite == 'bars' else CONFIG['fullClasses' if suite == 'full' else 'smokeClasses']
            result = adb(serial, 'shell', 'am', 'instrument', '-w', '-e', 'widgets', 'true',
                         '-e', 'capture', key, '-e', 'class', ','.join(classes),
                         app + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=1200)
            (evidence / f'{suite}.log').write_text(result, encoding='utf-8')
            (evidence / f'{suite}-logcat.txt').write_text(adb(serial, 'logcat', '-d', '-v', 'threadtime', timeout=60), encoding='utf-8')
            (evidence / f'{suite}-crash.txt').write_text(adb(serial, 'logcat', '-b', 'crash', '-d', timeout=60), encoding='utf-8')
            expected = CONFIG.get('fullTestCount',18) if suite == 'full' else 3 if suite == 'preview' else 4 if suite in ('dates','timeline') else 1 if suite == 'bars' else 5
            native_passed = f"OK ({expected} {'test' if expected == 1 else 'tests'})" in result
            healthy = health(serial,evidence,'after')
            passed = native_passed and healthy
            for folder in ['ui-v330', 'tablet-verification', 'system-bar-check', 'widget-v331', 'widget-v332', 'widget-v333']:
                adb(serial, 'pull', f'/sdcard/Android/data/{app}/files/{folder}', evidence / folder, timeout=60, check=False)
            source = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
            metadata = {'profile': profile, 'suite': suite, 'passed': passed,
                'status': 'completed', 'startedAt': started,
                'completedAt': datetime.now(timezone.utc).isoformat(),
                'tests': expected, 'apkSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                'testApkSha256': hashlib.sha256((ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk').read_bytes()).hexdigest(),
                'reportedApi': adb(serial, 'shell', 'getprop', 'ro.build.version.sdk').strip(),
                'reportedSize': adb(serial, 'shell', 'wm', 'size').strip(),
                'reportedDensity': adb(serial, 'shell', 'wm', 'density').strip(),
                'nativeAssertionsPassed': native_passed, 'systemUiHealthy': healthy,
                'animationsDuringTest': 0,
                'gpuMode': gpu,
                'wipeDataRequested': False, 'physicalDeviceTouched': False}
            (evidence / 'result.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
            (evidence / f'{suite}-result.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
            print(key + ': ' + result[-500:], flush=True)
            assert passed, f'Test failure: {key}; see {evidence}'
        except Exception as error:
            current = json.loads((evidence / f'{suite}-result.json').read_text(encoding='utf-8'))
            current.update(passed=False, status='failed', error=str(error),
                           completedAt=datetime.now(timezone.utc).isoformat())
            for name in ['result.json', f'{suite}-result.json']:
                (evidence / name).write_text(json.dumps(current, indent=2) + '\n', encoding='utf-8')
            raise
        finally:
            for setting,value in animation_before.items():
                try:
                    adb(serial,'shell','settings','delete','global',setting,check=False,timeout=10) if value=='null' else adb(serial,'shell','settings','put','global',setting,value,check=False,timeout=10)
                except subprocess.TimeoutExpired: pass
            try:
                adb(serial, 'shell', 'appwidget', 'revokebind', '--package', CONFIG['applicationId'], '--user', '0', check=False, timeout=10)
            except subprocess.TimeoutExpired: pass
            try: adb(serial, 'emu', 'kill', check=False, timeout=10)
            except subprocess.TimeoutExpired: pass
            try: process.wait(timeout=30)
            except subprocess.TimeoutExpired: process.terminate()
            # ADB can briefly retain an offline serial after the owned process exits.
            deadline = time.monotonic() + 30
            while serial in command([ADB, 'devices']) and time.monotonic() < deadline:
                time.sleep(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['create', 'run'])
    parser.add_argument('--profiles', nargs='+', choices=CONFIG['profiles'], default=list(CONFIG['profiles']))
    parser.add_argument('--suite', choices=['smoke', 'full', 'bars', 'timeline', 'preview', 'dates'], default='smoke')
    parser.add_argument('--port', type=int, default=5560)
    parser.add_argument('--gpu', choices=['auto', 'host', 'software', 'swiftshader', 'swiftshader_indirect', 'swangle'], default=None)
    args = parser.parse_args()
    assert 5554 <= args.port <= 5682 and args.port % 2 == 0
    for key in args.profiles:
        if args.action == 'create': create(key)
        else: run_profile(key, args.suite, args.port, args.gpu or CONFIG['profiles'][key].get('gpu','auto'))
