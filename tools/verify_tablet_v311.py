"""Patch regression and fresh data comparison on the tablet; no mock locations here."""
from pathlib import Path
import json,sys
import verify_tablet_v31 as common
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v311'
PKG='com.example.waktusolatmalaysia';QA.mkdir(exist_ok=True)
def snapshot(stage):
    for name in ['waktu_solat','personal_library','widget_config']:
        (QA/(stage+'-'+name+'.xml')).write_bytes(common.raw('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml'))
    for name in ['personal_tasks.json','backup-before-import.json']:
        (QA/(stage+'-'+name)).write_bytes(common.raw('exec-out','run-as',PKG,'cat','files/'+name))
def check(label,classes,**arguments):
    args=['shell','am','instrument','-w','-e','class',','.join(PKG+'.'+c for c in classes)]
    for key,value in arguments.items():args+=['-e',key,str(value)]
    out=common.adb(*args,PKG+'.test/androidx.test.runner.AndroidJUnitRunner');(QA/(label+'.log')).write_text(out,encoding='utf-8')
    expected=31 if label=='tablet-suite' else 1
    assert f'OK ({expected} '+('test)' if expected==1 else 'tests)') in out,out[-4000:]
    print(label+': PASS',flush=True)
if __name__=='__main__':
    if not (QA/'before-waktu_solat.xml').exists():snapshot('before')
    settings={k:common.adb('shell','settings','get','system',k).strip() for k in ['accelerometer_rotation','user_rotation']}
    try:
        if '--data-only' not in sys.argv:
            check('tablet-suite',['NativeAppSmokeTest','TabletFeatureTest','HubTouchTest','V3FeaturesTest',
                  'TabletLocationTest#physicalCompassSensorProducesOrientationEvents','IbadahV31Test','LauncherBrandTest','ReadingStateTest','CalendarInteractionTest'],capture='tablet-v311')
            common.adb('shell','settings','put','system','accelerometer_rotation',0)
            for value,label in [(0,'portrait'),(1,'landscape')]:
                common.adb('shell','settings','put','system','user_rotation',value)
                check('tablet-'+label,['IbadahLayoutTest'],capture='tablet-v311-'+label)
    finally:
        for key in ['user_rotation','accelerometer_rotation']:
            common.adb('shell','settings','delete','system',key) if settings[key]=='null' else common.adb('shell','settings','put','system',key,settings[key])
        common.adb('pull','/sdcard/Android/data/'+PKG+'/files/tablet-verification',QA/'tablet-previews')
    snapshot('after');report={}
    for name in ['waktu_solat','personal_library','widget_config']:
        before=common.prefs((QA/('before-'+name+'.xml')).read_bytes());after=common.prefs((QA/('after-'+name+'.xml')).read_bytes())
        changes=[k for k,v in before.items() if after.get(k)!=v]
        report[name]={'originalKeys':len(before),'retained':not changes,'changedKeys':changes,'addedKeys':[k for k in after if k not in before]}
    for name in ['personal_tasks.json','backup-before-import.json']:
        before=(QA/('before-'+name)).read_bytes();after=(QA/('after-'+name)).read_bytes()
        try:same=json.loads(before)==json.loads(after)
        except json.JSONDecodeError:same=before==after
        report[name]={'retained':same}
    (QA/'data-retention.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    assert all(x['retained'] for x in report.values()),report
    print('All current personal-data snapshots retained.',flush=True)
    common.adb('shell','am','force-stop',PKG);common.adb('shell','am','start','-n',PKG+'/.views.MainActivity')
