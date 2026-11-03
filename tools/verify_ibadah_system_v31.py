"""Emulator-only widget, split-window and permission checks, restoring settings in finally."""
from local_android import adb_path
from pathlib import Path
import json,re,subprocess
ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'app/build/verification/v31'
ADB=str(adb_path())
PKG='com.example.waktusolatmalaysia'
def adb(*args):
    p=subprocess.run([ADB,'-s','emulator-5554',*map(str,args)],capture_output=True,check=True)
    return (p.stdout+p.stderr).decode('utf-8','replace')
def permission():
    return next(x.strip() for x in adb('shell','dumpsys','package',PKG).splitlines()
                if 'android.permission.POST_NOTIFICATIONS: granted=' in x)
def test(classes,label,count=1,**arguments):
    args=['shell','am','instrument','-w','-e','class',','.join(PKG+'.'+c for c in classes)]
    for key,value in arguments.items():args+=['-e',key,str(value)]
    out=adb(*args,PKG+'.test/androidx.test.runner.AndroidJUnitRunner')
    (QA/(label+'.log')).write_text(out,encoding='utf-8')
    assert f'OK ({count} '+('test)' if count==1 else 'tests)') in out,out[-4000:]
    print(label+': PASS',flush=True)
if __name__=='__main__':
    font=adb('shell','settings','get','system','font_scale').strip()
    original=permission()
    try:
        test(['IbadahV31Test','LauncherBrandTest','ReadingStateTest','CalendarInteractionTest'],'emulator-ibadah',10)
        test(['IbadahLayoutTest'],'actual-split-screen',capture='actual-split',expectMultiWindow='true')
        adb('shell','appwidget','grantbind','--package',PKG,'--user','0')
        for scale in ['1','2']:
            adb('shell','settings','put','system','font_scale',scale)
            test(['WidgetHostTest'],'widget-host-text'+scale,widgets='true')
        adb('shell','settings','put','system','font_scale','1')
        adb('shell','pm','revoke',PKG,'android.permission.POST_NOTIFICATIONS')
        for flag in ['user-set','user-fixed']:
            adb('shell','pm','clear-permission-flags',PKG,'android.permission.POST_NOTIFICATIONS',flag)
        test(['ReminderPermissionTest'],'notification-denial')
    finally:
        adb('shell','appwidget','revokebind','--package',PKG,'--user','0')
        adb('shell','settings','delete','system','font_scale') if font=='null' else adb('shell','settings','put','system','font_scale',font)
        adb('shell','pm','grant' if 'granted=true' in original else 'revoke',PKG,'android.permission.POST_NOTIFICATIONS')
        for label,flag in [('USER_SET','user-set'),('USER_FIXED','user-fixed')]:
            adb('shell','pm','set-permission-flags' if label in original else 'clear-permission-flags',PKG,'android.permission.POST_NOTIFICATIONS',flag)
        after=permission()
        (QA/'notification-permission-restored.json').write_text(json.dumps({'original':original,'after':after},indent=2),encoding='utf-8')
        assert original==after,(original,after)
        (QA/'widget-host-restored.json').write_text(json.dumps({'fontBefore':font,'fontAfter':adb('shell','settings','get','system','font_scale').strip(),'temporaryBindPermission':'revoked'},indent=2),encoding='utf-8')
        adb('pull','/sdcard/Android/data/'+PKG+'/files/tablet-verification',QA/'emulator-previews')
        adb('shell','am','force-stop',PKG)
        adb('shell','am','start','-n',PKG+'/.views.MainActivity')
