"""Export static picker derivatives from this project's native v3.3.3 renderer captures."""
from pathlib import Path
import hashlib,json
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'app/build/verification/v333-native-exports'
DESIGN=ROOT/'design/widgets-v333'
DESIGN.mkdir(parents=True,exist_ok=True)
families={'next':'NextPrayerWidget','strip':'NextPrayerStripWidget','day':'DayPrayerWidget','expanded':'ExpandedPrayerWidget'}
records=[]
for family,component in families.items():
    for theme in ['light','dark']:
        path=SOURCE/f'picker-{component}-{theme}.png'
        with Image.open(path)as source:
            native=source.convert('RGBA')
            native=native.crop(native.getchannel('A').getbbox())
            original=DESIGN/f'{family}-{theme}-native.png'
            native.save(original,optimize=True)
            record={'family':family,'appearance':theme,'native':original.relative_to(ROOT).as_posix(),
                'nativeSha256':hashlib.sha256(original.read_bytes()).hexdigest()}
            if theme=='light':
                target=ROOT/f'app/src/main/res/drawable-nodpi/widget_preview_{family}.png'
                derivative=native.copy();derivative.thumbnail((720,960),Image.Resampling.LANCZOS)
                derivative.save(target,optimize=True)
                while target.stat().st_size>250000:
                    derivative=derivative.resize((int(derivative.width*.9),int(derivative.height*.9)),Image.Resampling.LANCZOS)
                    derivative.save(target,optimize=True)
                record.update(derivative=target.relative_to(ROOT).as_posix(),bytes=target.stat().st_size,
                    sha256=hashlib.sha256(target.read_bytes()).hexdigest())
            records.append(record)
(DESIGN/'previews.json').write_text(json.dumps({'version':'3.3.3','source':'WidgetDatesV333Test native framework renderer',
    'fixtureOnly':True,'gregorian':'2026-11-03','hijri':'1448-05-23','location':'Kuala Lumpur, Putrajaya',
    'note':'Labelled illustration; no fixture data is written to production caches. Full Hijri month names; no visible zone codes.',
    'assets':records},indent=2),encoding='utf-8')
print(json.dumps(records,indent=2))
