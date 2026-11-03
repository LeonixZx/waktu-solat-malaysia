"""Physical tablet orientation captures and semantic before/after data verification."""
from local_android import adb_path
from pathlib import Path
import subprocess,json,xml.etree.ElementTree as ET,sys
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'app/build/verification/v31'
ADB=str(adb_path());SERIAL=__import__('os').environ['WSM_DEVICE_SERIAL'];PKG='com.example.waktusolatmalaysia'
def raw(*args):return subprocess.run([ADB,'-s',SERIAL,*map(str,args)],capture_output=True,check=True).stdout
def adb(*args):return raw(*args).decode('utf-8','replace')
def prefs(data):
    return {n.attrib['name']:(n.tag,sorted(x.text or '' for x in n) if n.tag=='set' else n.attrib.get('value',n.text))
            for n in ET.fromstring(data) if not n.attrib['name'].startswith('scheduled_')
            and n.attrib['name'] not in ['alarm_generation','prefetch_day']}
if __name__=='__main__':
    settings={k:adb('shell','settings','get','system',k).strip() for k in ['accelerometer_rotation','user_rotation']}
    try:
        adb('shell','settings','put','system','accelerometer_rotation',0)
        for rotation,label in ([] if '--data-only' in sys.argv else [(0,'portrait'),(1,'landscape')]):
            adb('shell','settings','put','system','user_rotation',rotation)
            result=adb('shell','am','instrument','-w','-e','capture','tablet-'+label,'-e','class',PKG+'.IbadahLayoutTest',PKG+'.test/androidx.test.runner.AndroidJUnitRunner')
            (OUT/('tablet-'+label+'.log')).write_text(result,encoding='utf-8')
            if 'OK (1 test)' not in result:raise RuntimeError(result[-3000:])
            print(label+': PASS',flush=True)
        adb('pull','/sdcard/Android/data/'+PKG+'/files/tablet-verification',OUT/'tablet-previews')
    finally:
        for key in ['user_rotation','accelerometer_rotation']:
            if settings[key]=='null':adb('shell','settings','delete','system',key)
            else:adb('shell','settings','put','system',key,settings[key])
    report={}
    for name in ['waktu_solat','personal_library','widget_config']:
        before=OUT/('before-'+name+'.xml')
        if not before.exists():continue
        after=raw('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml');(OUT/('after-'+name+'.xml')).write_bytes(after)
        old,new=prefs(before.read_bytes()),prefs(after)
        changes=[k for k,v in old.items() if new.get(k)!=v]
        added=[k for k in new if k not in old]
        report[name]={'originalKeys':len(old),'retained':not changes,'changedKeys':changes,'addedKeys':added}
    for name in ['personal_tasks.json','backup-before-import.json']:
        before=OUT/('before-'+name)
        if not before.exists():continue
        after=raw('exec-out','run-as',PKG,'cat','files/'+name);(OUT/('after-'+name)).write_bytes(after)
        try: retained=json.loads(before.read_bytes())==json.loads(after)
        except json.JSONDecodeError: retained=before.read_bytes()==after
        report[name]={'retained':retained}
    (OUT/'data-retention.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    assert all(r['retained'] for r in report.values()),report
    print(json.dumps(report,indent=2),flush=True)
    adb('shell','am','force-stop',PKG);adb('shell','am','start','-n',PKG+'/.views.MainActivity')
