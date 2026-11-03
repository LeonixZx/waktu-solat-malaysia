from local_android import adb_path
from pathlib import Path
import subprocess,time,re,json,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'app/build/verification/v3';ADB=str(adb_path());SERIAL=__import__('os').environ['WSM_DEVICE_SERIAL']
def command(*args):return subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True)
def adb(*args):
 r=command(*args)
 if r.returncode:raise RuntimeError(r.stderr.decode('utf-8','replace'))
 return r.stdout.decode('utf-8','replace')
def screenshot(name):
 (OUT/(name+'.png')).write_bytes(command('exec-out','screencap','-p').stdout)
def tree():
 adb('shell','uiautomator','dump','/sdcard/wsm-v3-ui.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/wsm-v3-ui.xml'))
def click(predicate):
 root=tree()
 for node in root.iter('node'):
  if predicate(node.attrib):
   x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.4);return
 raise RuntimeError('Control not found')
def byid(name):click(lambda a:a.get('resource-id','').endswith('/'+name) and a.get('clickable')=='true')
def preferences(raw):
 root=ET.fromstring(raw);return {n.attrib['name']:(n.tag,sorted(c.text for c in n) if n.tag=='set' else n.attrib.get('value',n.text)) for n in root}
if __name__=='__main__':
 adb('shell','input','keyevent','224');adb('shell','wm','dismiss-keyguard');adb('shell','am','start','-n','com.example.waktusolatmalaysia/.views.MainActivity');time.sleep(1)
 results={}
 for file,baseline in [('waktu_solat','baseline-preferences.xml'),('personal_library','baseline-personal.xml')]:
  current=command('exec-out','run-as','com.example.waktusolatmalaysia','cat','shared_prefs/'+file+'.xml').stdout
  (OUT/('tablet-'+file+'.xml')).write_bytes(current)
  old=preferences((OUT/baseline).read_bytes());new=preferences(current)
  results[file]={'originalKeys':len(old),'retained':all(new.get(k)==v for k,v in old.items()),'changedKeys':[k for k,v in old.items() if new.get(k)!=v]}
 report=adb('shell','am','instrument','-w','com.example.waktusolatmalaysia/com.example.waktusolatmalaysia.debug.TabletProbe')
 (OUT/'tablet-inapp-qa.log').write_text(report)
 adb('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-probe.json',str(OUT/'tablet-probe.json'))
 assert json.loads((OUT/'tablet-probe.json').read_text())['allHubsReachable']
 for page in ['home','quranFragment','worshipFragment','exploreFragment','savedFragment']:
  name='tablet-v3-home-final' if page=='home' else 'tablet-v3-'+page
  adb('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-probe-'+page+'.png',str(OUT/(name+'.png')))
 (OUT/'tablet-upgrade.json').write_text(json.dumps(results,indent=2))
 print(json.dumps(results,indent=2))
