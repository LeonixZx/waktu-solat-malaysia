"""Place our remaining providers through the Xiaomi picker; never touch other providers."""
from local_android import adb_path
from pathlib import Path
import subprocess,xml.etree.ElementTree as ET,re,time,traceback
ADB=str(adb_path());QA=Path(__file__).resolve().parents[1]/'app/build/verification/v32'
def run(*a):return subprocess.run([ADB,'-s',__import__('os').environ['WSM_DEVICE_SERIAL'],*a],capture_output=True).stdout
def dump():
 out=run('shell','uiautomator','dump','/sdcard/v32-ui.xml');assert b'dumped to' in out,out
 return ET.fromstring(run('exec-out','cat','/sdcard/v32-ui.xml'))
def tap(e):
 v=list(map(int,re.findall(r'\d+',e.get('bounds'))));run('shell','input','tap',str((v[0]+v[2])//2),str((v[1]+v[3])//2))
def find(tree,kind,text):return next((e for e in tree.iter('node') if e.get(kind)==text),None)
try:
 run('shell','input','keyevent','KEYCODE_HOME');time.sleep(.8)
 tree=dump()
 if find(tree,'text','Widgets') is None:run('shell','input','keyevent','82');time.sleep(.8)
 for label in ['Prayer strip','Today’s prayers','Daily Plus']:
  tree=dump();button=find(tree,'text','Widgets');assert button is not None,ET.tostring(tree).decode()[-6000:];tap(button)
  for n in range(18):
   tree=dump();choice=find(tree,'content-desc',label)
   print(label+' scroll '+str(n),flush=True)
   if choice is not None:tap(choice);break
   run('shell','input','swipe','1400','2250','1400','650','400')
  else:raise AssertionError('Missing '+label)
  time.sleep(.8);QA.joinpath('launcher-placed-'+re.sub('[^A-Za-z]','',label)+'.png').write_bytes(run('exec-out','screencap','-p'));print('Placed '+label,flush=True)
 tree=dump();done=find(tree,'content-desc','Done')
 if done is not None:tap(done)
 QA.joinpath('launcher-service-placed.txt').write_bytes(run('shell','dumpsys','appwidget'));QA.joinpath('launcher-placed.xml').write_bytes(ET.tostring(dump()))
except Exception:
 QA.joinpath('launcher-error.txt').write_text(traceback.format_exc(),encoding='utf-8');raise
