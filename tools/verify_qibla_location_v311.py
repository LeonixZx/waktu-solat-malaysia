"""Real LocationManager regression on the emulator, never on the user's tablet."""
from pathlib import Path
import subprocess,json,re,sys
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v311'
ADB='C:/Users/USER/AppData/Local/Android/Sdk/platform-tools/adb.exe';PKG='com.example.waktusolatmalaysia'
def adb(*args):
    p=subprocess.run([ADB,'-s','emulator-5554',*map(str,args)],capture_output=True,check=True)
    return (p.stdout+p.stderr).decode('utf-8','replace')
def permissions():
    return {name:next(x.strip() for x in adb('shell','dumpsys','package',PKG).splitlines() if name+': granted=' in x)
      for name in ['android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_COARSE_LOCATION']}
if __name__=='__main__':
    baseline='--baseline' in sys.argv
    before=permissions();enabled=adb('shell','cmd','location','is-location-enabled').strip()
    op=adb('shell','appops','get',PKG,'android:mock_location');match=re.search(r'MOCK_LOCATION:\s*(\w+)',op);mode=match.group(1) if match else 'default'
    try:
        adb('shell','cmd','location','set-location-enabled','true')
        for name in before:adb('shell','pm','grant',PKG,name)
        adb('shell','appops','set',PKG,'android:mock_location','allow')
        cls=PKG+'.QiblaLocationUpdatesTest'+('#cachedFixDoesNotCancelSubsequentLiveUpdates' if baseline else '')
        result=adb('shell','am','instrument','-w','-e','class',cls,'-e','locationUpdates','true',PKG+'.test/androidx.test.runner.AndroidJUnitRunner')
        label='location-baseline-failure' if baseline else 'location-updates'
        (QA/(label+'.log')).write_text(result,encoding='utf-8');print(result[-5000:],flush=True)
        if baseline:assert 'FAILURES!!!' in result and 'Live location did not update bearing' in result,'Expected old-build regression did not reproduce'
        else:assert 'OK (3 tests)' in result
    finally:
        for name,line in before.items():
            adb('shell','pm','grant' if 'granted=true' in line else 'revoke',PKG,name)
            for flag,label in [('user-set','USER_SET'),('user-fixed','USER_FIXED')]:
                adb('shell','pm','set-permission-flags' if label in line else 'clear-permission-flags',PKG,name,flag)
        adb('shell','appops','set',PKG,'android:mock_location',mode)
        adb('shell','cmd','location','set-location-enabled',enabled)
        after=permissions();assert before==after,(before,after)
        (QA/('location-restored-baseline.json' if baseline else 'location-restored.json')).write_text(json.dumps({'permissionsBefore':before,'permissionsAfter':after,'locationEnabledBefore':enabled,'locationEnabledAfter':adb('shell','cmd','location','is-location-enabled').strip(),'mockLocationModeRestored':mode},indent=2),encoding='utf-8')
