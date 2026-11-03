"""Package the Android-only location refresh patch; retain the PWA 3.1.0 artifact."""
from pathlib import Path
import hashlib,json,shutil,subprocess,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v311';OUT=ROOT/'deliverables'
def read(path):return path.read_text(encoding='utf-8-sig',errors='replace')
assert 'BUILD SUCCESSFUL' in read(QA/'native-build.log')
assert 'BUILD SUCCESSFUL' in read(QA/'test-build.log')
assert 'Live location did not update bearing' in read(QA/'location-baseline-failure.log')
for name,count in [('location-updates',3),('tablet-suite',31),('tablet-portrait',1),('tablet-landscape',1),('android-320-text200',1)]:
    assert f'OK ({count} '+('test)' if count==1 else 'tests)') in read(QA/(name+'.log')),name
assert all(x['retained'] for x in json.loads(read(QA/'data-retention.json')).values())
display=json.loads(read(QA/'display-restored.json'));assert display['before']==display['after'] and display['fontBefore']==display['fontAfter']
restored=json.loads(read(QA/'location-restored.json'));assert restored['permissionsBefore']==restored['permissionsAfter'] and restored['locationEnabledBefore']==restored['locationEnabledAfter']
assert json.loads(read(QA/'final-device-state.json'))['restored']
tests=0
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    x=ET.parse(p).getroot().attrib;tests+=int(x['tests']);assert x.get('failures','0')=='0' and x.get('errors','0')=='0'
assert tests==41
aapt='C:/Users/USER/AppData/Local/Android/Sdk/build-tools/34.0.0/aapt.exe';artifacts=[]
for variant,relative in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
    p=ROOT/'app/build/outputs/apk'/relative
    meta=subprocess.run([aapt,'dump','badging',str(p)],capture_output=True,check=True).stdout.decode('utf-8','replace')
    assert "versionName='3.1.1'" in meta and "versionCode='7'" in meta and "name='com.example.waktusolatmalaysia'" in meta
    target=OUT/f'Waktu-Solat-Malaysia-v3.1.1-{variant}.apk';shutil.copy2(p,target);artifacts.append(target)
records=[{'file':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(OUT.iterdir()) if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
(QA/'delivery-metrics.json').write_text(json.dumps({'androidVersion':'3.1.1','pwaVersion':'3.1.0','nativeUnitTests':tests,'tabletChecks':31,'locationRegressionChecks':3,'physicalCompassAccuracyVerified':False,'artifacts':[x for x in records if 'v3.1.1' in x['file']]},indent=2),encoding='utf-8')
print(json.dumps([x for x in records if 'v3.1.1' in x['file']],indent=2))
