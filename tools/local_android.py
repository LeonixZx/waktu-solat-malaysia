"""Resolve local tooling without committing an owner's profile path or USB identifier."""
from pathlib import Path
import os

def android_sdk():
    configured=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if configured:return Path(configured)
    home=Path.home()
    options=[home/'AppData/Local/Android/Sdk',home/'Library/Android/sdk',home/'Android/Sdk']
    return next((p for p in options if p.exists()),options[0])

def adb_path():
    return android_sdk()/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb')
