"""Validate final Android evidence, preserve old deliveries and package UI review artifacts."""
from pathlib import Path
import hashlib, html, json, re, shutil, subprocess, zipfile
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v330'
OUT=ROOT/'deliverables'
REVIEW=OUT/'v3.3.0'
REVIEW.mkdir(exist_ok=True)
def read(p):return p.read_text(encoding='utf-8-sig',errors='replace')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
assert 'BUILD SUCCESSFUL' in read(QA/'native-build.log')
for label in ['tablet-verified','phone-verified','api24-verified']:
    assert 'OK (17 tests)' in read(QA/(label+'.log')),label
for label in ['phone360-130','phone360-200','landscape720-100']:
    assert 'OK (3 tests)' in read(QA/(label+'.log')),label
assert 'OK (5 tests)' in read(QA/'phone-regression.log')
assert all(x['retained'] for x in json.loads(read(QA/'data-retention.json')).values())
assert json.loads(read(QA/'pwa-unchanged.json'))['unchanged']
final=json.loads(read(QA/'tablet-final-state.json'))
assert final['version']=='3.3.0' and final['versionCode']==11
assert final['temporaryWidgetBindGrantRevoked'] and final['settingsRestored'] and final['personalDataRetained'] and not final['orphanConfigurations']
units=0
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    result=ET.parse(p).getroot().attrib;units+=int(result['tests']);assert int(result.get('failures',0))==int(result.get('errors',0))==0
assert units==41
lint=read(ROOT/'app/build/reports/lint-results-debug.txt');match=re.search(r'0 errors, (\d+) warnings',lint);assert match
for item in json.loads(read(OUT/'SHA256SUMS.json')):
    assert sha(OUT/item['file'])==item['sha256'],'Old delivery changed: '+item['file']
artifacts=[]
aapt=r'C:/Users/USER/AppData/Local/Android/Sdk/build-tools/34.0.0/aapt.exe'
for kind,relative in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
    source=ROOT/'app/build/outputs/apk'/relative
    meta=subprocess.check_output([aapt,'dump','badging',str(source)]).decode('utf-8','replace')
    for expected in ["name='com.example.waktusolatmalaysia'","versionName='3.3.0'","versionCode='11'","sdkVersion:'24'","targetSdkVersion:'34'"]:assert expected in meta
    dest=OUT/f'Waktu-Solat-Malaysia-v3.3.0-{kind}.apk';shutil.copy2(source,dest);artifacts.append(dest)
assert sha(artifacts[0])==final['installedDebugSha256']
for name in ['DESIGN_V330.md','VERIFICATION_V330.md']:shutil.copy2(ROOT/name,REVIEW/name)
known=[f'{theme}-{screen}.png' for theme in ['light','dark'] for screen in ['home','shortcuts','worship','collection','explore','settings']]+['settings-dark.png']
for label,source in [('phone320',QA/'ui-phone-320-final'),('tablet',QA/'ui-tablet-final'),('phone360-130',QA/'ui-phone360-130'),('phone360-200',QA/'ui-phone360-200'),('landscape720',QA/'ui-landscape720-100')]:
    if (source/'ui-v330').exists():source=source/'ui-v330'
    dest=REVIEW/'screenshots'/label;dest.mkdir(parents=True,exist_ok=True)
    for name in known:
        assert (source/name).exists(),(label,name)
        shutil.copy2(source/name,dest/name)
host=QA/'tablet-verified-screenshots'
if (host/'tablet-verification').exists():host=host/'tablet-verification'
dest=REVIEW/'screenshots/widget-host';dest.mkdir(exist_ok=True)
for provider in ['NextPrayerWidget','NextPrayerStripWidget','DayPrayerWidget','ExpandedPrayerWidget']:
    for theme in ['light','dark']:
        name=f'v32-{provider}-{theme}.png';shutil.copy2(host/name,dest/name)

# Direct emulator screenshots; no UI is redrawn into this preview sheet.
font=ImageFont.truetype(r'C:/Windows/Fonts/segoeui.ttf',28)
small=ImageFont.truetype(r'C:/Windows/Fonts/segoeui.ttf',18)
canvas=Image.new('RGB',(1000,1260),'#FCF9F3');draw=ImageDraw.Draw(canvas)
draw.text((28,20),'Waktu Solat Malaysia · Android 3.3.0',font=font,fill='#28231E')
draw.text((28,62),'320dp emulator captures · native phone interface',font=small,fill='#6F6459')
for x,label,name in [(28,'Koleksi + bottom navigation','light-collection.png'),(520,'Floating settings · Dark','dark-settings.png')]:
    draw.text((x,108),label,font=small,fill='#6F6459')
    screenshot=Image.open(REVIEW/'screenshots/phone320'/name).convert('RGB');screenshot=screenshot.resize((450,1125),Image.Resampling.LANCZOS);canvas.paste(screenshot,(x,137))
canvas.save(REVIEW/'ui-preview.png',optimize=True)
gallery=[]
for label,caption in [('phone320','API34 emulator · 320dp / 100%'),('phone360-130','API34 emulator · 360dp / 130%'),('phone360-200','API34 emulator · 360dp / 200%'),('landscape720','API34 emulator · landscape 720dp / 100%'),('tablet','Physical Xiaomi tablet')]:
    gallery.append('<h2>'+caption+'</h2>')
    for name in known[:12]:
        gallery.append(f'<figure><img loading="lazy" src="screenshots/{label}/{name}" alt="{html.escape(name)}"><figcaption>{html.escape(name)}</figcaption></figure>')
(REVIEW/'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Android 3.3.0 UI verification</title><style>body{max-width:1200px;margin:24px auto;padding:0 20px;background:#fcf9f3;color:#28231e;font:16px system-ui}figure{display:inline-block;vertical-align:top;width:min(100%,350px);margin:12px}img{width:100%;height:auto;border-radius:14px}figcaption{margin:8px 0}h2{clear:both}</style><h1>Android 3.3.0</h1><p>Direct captures, separated by emulator/window profile and physical tablet. Native widget-host images are included separately; they are not claimed as launcher placement.</p><p><a href="VERIFICATION_V330.md">Verification and manual limitations</a> · <a href="DESIGN_V330.md">Design and implementation</a></p>'+''.join(gallery)+'</html>',encoding='utf-8')
metadata={'androidVersion':'3.3.0','versionCode':11,'applicationId':'com.example.waktusolatmalaysia','pwaVersion':'3.1.0','unitTests':units,'lintErrors':0,'lintWarnings':int(match[1]),'testsPerMainEnvironment':17,'mainEnvironments':['API24 emulator','API34 phone emulator at 320dp','physical Xiaomi API36 tablet'],'widgetLayoutCasesPerEnvironment':576,'additionalCompactWidgetCasesPerEnvironment':44,'additionalChoiceLayoutCasesPerEnvironment':12,'additionalUiProfileTests':9,'readerAndSettingsRegressionTests':5,'placedWidgetIds':final['placedWidgetIds'],'personalDataRetained':True,'pwaUnchangedFiles':53,'manualChecks':['Actual other-phone OEM launcher placement','Spoken TalkBack / physical keyboard traversal','Reboot / long battery-restriction delivery','Release signing and runtime validation'],'artifacts':[{'file':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in artifacts]}
(REVIEW/'artifact-manifest.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
archive=OUT/'Waktu-Solat-Malaysia-v3.3.0-delivery.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6)as z:
    for p in artifacts:z.write(p,p.name)
    for p in REVIEW.rglob('*'):
        if p.is_file():z.write(p,'review/'+p.relative_to(REVIEW).as_posix())
with zipfile.ZipFile(archive)as z:
    assert z.testzip()is None
    assert not any(any(s in n for s in ['personal_library','before-import','reference-read-only.apk'])for n in z.namelist())
artifacts.append(archive)
records=[{'file':p.name,'bytes':p.stat().st_size,'sha256':sha(p)}for p in sorted(OUT.iterdir())if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
(OUT/'Waktu-Solat-Malaysia-v3.3.0-SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n'for p in artifacts),encoding='utf-8')
(QA/'delivery-metrics.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
print(json.dumps([r for r in records if 'v3.3.0' in r['file']],indent=2))
