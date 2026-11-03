"""Instance the OFL-licensed Jakarta variable font for Android 7 compatibility."""
from pathlib import Path
import sys

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / 'app/build/font-tools'))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

source = root / 'design/assets/jakarta-variable.ttf'
for weight, label in [(400, 'Regular'), (500, 'Medium'), (600, 'SemiBold'), (700, 'Bold')]:
    font = instantiateVariableFont(TTFont(source), {'wght': weight}, inplace=False)
    for entry in font['name'].names:
        if entry.nameID in {1, 4, 6, 16, 17}:
            value = {1: 'WSM Sans', 4: f'WSM Sans {label}', 6: f'WSMSans-{label}',
                     16: 'WSM Sans', 17: label}[entry.nameID]
            entry.string = value.encode(entry.getEncoding())
    destination = root / f'app/src/main/res/font/jakarta_{label.lower()}.ttf'
    font.save(destination)
    print(destination.name)
