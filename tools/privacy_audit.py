"""Read-only checks for private files, credentials and hardcoded local device metadata."""
from pathlib import Path
import argparse,json,re,subprocess,sys,zipfile

ROOT=Path(__file__).resolve().parents[1]
TEXT_EXT={'.java','.kt','.xml','.json','.jsonc','.md','.txt','.py','.yml','.yaml','.properties','.gradle','.ts','.tsx','.html','.svg','.bat','.sh','.toml','.ini','.conf','.mjs','.cjs','.css','.csv','.example',''}
PRIVATE_NAMES={'local.properties','hosts.yml','debug.keystore','personal_library.xml','widget_config.xml','personal_tasks.json',
    'credentials.json','google-services.json','key.properties','keystore.properties','secrets.properties'}
PRIVATE_DIRS={'node_modules','.recovery','.local-archive','.private','.git','.idea','.vscode',
    'build','dist','deliverables','.wrangler','test-results','playwright-report','__pycache__'}
PRIVATE_SUFFIXES={'.keystore','.jks','.pem','.key','.p12','.pfx','.mobileprovision',
    '.hprof','.heapdump','.trace','.db','.sqlite','.sqlite3','.backup','.bundle'}
OUTPUT_SUFFIXES={'.zip','.7z','.rar','.apk','.apks','.aab','.log','.mp4','.webm'}

def findings(name,data):
    problems=[]
    normalized=name.replace('\\','/').lower()
    parts=normalized.split('/')
    basename=parts[-1]
    suffix=Path(basename).suffix
    if basename in PRIVATE_NAMES or any(x in normalized for x in ['private-app-data','preinstall-','before-personal','after-personal']):problems.append('private file')
    if (basename=='.env' or basename.startswith('.env.') and not basename.endswith('.example')
            or basename=='.dev.vars' or basename.startswith('.dev.vars.') and not basename.endswith('.example')
            or basename.startswith('service-account') and suffix=='.json'):
        problems.append('private configuration')
    if any(x in PRIVATE_DIRS for x in parts):problems.append('private/generated directory')
    if suffix in PRIVATE_SUFFIXES:problems.append('key material or private data')
    if suffix in OUTPUT_SUFFIXES:problems.append('generated/release output')
    if suffix not in TEXT_EXT:return sorted(set(problems))
    text=data.decode('utf-8','ignore')
    for match in re.finditer(r'(?i)Users[/\\]+([^/\\\s"\x27]+)',text):
        if match[1].upper() not in {'USER','USERNAME','YOUR_USERNAME','PUBLIC'}:problems.append('personal profile path')
    for match in re.finditer(r'/home/' + r'([^/\s"\x27]+)',text):
        if match[1].upper() not in {'USER','USERNAME','YOUR_USERNAME','RUNNER'}:problems.append('personal profile path')
    if re.search(r'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,}|AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)',text):problems.append('credential pattern')
    if re.search(r'(?i)(?:serial\s*[:=]\s*["\x27]|adb\s+-s\s+)(?:[0-9a-f]{8})(?=["\x27\s,])',text):problems.append('hardcoded USB identifier')
    return sorted(set(problems))

def audit_git(root,staged=False):
    """Scan index bytes before commit, or current tracked working-tree files."""
    issues=[];count=0
    names=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
    for n in filter(None,names):
        path=Path(root)/n
        if not staged and not path.is_file():continue
        data=subprocess.check_output(['git','show',':'+n],cwd=root) if staged else path.read_bytes()
        count+=1
        for reason in findings(n,data):issues.append({'file':n,'reason':reason})
    return count,issues


def main():
    parser=argparse.ArgumentParser();parser.add_argument('archives',nargs='*')
    parser.add_argument('--staged',action='store_true',help='Scan exact Git index bytes, not unstaged edits')
    args=parser.parse_args()
    issues=[];count=0
    if args.archives:
        for file in args.archives:
            with zipfile.ZipFile(file)as z:
                for n in z.namelist():
                    if n.endswith('/'):continue
                    count+=1
                    for reason in findings(n,z.read(n)):issues.append({'file':n,'reason':reason})
    else:
        count,issues=audit_git(ROOT,args.staged)
    print(json.dumps({'checkedFiles':count,'passed':not issues,'findings':issues},indent=2))
    return 1 if issues else 0
if __name__=='__main__':sys.exit(main())
