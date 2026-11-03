"""Package already-verified local outputs; never deploy or include personal snapshots."""
from pathlib import Path
import json,re,hashlib,shutil,zipfile,statistics,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];QA=ROOT/'app/build/verification/v3';OUT=ROOT/'deliverables';OUT.mkdir(exist_ok=True)
def read(path):return path.read_text(encoding='utf-8-sig',errors='replace')
assert 'BUILD SUCCESSFUL' in read(QA/'settings-selection-build.log')
assert 'BUILD SUCCESSFUL' in read(QA/'settings-selection-release.log')
assert 'OK (19 tests)' in read(QA/'navigation-native-regression.log')
assert 'OK (20 tests)' in read(QA/'navigation-tablet-suite.log')
assert '9 passed' in read(QA/'settings-selection-web-e2e.log')
assert '12 passed' in read(QA/'settings-selection-web-unit.log')
assert '--dry-run: exiting now.' in read(QA/'settings-selection-worker-dry-run.log')
layouts=json.loads(read(QA/'navigation-layouts.json'));assert len(layouts)==16 and all(x['passed'] for x in layouts)
for orientation in ['portrait','landscape']:assert 'OK (3 tests)' in read(QA/f'navigation-tablet-{orientation}.log')
for label in ['phone','width-600','width-1280']:
 assert 'OK (3 tests)' in read(QA/f'settings-selection-{label}.log')
probe=json.loads(read(QA/'tablet-probe.json'));assert probe['allHubsReachable']
upgrade=json.loads(read(QA/'navigation-data-retention.json'));assert all(x['retained'] for x in upgrade.values())
boot=json.loads(read(QA/'reboot-result.json'));assert boot['futureTaskRestored'] and boot['generationChanged']
assert 'OK (1 test)' in read(QA/'native-doze-delivery.log')
assert 'OK (1 test)' in read(QA/'native-download-queue.log')
assert 'OK (1 test)' in read(QA/'native-offline-verified.log')
assert 'OK (1 test)' in read(QA/'widget-host.log')
for scale in ['1','1.3','2']:
 for width in [320,360,600,840,1280]:assert 'OK (1 test)' in read(QA/f'android-{width}-text{scale}.log')

artifacts=[]
for source,name in [('app/build/outputs/apk/debug/app-debug.apk','Waktu-Solat-Malaysia-v3.0.2-debug.apk'),('app/build/outputs/apk/release/app-release-unsigned.apk','Waktu-Solat-Malaysia-v3.0.2-release-unsigned.apk')]:
 target=OUT/name;shutil.copy2(ROOT/source,target);artifacts.append(target)
config={'name':'waktu-solat-malaysia','main':'worker/index.js','compatibility_date':'2026-11-02','assets':{'directory':'./dist','binding':'ASSETS','not_found_handling':'single-page-application','run_worker_first':['/api/*']},'observability':{'enabled':True}}
target=OUT/'Waktu-Solat-Malaysia-v3.0.2-PWA.zip'
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as archive:
 for path in (ROOT/'web/dist').rglob('*'):
  if path.is_file() and not path.name.endswith('.map'):archive.write(path,Path('dist')/path.relative_to(ROOT/'web/dist'))
 archive.write(QA/'worker-bundle/index.js','worker/index.js')
 archive.writestr('wrangler.jsonc',json.dumps(config,indent=2))
 archive.writestr('README.txt','Waktu Solat Malaysia 3.0.2 PWA\n\nLocal preview: npx wrangler@4.156.1 dev --port 8787\nDeploy after selecting your Cloudflare account: npx wrangler@4.156.1 deploy\n\nThe zip includes built static assets and the fixed-provider Worker. No account/domain configuration or signing secrets are included. App data stays in the browser.\n')
 for name in ['ASSET_PROVENANCE.md','API_SOURCES.md','ARCHITECTURE_V3.md','NAVIGATION_UX.md']:archive.write(ROOT/name,name)
artifacts.append(target)
checksums=[{'file':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in artifacts]
(OUT/'SHA256SUMS.json').write_text(json.dumps(checksums,indent=2)+'\n')
native_tests=sum(int(ET.parse(p).getroot().attrib['tests']) for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
startup=[]
for p in sorted(QA.glob('tablet-startup-*.txt')):
 match=re.search(r'TotalTime:\s*(\d+)',read(p))
 if match:startup.append(int(match[1]))
with zipfile.ZipFile(QA/'baseline-v2.apk') as old:
 before=sum(i.file_size for i in old.infolist() if any(k in i.filename for k in ['hero_mosque','banner_quran','banner_hikmah']))
after=sum(p.stat().st_size for p in (ROOT/'app/src/main/res/drawable-nodpi').glob('*.webp'))
metrics={'unitTests':native_tests,'tablet':probe,'upgrade':upgrade,'startupMilliseconds':startup,'startupMedianMilliseconds':statistics.median(startup) if startup else None,'runtimeArtworkBytes':after,'v2ArtworkBytes':before,'artworkReductionPercent':round((1-after/before)*100,1),'artifacts':checksums}
(QA/'delivery-metrics.json').write_text(json.dumps(metrics,indent=2)+'\n')
perf=probe.get('performance',{})
report=f'''# v3.0.2 verification · 2 November 2026

The debug app is installed on the connected Xiaomi tablet (`DEVICE_SERIAL`, Android 16/API 36). The reference app and C:\\Tools\\waktu-re\\ were not modified. This is local development verification; signing and public hosting remain release configuration.

The 3.0.1 fix removes the redundant drawer/menu modal, adds a separate Settings entry and direct Quran download access, and flattens the rail/sidebar. The 3.0.2 follow-up fixes Settings incorrectly selecting Saved. Builds, unit/browser checks, current-data retention and targeted navigation/selection tests were rerun. The broader functional and width/text-size results below are the v3.0.1 baseline. Widget host, offline Android, Doze and reboot rows retain the passing v3 baseline results; those services were unchanged. See [NAVIGATION_UX.md](NAVIGATION_UX.md).

## Passing checks

| Area | Result |
|---|---|
| Native build | Debug APK, optimized unsigned release APK, lint (zero errors), {native_tests} JVM tests |
| Native functional regression | v3.0.1 baseline: 19 tests. v3.0.2: three targeted navigation/selection tests at phone, 600 dp rail, and 1280 dp sidebar sizes, including Saved → Settings, recreation and Back |
| Adaptive native layout | v3.0.1 baseline: 15 width/text-size configurations plus short landscape. Selection follow-up tested at phone/rail/sidebar sizes |
| Native menu taps | Actual bottom-tab, rail and sidebar taps; exactly one primary menu asserted |
| Physical tablet | v3.0.1 baseline: 20 tests. v3.0.2: three selection/navigation tests plus direct screenshots in each orientation |
| Upgrade | Fresh snapshots retain all current app/personal preference fields across reinstall/testing, including the current counter; volatile scheduler fields excluded |
| Widgets | Actual AppWidgetHost binds/resizes all three providers; independent zones do not change the app default; light/dark/system RemoteViews render |
| Reminders | Planner offset/weekday/midnight tests, stale generation rejection, edit/delete cancellation, actual AlarmManager delivery in forced deep Doze |
| Reboot | Real emulator reboot restored a future task from cached official times with a new planner generation; no missed backlog |
| Text downloads | Android selected-surah queue resume, aligned Malay/English text and deletion with bookmarks retained; browser transactional queue and late-response deletion tests |
| Portable data | Android export consumed by browser import; browser backup validated on Android; preview, merge/replace, invalid input rejection and disabled imported tasks |
| Web | TypeScript/Vite production build, 12 unit tests, 9 Playwright scenarios, Worker deployment dry run |
| Web layout/accessibility | 320–1920 px routes without page overflow; 200% text across seven routes; axe WCAG A/AA checks; keyboard focus, reduced motion and dark theme |
| Offline PWA | Service worker control, install manifest, deep-link reload, cached Quran and bundled collections after network disconnection |
| Offline Android | Network disconnected; cached prayers/readings remain usable and an uncached date shows no invented times |
| Live adapter | Genuine JAKIM November 2026 month (30 days) and Al-Fatihah four-edition response |

## Measurements

- Earlier v3 cold debug activity launches on the tablet: {', '.join(str(t)+' ms' for t in startup)}; median {statistics.median(startup) if startup else 'unavailable'} ms. These are development measurements, not release startup guarantees.
- The physical in-app sample records {perf.get('sampledFrames','n/a')} frames while opening/scrolling the Quran list, {perf.get('overDeadlineFrames','n/a')} beyond Android's reported frame deadline, longest {perf.get('longestFrameMs','n/a')} ms; total PSS {perf.get('totalPssKb','n/a')} KB and native allocated heap {perf.get('nativeHeapBytes','n/a')} bytes. The raw report is in `app/build/verification/v3/tablet-probe.json`. Deadline interpretation follows [Android FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics#DEADLINE); this debug/instrumented sample is not a release benchmark.
- All six runtime WebP illustrations total {after:,} bytes versus {before:,} bytes for the old three raster illustrations: **{(1-after/before)*100:.1f}% less raster data**. Each derivative is under 90 KB. Original generated PNGs remain outside the runtime bundles.
- Native surah/ayah holders reuse their child views, stable IDs and click controls. Artwork decoding uses measured bounds off the main thread with a bounded cache. Unchanged dashboard labels are not reset on each countdown tick.

## Evidence and repeatability

Canonical logs and captures are under `app/build/verification/v3/`: `final-verified-build.log`, `native-regression-verified.log`, `android-*-text*.log`, `widget-host.log`, `native-download-queue.log`, `native-doze-delivery.log`, `reboot-result.json`, `tablet-probe.json`, `tablet-upgrade.json`, `pwa-*-delivery-final.log` and `delivery-metrics.json`.

The 3.0.1 logs use the `navigation-` prefix: `navigation-build.log`, `navigation-release.log`, `navigation-native-regression.log`, `navigation-tablet-suite.log`, `navigation-layouts.json`, `navigation-data-retention.json` and `navigation-web-*.log`. Device rotation and emulator display/font settings were restored. The debug-only in-app probe is absent from release. The historical v2 snapshot has a different tasbih counter; it is retained as historical evidence, never imported or used to reset the current count.

Selection previews: [Settings on the tablet](app/build/verification/v3/settings-selection-tablet-landscape.png).

Useful previews: [tablet portrait](app/build/verification/v3/navigation-tablet-portrait.png), [tablet landscape](app/build/verification/v3/navigation-tablet-landscape.png), [web desktop](app/build/verification/v3/nav-home-1280x900.png), [web phone](app/build/verification/v3/nav-home-360x800.png).

## Release checks still requiring human/platform review

Manual TalkBack listening/Arabic pronunciation, native keyboard navigation with real hardware, Windows installation through the user's browser profile, and prolonged OEM battery/launcher behavior remain release QA. Automatic widget pinning did not show a launcher prompt on this MIUI tablet; the app provides a manual launcher-widget guide. Actual binding/resizing is verified in the emulator widget host. Full 114-surah network downloads and current live-program playback depend on upstream availability; selected downloads, queue persistence and unavailable-media behavior are covered. Publisher signing, domain/account setup and content distribution approval are not configured by this local build.
'''
(ROOT/'VERIFICATION_V3.md').write_text(report,encoding='utf-8')
print(json.dumps(checksums,indent=2))
