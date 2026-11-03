"""Reversible emulator-only Ibadah matrix; never changes the owner's tablet settings."""
from pathlib import Path
import subprocess,re,json,sys
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/build/verification/v31'
ADB='C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL='emulator-5554'
def adb(*args):
    r=subprocess.run([ADB,'-s',SERIAL,*map(str,args)],capture_output=True,check=True)
    return r.stdout.decode('utf-8','replace')
def check(label,classes):
    result=adb('shell','am','instrument','-w','-e','capture',label,'-e','class',classes,
               'com.example.waktusolatmalaysia.test/androidx.test.runner.AndroidJUnitRunner')
    (OUT/(label+'.log')).write_text(result,encoding='utf-8')
    if 'OK (' not in result:raise RuntimeError(label+'\n'+result[-4000:])
    print(label+': PASS',flush=True)
if __name__=='__main__':
    original={k:adb('shell','wm',k) for k in ['size','density']}
    font=adb('shell','settings','get','system','font_scale').strip()
    results=[]
    try:
        adb('shell','wm','density',160)
        for scale in [1,1.3,2]:
            adb('shell','settings','put','system','font_scale',scale)
            for width in [320,360,600,800,840,1280]:
                adb('shell','wm','size',f'{width}x1000')
                label=f'android-{width}-font{scale}'
                prior=OUT/(label+'.log')
                if '--resume' not in sys.argv or not prior.exists() or 'OK (' not in prior.read_text(encoding='utf-8'):
                    check(label,'com.example.waktusolatmalaysia.IbadahLayoutTest')
                results.append({'width':width,'height':1000,'fontScale':scale,'passed':True})
        adb('shell','settings','put','system','font_scale',1)
        for width,height,label in [(1280,800,'android-landscape'),(800,600,'android-resized-window'),(840,400,'android-short-window')]:
            adb('shell','wm','size',f'{width}x{height}')
            check(label,'com.example.waktusolatmalaysia.IbadahLayoutTest')
            results.append({'width':width,'height':height,'fontScale':1,'passed':True})
        (OUT/'android-layouts.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    finally:
        for key,value in original.items():
            match=re.search(r'Override '+key+r':\s*(\S+)',value)
            adb('shell','wm',key,match.group(1) if match else 'reset')
        if font=='null':adb('shell','settings','delete','system','font_scale')
        else:adb('shell','settings','put','system','font_scale',font)
        adb('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-verification',str(OUT/'emulator-previews'))
