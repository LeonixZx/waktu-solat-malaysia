"""Read-only checks for private files, credentials and hardcoded local device metadata."""
from pathlib import Path
import argparse,json,re,subprocess,sys,zipfile

ROOT=Path(__file__).resolve().parents[1]
TEXT_EXT={'.java','.kt','.xml','.json','.jsonc','.md','.txt','.py','.yml','.yaml','.properties','.gradle','.ts','.tsx','.html','.svg','.bat','.sh',''}
PRIVATE_NAMES={'local.properties','hosts.yml','debug.keystore','personal_library.xml','widget_config.xml','personal_tasks.json'}

def findings(name,data):
    problems=[]
    parts=Path(name).parts
    if Path(name).name in PRIVATE_NAMES or any(x in name for x in ['private-app-data','preinstall-','before-personal','after-personal']):problems.append('private file')
    if any(x in parts for x in ['node_modules','.recovery']):problems.append('private/generated directory')
    if Path(name).suffix.lower() in {'.keystore','.jks','.pem','.key','.p12'}:problems.append('key material')
    if Path(name).suffix.lower() not in TEXT_EXT:return problems
    text=data.decode('utf-8','ignore')
    for match in re.finditer(r'(?i)Users[/\\]+([^/\\\s"\x27]+)',text):
        if match[1].upper() not in {'USER','USERNAME','YOUR_USERNAME','PUBLIC'}:problems.append('personal profile path')
    if re.search(r'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,}|AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)',text):problems.append('credential pattern')
    if re.search(r'(?i)(?:serial\s*[:=]\s*["\x27]|adb\s+-s\s+)(?:[0-9a-f]{8})(?=["\x27\s,])',text):problems.append('hardcoded USB identifier')
    return sorted(set(problems))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('archives',nargs='*');args=parser.parse_args()
    issues=[];count=0
    if args.archives:
        for file in args.archives:
            with zipfile.ZipFile(file)as z:
                for n in z.namelist():
                    if n.endswith('/'):continue
                    count+=1
                    for reason in findings(n,z.read(n)):issues.append({'file':n,'reason':reason})
    else:
        names=subprocess.check_output(['git','ls-files','-z'],cwd=ROOT).decode().split('\0')
        for n in filter(None,names):
            count+=1
            for reason in findings(n,(ROOT/n).read_bytes()):issues.append({'file':n,'reason':reason})
    print(json.dumps({'checkedFiles':count,'passed':not issues,'findings':issues},indent=2))
    return 1 if issues else 0
if __name__=='__main__':sys.exit(main())
