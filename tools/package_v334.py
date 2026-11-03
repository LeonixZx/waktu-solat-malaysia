"""Package tested alert fixes and public recovery artifacts without private device snapshots."""
from pathlib import Path
import hashlib,json,re,shutil,subprocess,zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'deliverables';REVIEW=OUT/'v3.3.4';REVIEW.mkdir(exist_ok=True)
def read(p):return p.read_text(encoding='utf-8-sig')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT).decode().strip()
debug=ROOT/'app/build/outputs/apk/debug/app-debug.apk';digest=sha(debug)
assert digest=='31742a4184f3caa44c67673ac2474f31e58799410c26d20655d20a0f03f60b16'
assert not git('status','--porcelain'),'Commit the reviewable source before packaging'
old=json.loads(read(OUT/'SHA256SUMS.json'));assert len(old)==38
for item in old:assert sha(OUT/item['file'])==item['sha256'],item['file']
assert 'BUILD SUCCESSFUL' in read(ROOT/'app/build/verification/v334-build-final.log')
unit=0
for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
    a=ET.parse(p).getroot().attrib;assert int(a.get('errors',0))==int(a.get('failures',0))==0;unit+=int(a['tests'])
assert unit==46
lint=re.search(r'0 errors, (\d+) warnings',read(ROOT/'app/build/reports/lint-results-debug.txt'));assert lint
native=[]
for profile in ['standard_phone','legacy_api24']:
    folder=ROOT/'app/build/verification/emulator-matrix'/profile
    check=json.loads(read(folder/'alerts-result.json'))
    assert check['passed']and check['systemUiHealthy']and check['nativeAssertionsPassed']and check['tests']==7
    assert check['apkSha256']==digest
    assert len(check['coldStartChecks'])==2 and all(x['processConfirmedAbsent']and x['delivered']for x in check['coldStartChecks'])
    native.append(check)
    for name in ['alerts-result.json','alerts.log','cold-notification.log','cold-azan.log']:
        shutil.copy2(folder/name,REVIEW/(profile+'-'+name))
phone=json.loads(read(ROOT/'app/build/verification/v334-phone/verification.json'))
assert phone['tests']==2 and phone['passed']and phone['installedApkMatches']and phone['apkSha256']==digest
assert phone['settingsUnchanged']and all(x['retained']for x in phone['personalData'].values())
assert all(x['retained']for x in phone['persistentFiles'].values())
clone=json.loads(read(ROOT/'app/build/verification/github-clone/verification.json'))
assert clone['androidBuildPassed']and clone['publicCloneWithoutCredentials']and clone['web']['passed']
for n in ['VERIFICATION_V334.md','CLONE_AND_BUILD.md','GITHUB_BACKUP_VERIFICATION.md','NOTICE.md','LICENSE']:
    shutil.copy2(ROOT/n,REVIEW/n)
# Real-device screenshots are private; do not include them in public releases.
# Public summaries omit serial numbers, private screenshots, preference contents and schedule zones.
public_phone={'api':36,'model':'24069PC21G','tests':2,'passed':True,'installedApkMatches':True,
    'existingPreferencesAndWidgetsRetained':True,'cachedScheduleMonthsRetained':len(phone['persistentFiles']),
    'settingsUnchanged':True,'exactAlarmPermissionBefore':False,'exactAlarmPermissionAfter':True,
    'permissionChangeMadeByOwner':True,'alarmVolumeUnchanged':'4/15'}
public_clone={k:v for k,v in clone.items()if k!='clonePath'}
manifest={'version':'3.3.4','versionCode':15,'applicationId':'com.example.waktusolatmalaysia',
    'debugSha256':digest,'gitCommit':git('rev-parse','HEAD'),'repository':'https://github.com/LeonixZx/waktu-solat-malaysia',
    'unitTests':unit,'lintErrors':0,'lintWarnings':int(lint[1]),'nativeEnvironments':native,
    'phone':public_phone,'independentPublicClone':public_clone,'preservedPriorArtifacts':len(old),
    'limits':['Debug APK is for development/testing; the optimized release APK is unsigned.',
        'Playback state and notification delivery are verified; speaker loudness was not measured by a microphone.',
        'OEM battery restrictions, force-stop, muted channels and revoked permissions can affect delivery.',
        'Private signing keys and phone app data are not included in public source backup.']}
artifacts=[]
for kind,relative in [('debug','debug/app-debug.apk'),('release-unsigned','release/app-release-unsigned.apk')]:
    source=ROOT/'app/build/outputs/apk'/relative;target=OUT/f'Waktu-Solat-Malaysia-v3.3.4-{kind}.apk'
    assert not target.exists()or sha(target)==sha(source),'Never overwrite an earlier different installer'
    shutil.copy2(source,target);artifacts.append(target)
files=[ROOT/n for n in git('ls-files').splitlines()]
assert all(not p.relative_to(ROOT).as_posix().startswith(('.recovery/','app/build/','deliverables/'))for p in files)
manifest['sourceFiles']={p.relative_to(ROOT).as_posix():sha(p)for p in files}
manifest['artifacts']=[{'file':p.name,'sha256':sha(p),'bytes':p.stat().st_size}for p in artifacts]
source=OUT/'Waktu-Solat-Malaysia-v3.3.4-source.zip';assert not source.exists()
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6)as z:
    for p in files:z.write(p,p.relative_to(ROOT).as_posix())
    z.writestr('recovery/V334_SOURCE_FILES.json',json.dumps(manifest,indent=2))
with zipfile.ZipFile(source)as z:assert z.testzip()is None
artifacts.append(source)
(REVIEW/'artifact-manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
(REVIEW/'index.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Waktu Solat Malaysia 3.3.4 verification</title><style>body{max-width:900px;margin:32px auto;padding:0 20px;font:17px system-ui;background:#fcf9f3;color:#28231e}img{max-width:100%}</style><h1>Adhan and notifications Â· 3.3.4</h1><p>Original project branding is used. Real-device captures remain private.</p><p><a href="VERIFICATION_V334.md">Device verification</a> Â· <a href="CLONE_AND_BUILD.md">Clone and build</a> Â· <a href="artifact-manifest.json">Evidence and source manifest</a></p>',encoding='utf-8')
delivery=OUT/'Waktu-Solat-Malaysia-v3.3.4-delivery.zip';assert not delivery.exists()
with zipfile.ZipFile(delivery,'w',zipfile.ZIP_DEFLATED,compresslevel=6)as z:
    for p in artifacts[:2]:z.write(p,p.name)
    for p in REVIEW.rglob('*'):
        if p.is_file():z.write(p,'review/'+p.relative_to(REVIEW).as_posix())
with zipfile.ZipFile(delivery)as z:assert z.testzip()is None
artifacts.append(delivery)
records=[{'file':p.name,'sha256':sha(p),'bytes':p.stat().st_size}for p in sorted(OUT.iterdir())if p.suffix in ['.apk','.zip']]
(OUT/'SHA256SUMS.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
(OUT/'Waktu-Solat-Malaysia-v3.3.4-SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n'for p in artifacts),encoding='utf-8')
print(json.dumps({'debugSha256':digest,'sourceFiles':len(files),'artifacts':[p.name for p in artifacts],
    'phoneTests':2,'nativeAlertTests':14,'coldProcessDeliveries':4,'unitTests':unit,'oldArtifactsPreserved':len(old)},indent=2))
