"""Package the tested phone allocation fix; preserve older deliveries and private QA data."""
from pathlib import Path
import hashlib, html, json, re, shutil, zipfile
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'deliverables'
QA=ROOT/'app/build/verification/v333-phone'
REVIEW=OUT/'v3.3.3'
SCREENS=REVIEW/'screenshots'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return p.read_text(encoding='utf-8-sig')
debug=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
digest=sha(debug)
assert digest=='e3999eba299256d8c1bd6ff12913a35c7d83e6c6e5079f52a9f29b3a18d601c8'
phone=json.loads(read(QA/'verification.json'))
assert phone['tests']==29 and phone['passed'] and phone['apkSha256']==digest
assert phone['installedApkMatches'] and phone['settingsUnchanged'] and phone['widgetBindPermissionRestored']
assert all(x['retained'] for x in phone['personalData'].values())
assert all(x.get('retained',x.get('absentBeforeAndAfter',False)) for x in phone['persistentFiles'].values())
assert phone['widgetLifecycle']['currentConfigurationsMatchPlacedWidgets']
assert phone['widgetLifecycle']['sameInstanceSettingsRetained']
assert 'OK (29 tests)' in read(QA/'native-tests.log')
emulator_dir=ROOT/'app/build/verification/emulator-matrix/legacy_api24'
legacy=json.loads(read(emulator_dir/'dates-result.json'))
assert legacy['tests']==4 and legacy['apkSha256']==digest
assert legacy['passed'] and legacy['nativeAssertionsPassed'] and legacy['systemUiHealthy']
assert 'BUILD SUCCESSFUL' in read(ROOT/'app/build/verification/v333-build-final.log')
units=0
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    a=ET.parse(p).getroot().attrib
    assert int(a.get('errors',0))==int(a.get('failures',0))==0
    units+=int(a['tests'])
assert units==46
lint=re.search(r'0 errors, (\d+) warnings',read(ROOT/'app/build/reports/lint-results-debug.txt'))
assert lint
old=json.loads(read(OUT/'SHA256SUMS.json'))
assert len(old)==34
for record in old:assert sha(OUT/record['file'])==record['sha256'],record['file']
reference=json.loads(read(ROOT/'app/build/verification/v331/reference-read-only-hashes.json'))
for path,value in reference.items():assert sha(Path(path))==value,path
web=json.loads(read(ROOT/'app/build/verification/v330/web-before.json'))
for path,value in web.items():assert sha(ROOT/path)==value,path
baseline=json.loads(read(OUT/'v3.3.2/artifact-manifest.json'))['sourceFiles']
changed=[]
for path,value in baseline.items():
    p=ROOT/path
    assert p.exists(),path
    if (path.startswith('app/src/main/') or path=='app/build.gradle') and sha(p)!=value:
        changed.append(path)
        assert path=='app/build.gradle' or '/widgets/' in path or path.endswith('/fragments/WidgetsFragment.java') or '/widget_timeline_' in path or '/widget_preview_' in path,path

assert not (OUT/'Waktu-Solat-Malaysia-v3.3.3-source.zip').exists(),'Never overwrite a source checkpoint'
assert not (OUT/'Waktu-Solat-Malaysia-v3.3.3-delivery.zip').exists(),'Never overwrite a delivery'
SCREENS.mkdir(parents=True,exist_ok=True)
for name in ['DESIGN_V333.md','VERIFICATION_V333.md']:shutil.copy2(ROOT/name,REVIEW/name)
for name in ['verification.json','after-allocation.json','native-tests.log']:
    shutil.copy2(QA/name,REVIEW/name)
for name in ['dates-result.json','dates.log']:
    shutil.copy2(emulator_dir/name,REVIEW/('api24-'+name))
shutil.copy2(QA/'widget-preview-69-native.png',SCREENS/'placed-widget-native.png')
shutil.copy2(QA/'widget-preview-69-app.png',SCREENS/'my-widgets-phone.png')
# Exclude unrelated icons from the direct home-screen capture; keep the full source in private QA.
shutil.copy2(QA/'home-widget-crop.png',SCREENS/'xiaomi-launcher-widget-crop.png')
for name in ['phone-actual-host.png','phone-preview-scaled.png','wide-actual-host.png','wide-preview-scaled.png']:
    matches=list((QA/'widget-v332').rglob(name));assert matches,name
    shutil.copy2(max(matches,key=lambda p:p.stat().st_mtime),SCREENS/('fixture-'+name))
for p in (QA/'widget-v333').glob('*.png'):
    shutil.copy2(p,SCREENS/('dates-'+p.name))

font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',26)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
canvas=Image.new('RGB',(1100,780),'#FCF9F3');draw=ImageDraw.Draw(canvas)
draw.text((24,16),'Waktu Solat Malaysia 3.3.3 · calendar dates and location names',font=font,fill='#28231E')
draw.text((24,60),'Full Islamic month · actual 277.6 × 137.9dp phone allocation',font=small,fill='#62574D')
for y,name,label in [(130,'xiaomi-launcher-widget-crop.png','Xiaomi launcher · actual home-screen widget'),(480,'placed-widget-native.png','My widgets · same instance and saved Timeline style')]:
    draw.text((24,y-30),label,font=small,fill='#62574D')
    with Image.open(SCREENS/name) as im:
        im=im.convert('RGBA');im.thumbnail((1000,270),Image.Resampling.LANCZOS)
        canvas.paste(im,(24,y),im)
canvas.save(REVIEW/'widget-comparison.png',optimize=True)

artifacts=[]
for kind,relative in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
    p=ROOT/'app/build/outputs/apk'/relative
    target=OUT/('Waktu-Solat-Malaysia-v3.3.3-'+kind+'.apk')
    assert not target.exists() or sha(target)==sha(p)
    shutil.copy2(p,target);artifacts.append(target)
source_files=[p for p in ROOT.iterdir() if p.is_file() and p.suffix not in ['.apk','.apks'] and not p.name.startswith('.env')]
for folder in ['app/src','gradle','shared','web','tools','testing','design','recovery']:
    for p in (ROOT/folder).rglob('*'):
        if p.is_file() and not any(x in ['node_modules','dist','.wrangler','__pycache__','.git','test-results','playwright-report'] for x in p.relative_to(ROOT).parts) and not p.name.endswith('.tsbuildinfo') and not p.name.startswith('.env'):
            source_files.append(p)
source_files+= [ROOT/'app/build.gradle',ROOT/'app/proguard-rules.pro']
source_files=sorted(set(source_files))
manifest={'version':'3.3.3','versionCode':14,'applicationId':'com.example.waktusolatmalaysia','debugSha256':digest,
    'unitTests':units,'physicalPhoneTests':29,'api24DateTests':4,'lintErrors':0,'lintWarnings':int(lint[1]),
    'phoneVerification':phone,'api24Verification':legacy,'pwaVersion':'3.1.0','pwaUnchangedFiles':len(web),
    'referenceUnchangedFiles':len(reference),'preservedPriorArtifacts':len(old),'changedProductionFiles':changed,
    'sourceFiles':{p.relative_to(ROOT).as_posix():sha(p) for p in source_files},
    'artifacts':[{'file':p.name,'sha256':sha(p),'bytes':p.stat().st_size}for p in artifacts],
    'limitations':['OEM placement evidence covers Today\'s prayers on this Xiaomi phone at its current size.',
        'This update preserves the existing phone widget instances and their configurations.',
        'Other OEM grids, spoken TalkBack and prolonged reboot/Doze delivery remain manual.',
        'Optimized release APK is unsigned; runtime evidence uses the signed debug APK.']}
source=OUT/'Waktu-Solat-Malaysia-v3.3.3-source.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6)as z:
    for p in source_files:z.write(p,p.relative_to(ROOT).as_posix())
    z.writestr('recovery/V333_SOURCE_FILES.json',json.dumps(manifest,indent=2))
with zipfile.ZipFile(source)as z:assert z.testzip() is None
artifacts.append(source)
(REVIEW/'artifact-manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
captions={'xiaomi-launcher-widget-crop.png':'Direct Xiaomi home-screen crop · real cached schedule',
    'placed-widget-native.png':'Placed instance #69 · direct native preview',
    'my-widgets-phone.png':'My widgets · direct app screenshot'}
gallery=[]
for p in sorted(SCREENS.glob('*.png')):
    caption=captions.get(p.name,'Native test fixture · '+p.stem)
    gallery.append('<figure><img loading="lazy" src="screenshots/'+p.name+'"><figcaption>'+html.escape(caption)+'</figcaption></figure>')
(REVIEW/'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Compact widgets 3.3.3</title><style>body{max-width:1100px;margin:24px auto;padding:0 20px;background:#fcf9f3;color:#28231e;font:16px system-ui}figure{margin:28px 0}img{max-width:100%;max-height:900px;height:auto}figcaption{margin-top:8px}</style><h1>Compact widgets · Android 3.3.3</h1><p>Actual Xiaomi phone placement and matching instance previews. Test fixtures are labelled separately.</p><p><a href="VERIFICATION_V333.md">Verification</a> · <a href="DESIGN_V333.md">Design</a></p>'+''.join(gallery),encoding='utf-8')
delivery=OUT/'Waktu-Solat-Malaysia-v3.3.3-delivery.zip'
with zipfile.ZipFile(delivery,'w',zipfile.ZIP_DEFLATED,compresslevel=6)as z:
    for p in artifacts[:2]:z.write(p,p.name)
    for p in REVIEW.rglob('*'):
        if p.is_file():z.write(p,'review/'+p.relative_to(REVIEW).as_posix())
with zipfile.ZipFile(delivery)as z:assert z.testzip() is None
artifacts.append(delivery)
records=[{'file':p.name,'sha256':sha(p),'bytes':p.stat().st_size}for p in sorted(OUT.iterdir())if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
(OUT/'Waktu-Solat-Malaysia-v3.3.3-SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n'for p in artifacts),encoding='utf-8')
print(json.dumps({'debugSha256':digest,'unitTests':units,'phoneTests':29,'api24Tests':4,'oldArtifactsPreserved':len(old),'artifacts':[p.name for p in artifacts]},indent=2))
