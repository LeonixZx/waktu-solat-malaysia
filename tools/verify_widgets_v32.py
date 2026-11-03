"""Own-package host tests; keep existing widget IDs and personal records intact."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v32';ADB=r'C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe'
serial=sys.argv[1];label=sys.argv[2]
def run(*args):return subprocess.run([ADB,'-s',serial,*map(str,args)],capture_output=True,text=True,encoding='utf-8',errors='replace').stdout
for apk in ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
 result=run('install','-r',ROOT/apk);assert 'Success' in result,result
print('Installed '+serial,flush=True)
run('shell','appwidget','grantbind','--package','com.example.waktusolatmalaysia','--user','0')
out=run('shell','am','instrument','-w','-e','widgets','true','-e','capture',label,'-e','class','com.example.waktusolatmalaysia.WidgetV32Test','com.example.waktusolatmalaysia.test/androidx.test.runner.AndroidJUnitRunner')
(QA/(label+'.log')).write_text(out,encoding='utf-8');print(out[-6000:],flush=True)
run('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/tablet-verification',QA/(label+'-screenshots'))
assert 'OK (9 tests)' in out
