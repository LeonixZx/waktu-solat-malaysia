"""Explicit-device upgrade and regression checks; no data clear, uninstall or settings overrides."""
from pathlib import Path
import hashlib, json, subprocess, sys, tarfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v332-phone'
ADB=r'C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PKG='com.example.waktusolatmalaysia'
SERIAL=sys.argv[1]
QA.mkdir(exist_ok=True)
def call(*args,check=True,timeout=30):
    p=subprocess.run([ADB,'-s',SERIAL,*map(str,args)],capture_output=True,timeout=timeout)
    if check and p.returncode:raise RuntimeError((p.stdout+p.stderr).decode('utf-8','replace'))
    return p
def text(*args,**kw):return call(*args,**kw).stdout.decode('utf-8','replace')
apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
digest=hashlib.sha256(apk.read_bytes()).hexdigest()
verify_only='--verify-only' in sys.argv
if not verify_only:
    for label,path in [('app',apk),('test',ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')]:
        p=call('install','-r',path,check=False,timeout=120)
        (QA/('final-install-'+label+'.log')).write_bytes(p.stdout+p.stderr)
        print((p.stdout+p.stderr).decode('utf-8','replace'),flush=True)
        assert b'Success'in p.stdout
    service=text('shell','dumpsys','appwidget')
    had_grant='Grants:'in service and PKG in service.split('Grants:')[-1]
    classes=json.loads((ROOT/'testing/android-emulators.json').read_text(encoding='utf-8'))['fullClasses']
    before_settings={k:text('shell','settings','get',scope,k).strip() for scope,k in [('system','font_scale'),('system','accelerometer_rotation'),('system','user_rotation'),('global','window_animation_scale'),('global','transition_animation_scale'),('global','animator_duration_scale')]}
    try:
        if not had_grant:call('shell','appwidget','grantbind','--package',PKG,'--user','0')
        result=text('shell','am','instrument','-w','-e','widgets','true','-e','listener',PKG+'.KeepAwakeListener','-e','class',','.join(classes),PKG+'.test/androidx.test.runner.AndroidJUnitRunner',timeout=1200)
        (QA/'native-tests.log').write_text(result,encoding='utf-8')
        print(result[-4500:],flush=True)
        assert 'OK (25 tests)'in result
    finally:
        if not had_grant:call('shell','appwidget','revokebind','--package',PKG,'--user','0',check=False)
        for folder in ['ui-v330','tablet-verification','system-bar-check','widget-v331','widget-v332']:
            call('pull',f'/sdcard/Android/data/{PKG}/files/{folder}',QA/folder,check=False,timeout=60)
else:
    original_service=(QA/'before-widgets.txt').read_text(encoding='utf-8')
    had_grant='Grants:' in original_service and PKG in original_service.split('Grants:')[-1]
    assert 'OK (25 tests)' in (QA/'native-tests.log').read_text(encoding='utf-8')
    before_settings=json.loads((QA/'baseline.json').read_text(encoding='utf-8'))['settings']
after_settings={k:text('shell','settings','get',scope,k).strip() for scope,k in [('system','font_scale'),('system','accelerometer_rotation'),('system','user_rotation'),('global','window_animation_scale'),('global','transition_animation_scale'),('global','animator_duration_scale')]}
def prefs(raw):
    if raw.strip().startswith(b'cat:') and b'No such file' in raw:return {}
    return {e.get('name'):(e.tag,sorted(x.text or '' for x in e) if e.tag=='set' else e.get('value',e.text)) for e in ET.fromstring(raw)if not e.get('name','').startswith('scheduled_')and e.get('name')not in ['alarm_generation','prefetch_day']}
retention={}
for name in ['waktu_solat','personal_library']:
    raw=call('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml').stdout
    (QA/('after-'+name+'.xml')).write_bytes(raw)
    old=prefs((QA/('preinstall-'+name+'.xml')).read_bytes());new=prefs(raw)
    changed=[k for k,v in old.items()if new.get(k)!=v]
    retention[name]={'retained':not changed,'originalKeys':len(old),'changedKeys':changed,'newKeys':[k for k in new if k not in old]}
path=text('shell','pm','path',PKG).strip().split('package:')[1]
installed=text('shell','sha256sum',path).split()[0]
service=text('shell','dumpsys','appwidget');(QA/'after-widgets.txt').write_text(service,encoding='utf-8')
allocation=json.loads(text('shell','cat',f'/sdcard/Android/data/{PKG}/files/widget-allocation.json'))
live_ids=sorted(str(w['id']) for w in allocation['widgets'])
raw=call('exec-out','run-as',PKG,'cat','shared_prefs/widget_config.xml').stdout
(QA/'after-widget_config.xml').write_bytes(raw)
old_widgets=prefs((QA/'preinstall-widget_config.xml').read_bytes());current_widgets=prefs(raw)
user_replaced='--user-replaced-widgets' in sys.argv
widget_changes=[k for k,v in old_widgets.items() if current_widgets.get(k)!=v]
widget_report={'beforeIds':sorted(old_widgets),'currentIds':live_ids,'configuredIds':sorted(current_widgets),
    'changedIds':widget_changes,'sameInstanceSettingsRetained':not widget_changes,
    'userConfirmedRemovalAndReaddition':user_replaced,
    'placementObservation':'User confirmed removing/re-adding widgets during comparison. IDs 69/70 are new placements; 70 followed the requested launcher pin prompt.' if user_replaced else 'No manual replacement recorded.',
    'currentConfigurationsMatchPlacedWidgets':sorted(current_widgets)==live_ids}
file_retention={}
with tarfile.open(QA/'private-app-data-before-upgrade.tar') as archive:
    baseline_files={m.name:archive.extractfile(m).read() for m in archive.getmembers() if m.isfile() and (m.name.startswith('files/prayer_times/') or m.name.startswith('files/quran/') or m.name=='files/personal_tasks.json')}
    for name,old in baseline_files.items():
        current=call('exec-out','run-as',PKG,'cat',name).stdout
        record={'retained':current==old,'byteIdentical':current==old,'beforeSha256':hashlib.sha256(old).hexdigest(),'afterSha256':hashlib.sha256(current).hexdigest()}
        if name.startswith('files/prayer_times/'):
            before=json.loads(old);after=json.loads(current)
            same_schedule={k:v for k,v in before.items() if k!='serverTime'}=={k:v for k,v in after.items() if k!='serverTime'}
            record.update(retained=same_schedule,scheduleUnchanged=same_schedule,days=len(after.get('prayerTime',[])),serverTimestampRefreshed=before.get('serverTime')!=after.get('serverTime'))
        file_retention[name]=record
    for name in ['files/personal_tasks.json','files/backup-before-import.json']:
        if name not in archive.getnames():
            current=call('exec-out','run-as',PKG,'cat',name,check=False).stdout
            absent=current.strip().startswith(b'cat:') and b'No such file' in current
            file_retention[name]={'absentBeforeAndAfter':absent}
report={'serial':SERIAL,'version':'3.3.2','versionCode':13,'tests':25,'passed':True,'apkSha256':digest,'installedApkMatches':installed==digest,
    'settingsUnchanged':before_settings==after_settings,'personalData':retention,'persistentFiles':file_retention,'widgetLifecycle':widget_report,
    'widgetBindPermissionRestored':had_grant or PKG not in service.split('Grants:')[-1]}
(QA/'verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
assert report['installedApkMatches']and report['settingsUnchanged']and report['widgetBindPermissionRestored']
assert all(x['retained']for x in retention.values()),retention
assert all(x.get('retained',x.get('absentBeforeAndAfter',False)) for x in file_retention.values()),file_retention
assert widget_report['currentConfigurationsMatchPlacedWidgets'],widget_report
assert not widget_changes or user_replaced,widget_report
if user_replaced:
    assert not set(widget_changes).intersection(live_ids), 'A still-placed baseline widget changed unexpectedly'
print(json.dumps(report,indent=2),flush=True)
