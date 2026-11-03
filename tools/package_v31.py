"""Package verified v3.1 outputs, with no personal snapshots or deployment side effects."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v31'
OUT=ROOT/'deliverables'
def read(path):return path.read_text(encoding='utf-8-sig',errors='replace')

assert 'BUILD SUCCESSFUL' in read(QA/'native-build.log')
assert 'BUILD SUCCESSFUL' in read(QA/'test-build.log')
for name,count in [('tablet-suite',31),('emulator-ibadah',10),('tablet-portrait',1),('tablet-landscape',1),
                   ('actual-split-screen',1),('tablet-dark',1),('calendar-interaction',1),
                   ('widget-host-text1',1),('widget-host-text2',1),('notification-denial',1)]:
    assert f'OK ({count} '+('test)' if count==1 else 'tests)') in read(QA/(name+'.log')),name
layouts=json.loads(read(QA/'android-layouts.json'))
assert len(layouts)==21 and all(x['passed'] for x in layouts)
assert all(v['retained'] for v in json.loads(read(QA/'data-retention.json')).values())
assert '15 passed' in read(QA/'web-e2e.log')
assert '17 passed' in read(QA/'web-unit.log')
assert 'built in' in read(QA/'web-build.log')
assert '--dry-run: exiting now.' in read(QA/'worker-dry-run.log')
tests=sum(int(ET.parse(p).getroot().attrib['tests']) for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
assert tests==41
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    result=ET.parse(p).getroot().attrib
    assert result.get('failures','0')=='0' and result.get('errors','0')=='0'

OUT.mkdir(exist_ok=True)
artifacts=[]
aapt=Path('C:/Users/USER/AppData/Local/Android/Sdk/build-tools/34.0.0/aapt.exe')
for variant,source in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
    apk=ROOT/'app/build/outputs/apk'/source
    info=subprocess.run([str(aapt),'dump','badging',str(apk)],capture_output=True,check=True).stdout.decode('utf-8','replace')
    assert "name='com.example.waktusolatmalaysia'" in info and "versionName='3.1.0'" in info
    target=OUT/f'Waktu-Solat-Malaysia-v3.1.0-{variant}.apk'
    shutil.copy2(apk,target);artifacts.append(target)

config={'name':'waktu-solat-malaysia','main':'worker/index.js','compatibility_date':'2026-11-02',
        'assets':{'directory':'./dist','binding':'ASSETS','not_found_handling':'single-page-application','run_worker_first':['/api/*']},
        'observability':{'enabled':True}}
target=OUT/'Waktu-Solat-Malaysia-v3.1.0-PWA.zip'
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
    for path in (ROOT/'web/dist').rglob('*'):
        if path.is_file() and not path.name.endswith('.map'):
            archive.write(path,Path('dist')/path.relative_to(ROOT/'web/dist'))
    archive.write(QA/'worker-bundle/index.js','worker/index.js')
    archive.writestr('wrangler.jsonc',json.dumps(config,indent=2))
    archive.writestr('README.txt','Waktu Solat Malaysia 3.1.0 PWA\n\nLocal preview: npx wrangler@4.156.1 dev --port 8787\nDeploy after selecting your Cloudflare account: npx wrangler@4.156.1 deploy\n\nBuilt static assets and the fixed-provider Worker are included. No account/domain configuration, personal data or signing secrets are included. Compass guidance in a browser does not certify true-north alignment.\n')
    for name in ['ASSET_PROVENANCE.md','API_SOURCES.md','ARCHITECTURE_V3.md','IMPLEMENTATION_V31.md','VERIFICATION_V31.md']:
        archive.write(ROOT/name,name)
artifacts.append(target)

checksums=[{'file':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(OUT.iterdir()) if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(checksums,indent=2)+'\n',encoding='utf-8')
(QA/'delivery-metrics.json').write_text(json.dumps({'version':'3.1.0','nativeUnitTests':tests,'tabletTests':31,'androidConfigurations':len(layouts),'webUnitTests':17,'browserScenarios':15,'physicalQiblaReferenceComparison':'pending; no independent reference supplied','artifacts':[x for x in checksums if 'v3.1.0' in x['file']]},indent=2)+'\n',encoding='utf-8')
print(json.dumps([x for x in checksums if 'v3.1.0' in x['file']],indent=2))
