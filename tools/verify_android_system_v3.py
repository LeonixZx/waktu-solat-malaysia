from verify_android_v3 import *
import time,xml.etree.ElementTree as ET
def raw(*args):return subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True).stdout
if __name__=='__main__':
 print(adb('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk')),flush=True)
 print(adb('install','-r','-t',str(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')),flush=True)
 instrument('NativeAppSmokeTest,com.example.waktusolatmalaysia.TabletFeatureTest,com.example.waktusolatmalaysia.V3FeaturesTest','native-regression-complete')
 instrument('DownloadQueueTest','native-download-queue',downloads='true')
 try:
  adb('shell','pm','grant','com.example.waktusolatmalaysia','android.permission.POST_NOTIFICATIONS')
  adb('shell','appops','set','com.example.waktusolatmalaysia','SCHEDULE_EXACT_ALARM','allow')
  adb('shell','dumpsys','battery','unplug');adb('shell','input','keyevent','223')
  idle=adb('shell','dumpsys','deviceidle','force-idle');(OUT/'doze-state.txt').write_text(idle)
  instrument('AlarmDeliveryTest','native-doze-delivery',alarms='true')
 finally:
  adb('shell','dumpsys','deviceidle','unforce');adb('shell','dumpsys','battery','reset');adb('shell','input','keyevent','224')
  adb('shell','appops','set','com.example.waktusolatmalaysia','SCHEDULE_EXACT_ALARM','default')
 subprocess.run([sys.executable,str(ROOT/"tools/verify_reboot_v3.py")],check=True)
