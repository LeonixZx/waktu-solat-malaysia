"""Smoke-test the built PWA zip locally. Does not deploy or use a browser profile."""
from pathlib import Path
import json,shutil,subprocess,time,urllib.request,urllib.error,zipfile
ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v31'
folder=QA/'packaged-pwa-smoke';folder.mkdir(exist_ok=True)
with zipfile.ZipFile(ROOT/'deliverables/Waktu-Solat-Malaysia-v3.1.0-PWA.zip') as archive:
    for item in archive.infolist():
        target=(folder/item.filename).resolve()
        assert target.is_relative_to(folder.resolve()),item.filename
    archive.extractall(folder)
def request(path,method='GET'):
    req=urllib.request.Request('http://127.0.0.1:8788'+path,method=method)
    try:
        with urllib.request.urlopen(req,timeout=3) as response:return response.status,response.read()
    except urllib.error.HTTPError as error:return error.code,error.read()
results={}
with (QA/'packaged-pwa-server.log').open('w',encoding='utf-8') as log:
    process=subprocess.Popen([shutil.which('node'),str(ROOT/'web/node_modules/wrangler/bin/wrangler.js'),'dev',
                '--config',str(folder/'wrangler.jsonc'),'--port','8788'],cwd=folder,stdout=log,stderr=log,
                creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    try:
        deadline=time.monotonic()+40
        while True:
            try:
                status,body=request('/');assert status==200 and b'<html' in body;break
            except (OSError,AssertionError):
                assert process.poll() is None, 'Local Worker exited early'
                if time.monotonic()>deadline:raise
                time.sleep(.5)
        for path in ['/','/collections/wirid','/qibla','/sw.js','/manifest.webmanifest']:
            status,body=request(path);assert status==200 and body,path;results[path]=status
        status,body=request('/api/catalog');catalog=json.loads(body)
        assert status==200 and catalog.get('entries'),catalog
        results['catalogEntries']=len(catalog['entries'])
        for path,method,expected in [('/api/prayers?zone=INVALID&month=2026-11','GET',400),('/api/anything?url=https://example.com','GET',404),('/api/prayers','POST',405)]:
            status,_=request(path,method);assert status==expected,(path,status);results[method+' '+path]=status
        (QA/'packaged-pwa-smoke.json').write_text(json.dumps({'passed':True,'checks':results},indent=2),encoding='utf-8')
        print(json.dumps(results,indent=2))
    finally:
        subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],capture_output=True)
        process.wait(timeout=10)
