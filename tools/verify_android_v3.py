"""Reversible emulator checks. All ADB commands require an explicit serial."""
from local_android import adb_path
from pathlib import Path
import subprocess,sys,json
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(str(adb_path()))
SERIAL='emulator-5554'
OUT=ROOT/'app/build/verification/v3';OUT.mkdir(parents=True,exist_ok=True)
def adb(*args):
 r=subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True)
 return (r.stdout+r.stderr).decode('utf-8','replace')
def instrument(cls,label,**arguments):
 args=['shell','am','instrument','-w','-e','class','com.example.waktusolatmalaysia.'+cls]
 for key,value in arguments.items():args+=['-e',key,str(value)]
 args+=['com.example.waktusolatmalaysia.test/androidx.test.runner.AndroidJUnitRunner']
 result=adb(*args);(OUT/(label+'.log')).write_text(result,encoding='utf-8')
 print(label+': '+('PASS' if 'OK (' in result else 'FAIL'),flush=True)
 if 'OK (' not in result:raise RuntimeError(result[-2000:])
if __name__=='__main__':
 original_font=adb('shell','settings','get','system','font_scale').strip()
 try:
  adb('shell','appwidget','grantbind','--package','com.example.waktusolatmalaysia','--user','0')
  adb('push',str(OUT/'web-backup.json'),'/sdcard/Android/data/com.example.waktusolatmalaysia/files/web-backup.json')
  instrument('WidgetHostTest','widget-host',widgets='true')
  adb('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/android-backup.json',str(OUT/'android-backup.json'))
  adb('shell','appwidget','revokebind','--package','com.example.waktusolatmalaysia','--user','0')
  adb('shell','wm','density','160')
  for scale in [1,1.3,2]:
   adb('shell','settings','put','system','font_scale',str(scale))
   for width in [320,360,600,840,1280]:
    adb('shell','wm','size',str(width)+'x1000')
    label=f'android-{width}-text{scale}'
    instrument('AdaptiveProbeTest',label,capture=label)
 finally:
  adb('shell','wm','size','reset');adb('shell','wm','density','reset')
  if original_font=='null':adb('shell','settings','delete','system','font_scale')
  else:adb('shell','settings','put','system','font_scale',original_font)
  adb('shell','appwidget','revokebind','--package','com.example.waktusolatmalaysia','--user','0')
  print(adb('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-verification',str(OUT/'emulator-previews')),flush=True)
