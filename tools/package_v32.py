"""Package Android-only v3.2.0; exclude personal snapshots and preserve older deliverables."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v32';OUT=ROOT/'deliverables';REVIEW=OUT/'v3.2.0'
def read(p):return p.read_text(encoding='utf-8-sig',errors='replace')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert 'BUILD SUCCESSFUL' in read(QA/'native-build.log')
assert 'BUILD SUCCESSFUL' in read(QA/'test-build.log')
for label in ['emulator-24-widgets','emulator-30-widgets','emulator-31-widgets','emulator-34-widgets','tablet-widgets']:assert 'OK (9 tests)' in read(QA/(label+'.log')),label
for label,count in [('tablet-regression',31),('upgrade-before',1),('upgrade-after',1)]:assert f'OK ({count} '+('test)' if count==1 else 'tests)') in read(QA/(label+'.log')),label
assert all(x['retained'] for x in json.loads(read(QA/'data-retention.json')).values())
assert json.loads(read(QA/'pwa-unchanged.json'))['unchanged']
assert json.loads(read(QA/'tablet-settings-restored.json'))['restored']
final=json.loads(read(QA/'tablet-final-state.json'));assert final['personalDataRetained'] and not final['orphanConfigurations']
units=0
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
 x=ET.parse(p).getroot().attrib;units+=int(x['tests']);assert int(x.get('failures','0'))==int(x.get('errors','0'))==0
assert units==41
lint=read(ROOT/'app/build/reports/lint-results-debug.txt');assert '0 errors,' in lint
old=json.loads(read(OUT/'SHA256SUMS.json'))
for item in old:
 p=OUT/item['file'];assert sha(p)==item['sha256'],'An older deliverable changed: '+p.name
artifacts=[];aapt=r'C:/Users/USER/AppData/Local/Android/Sdk/build-tools/34.0.0/aapt.exe'
for variant,relative in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
 p=ROOT/'app/build/outputs/apk'/relative;meta=subprocess.run([aapt,'dump','badging',str(p)],capture_output=True,check=True).stdout.decode('utf-8','replace');assert "name='com.example.waktusolatmalaysia'" in meta and "versionName='3.2.0'" in meta and "versionCode='8'" in meta;assert "sdkVersion:'24'" in meta and "targetSdkVersion:'34'" in meta
 target=OUT/f'Waktu-Solat-Malaysia-v3.2.0-{variant}.apk';shutil.copy2(p,target);artifacts.append(target)
assert sha(artifacts[0])==final['installedDebugSha256']
for name in ['DESIGN_WIDGETS_V32.md','VERIFICATION_V32.md']:shutil.copy2(ROOT/name,REVIEW/name)
metadata={'androidVersion':'3.2.0','versionCode':8,'applicationId':'com.example.waktusolatmalaysia','pwaVersion':'3.1.0','unitTests':units,'widgetTestsPerEnvironment':9,'environments':['API24 emulator','API30 emulator','API31 emulator','API34 emulator','Xiaomi API36 tablet'],'measuredLayoutCasesPerEnvironment':576,'tabletRegressionTests':31,'upgradePassed':True,'physicalPlacedWidgetIds':final['placedWidgetIds'],'pwaSourceFilesUnchanged':53,'rebootVerified':False,'manualChecks':['Xiaomi drag-handle resizing','Spoken TalkBack/physical keyboard review','Reboot survival; emulator checkpoint invalidated during recovery'],'artifacts':[{'file':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in artifacts]}
(REVIEW/'artifact-manifest.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
archive=OUT/'Waktu-Solat-Malaysia-v3.2.0-delivery.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in artifacts:z.write(p,p.name)
 for p in sorted(REVIEW.rglob('*')):
  if p.is_file():z.write(p,'review/'+p.relative_to(REVIEW).as_posix())
 artifacts.append(archive)
records=[{'file':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(OUT.iterdir()) if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
(OUT/'Waktu-Solat-Malaysia-v3.2.0-SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in artifacts),encoding='utf-8')
(QA/'delivery-metrics.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8');print(json.dumps([r for r in records if 'v3.2.0' in r['file']],indent=2))
