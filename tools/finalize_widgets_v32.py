"""Verify current personal data/settings and isolate new launcher widgets from test fixtures."""
from local_android import adb_path
from pathlib import Path
import subprocess,json,hashlib,xml.etree.ElementTree as ET,re
ROOT=Path(__file__).resolve().parents[1];Q=ROOT/'app/build/verification/v32';ADB=str(adb_path());PKG='com.example.waktusolatmalaysia'
def run(*args):return subprocess.run([ADB,'-s',__import__('os').environ['WSM_DEVICE_SERIAL'],*args],capture_output=True,check=True,timeout=20).stdout
def prefs(raw):
 try:return {e.get('name'):(e.tag,sorted(x.text or '' for x in e) if e.tag=='set' else e.get('value',e.text)) for e in ET.fromstring(raw) if not e.get('name').startswith('scheduled_') and e.get('name') not in ['alarm_generation','prefetch_day']}
 except ET.ParseError:
  assert b'No such file' in raw,raw;return {}
run('shell','input','keyevent','KEYCODE_BACK')
run('shell','appwidget','revokebind','--package',PKG,'--user','0')
report={}
for name in ['waktu_solat','personal_library','widget_config']:
 raw=run('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml');(Q/('after-'+name+'.xml')).write_bytes(raw);old=prefs((Q/('before-'+name+'.xml')).read_bytes());new=prefs(raw);changed=[k for k,v in old.items() if new.get(k)!=v];report[name]={'originalKeys':len(old),'retained':not changed,'changedKeys':changed,'newKeys':[k for k in new if k not in old]}
for name in ['personal_tasks.json','backup-before-import.json']:
 raw=run('exec-out','run-as',PKG,'cat','files/'+name);(Q/('after-'+name)).write_bytes(raw);old=(Q/('before-'+name)).read_bytes()
 try:same=json.loads(old)==json.loads(raw)
 except json.JSONDecodeError:same=old==raw
 report[name]={'retained':same}
(Q/'data-retention.json').write_text(json.dumps(report,indent=2));assert all(v['retained'] for v in report.values()),report
settings={k:run('shell','settings','get','system',k).decode().strip() for k in ['font_scale','accelerometer_rotation','user_rotation']};before=json.loads((Q/'tablet-settings-before.json').read_text());(Q/'tablet-settings-restored.json').write_text(json.dumps({'before':before,'after':settings,'restored':before==settings},indent=2));assert before==settings
baseline=json.loads((Q/'web-before.json').read_text());changed=[p for p,sha in baseline.items() if hashlib.sha256((ROOT/p).read_bytes()).hexdigest()!=sha];(Q/'pwa-unchanged.json').write_text(json.dumps({'version':'3.1.0','files':len(baseline),'changed':changed,'unchanged':not changed},indent=2));assert not changed
service=run('shell','dumpsys','appwidget').decode('utf-8','replace');(Q/'tablet-widget-service-final.txt').write_text(service,encoding='utf-8');assert PKG not in service.split('Grants:')[-1];widgets=service.split('Widgets:')[1].split('Hosts:')[0];live={re.search(r'id=(\d+)',block).group(1) for block in widgets.split('  [') if PKG in block};stored=set(prefs((Q/'after-widget_config.xml').read_bytes()));assert stored==live,(stored,live);assert len(live)==4
package=run('shell','dumpsys','package',PKG).decode('utf-8','replace');permissions=[line.strip() for line in package.splitlines() if 'android.permission.' in line and ('granted=' in line)];(Q/'tablet-final-permissions.json').write_text(json.dumps(permissions,indent=2));assert any('ACCESS_FINE_LOCATION: granted=false' in p for p in permissions)
path=run('shell','pm','path',PKG).decode().strip().split('package:')[1];installed=run('exec-out','cat',path);sha=hashlib.sha256(installed).hexdigest();assert sha==hashlib.sha256((ROOT/'app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest()
(Q/'tablet-final-state.json').write_text(json.dumps({'version':'3.2.0','versionCode':8,'installedDebugSha256':sha,'placedWidgetIds':sorted(live),'temporaryWidgetBindGrantRevoked':True,'orphanConfigurations':False,'settingsRestored':True,'personalDataRetained':True},indent=2))
run('shell','am','start','-n',PKG+'/.views.MainActivity');print('Final tablet state, exact installed APK, personal data and unchanged PWA verified.')
