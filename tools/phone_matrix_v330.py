"""Additional phone window/text checks; restores emulator display settings in finally."""
from local_android import adb_path
from pathlib import Path
import subprocess
import json

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v330'
ADB=str(adb_path())
SERIAL='emulator-5554'

def run(*args):
    return subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True,check=True,encoding='utf-8',errors='replace').stdout

prefix='com.example.waktusolatmalaysia.'
classes=','.join(prefix+'PhoneUiV330Test#'+name for name in [
    'screenshotAffectedHubsInBothThemesAndRetainFilters',
    'choicesWrapAtPhoneWidthsWithoutClipping',
    'settingsFloatsOverHubAndThemeSurvivesRecreation'])
results=[]
try:
    for label,pixels,scale in [
        ('phone360-130','945x2100','1.3'),
        ('phone360-200','945x2100','2.0'),
        ('landscape720-100','1890x945','1.0')]:
        run('shell','wm','size',pixels)
        run('shell','settings','put','system','font_scale',scale)
        text=run('shell','am','instrument','-w','-e','class',classes,prefix+'test/androidx.test.runner.AndroidJUnitRunner')
        (QA/(label+'.log')).write_text(text,encoding='utf-8')
        print(label+': '+text[-250:],flush=True)
        run('pull','/sdcard/Android/data/com.example.waktusolatmalaysia/files/ui-v330',str(QA/('ui-'+label)))
        assert 'OK (3 tests)' in text,label
        results.append({'label':label,'size':pixels,'density':420,'fontScale':scale,'tests':3,'passed':True})
    run('shell','wm','size','945x2100')
    run('shell','settings','put','system','font_scale','1.0')
    legacy=','.join([prefix+'IbadahV31Test#collectionSelectionSearchAndReaderSurviveRecreation',prefix+'IbadahV31Test#everySuppliedReadingRendersAllItsSectionsOffline']+
        [prefix+'NativeAppSmokeTest#'+name for name in ['settingsPersistAcrossActivityRecreation','bottomNavigationOpensQiblaAndPreferences','nightModeSurvivesRecreation']])
    text=run('shell','am','instrument','-w','-e','class',legacy,prefix+'test/androidx.test.runner.AndroidJUnitRunner')
    (QA/'phone-regression.log').write_text(text,encoding='utf-8')
    print(text[-3000:],flush=True)
    assert 'OK (5 tests)' in text
finally:
    run('shell','wm','size','reset')
    run('shell','settings','put','system','font_scale','1.0')
    (QA/'phone-window-matrix.json').write_text(json.dumps({'results':results,'restored':True,'restoredPhysicalSize':'1080x2400','restoredDensity':420,'restoredFontScale':1.0},indent=2)+'\n',encoding='utf-8')
