"""Read-only public build prerequisites check; intentionally avoids private paths/IDs."""
from pathlib import Path
import argparse
import os
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def version(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=15)
        return result.stdout + result.stderr if result.returncode == 0 else ''
    except (OSError, subprocess.TimeoutExpired):
        return ''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--web', action='store_true', help='Also require Node.js 24+')
    args = parser.parse_args()
    checks = []

    def check(label, passed, remedy):
        checks.append(passed)
        print(('PASS ' if passed else 'FIX  ') + label + ('' if passed else ': ' + remedy))

    java_home = os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if java_home else shutil.which('java')
    check('JDK 21', bool(java and re.search(r'version "21(?:\.|\")', version([java, '-version']))), 'Install JDK 21 and set JAVA_HOME.')
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    props = ROOT / 'local.properties'
    if not sdk and props.exists():
        match = re.search(r'^sdk\.dir\s*=\s*(.+)$', props.read_text(encoding='utf-8-sig'), re.M)
        if match:
            sdk = match[1].strip().replace('\\:', ':').replace('\\\\', '\\')
    if not sdk:
        options = [Path.home() / 'AppData/Local/Android/Sdk', Path.home() / 'Library/Android/sdk', Path.home() / 'Android/Sdk']
        sdk = next((str(p) for p in options if p.is_dir()), None)
    sdk_path = Path(sdk) if sdk else None
    for label, relative in [('SDK Platform 34', 'platforms/android-34/android.jar'), ('Build Tools 34.0.0', 'build-tools/34.0.0/source.properties'), ('Platform Tools', 'platform-tools/adb.exe' if os.name == 'nt' else 'platform-tools/adb')]:
        check(label, bool(sdk_path and (sdk_path / relative).is_file()), 'Install with SDK Manager; configure ANDROID_HOME or local.properties.')
    check('Included Gradle wrapper', all((ROOT / n).is_file() for n in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar', 'gradle/wrapper/gradle-wrapper.properties']), 'Restore the wrapper from Git.')
    if args.web:
        node = shutil.which('node')
        match = re.match(r'v(\d+)', version([node, '--version'])) if node else None
        check('Node.js 24+', bool(match and int(match[1]) >= 24), 'Install Node.js 24.')
    print('Ready to build.' if all(checks) else 'See CLONE_AND_BUILD.md. No settings were changed.')
    return 0 if all(checks) else 1


if __name__ == '__main__':
    raise SystemExit(main())
