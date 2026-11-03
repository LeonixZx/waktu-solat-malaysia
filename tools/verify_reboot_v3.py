from verify_android_v3 import *
import time,xml.etree.ElementTree as ET
def raw(*args):return subprocess.run([str(ADB),'-s',SERIAL,*args],capture_output=True).stdout
original=raw('exec-out','run-as','com.example.waktusolatmalaysia','cat','files/personal_tasks.json')
temporary=OUT/'boot-reminders.json'
try:
 temporary.write_text(json.dumps({'schemaVersion':1,'data':[{'id':'boot-verification','title':'Boot check','anchor':'isha','offsetMinutes':360,'weekdays':[1,2,3,4,5,6,7],'enabled':True}]}))
 adb('push',str(temporary),'/data/local/tmp/wsm-boot-reminders.json');adb('shell','run-as','com.example.waktusolatmalaysia','cp','/data/local/tmp/wsm-boot-reminders.json','files/personal_tasks.json')
 adb('shell','am','force-stop','com.example.waktusolatmalaysia');adb('shell','am','start','-n','com.example.waktusolatmalaysia/.views.MainActivity');time.sleep(3)
 before=ET.fromstring(raw('exec-out','run-as','com.example.waktusolatmalaysia','cat','shared_prefs/personal_library.xml'))
 generation=next((n.text for n in before if n.attrib.get('name')=='alarm_generation'),'')
 assert next((n.text for n in before if n.attrib.get('name')=='scheduled_task_boot-verification'),''),'Seed task must be scheduled before reboot'
 uptime_before=adb('shell','cat','/proc/uptime').split()[0]
 adb('reboot');adb('wait-for-disconnect');adb('wait-for-device');end=time.monotonic()+90
 while time.monotonic()<end:
  if adb('shell','getprop','sys.boot_completed').strip()=='1':break
  time.sleep(1)
 adb('shell','input','keyevent','82');adb('shell','wm','dismiss-keyguard')
 # BOOT_COMPLETED can be queued behind other receivers after sys.boot_completed.
 # Do not force-stop the package in cleanup until delivery has had time to finish.
 deadline=time.monotonic()+90
 while True:
  root=ET.fromstring(raw('exec-out','run-as','com.example.waktusolatmalaysia','cat','shared_prefs/personal_library.xml'))
  value=next((n.text for n in root if n.attrib.get('name')=='alarm_generation'),'')
  if value!=generation or time.monotonic()>deadline:break
  time.sleep(1)
 scheduled=next((n.text for n in root if n.attrib.get('name')=='scheduled_task_boot-verification'),'')
 new_generation=next((n.text for n in root if n.attrib.get('name')=='alarm_generation'),'')
 uptime_after=adb('shell','cat','/proc/uptime').split()[0]
 (OUT/'reboot-result.json').write_text(json.dumps({'bootCompleted':True,'futureTaskRestored':bool(scheduled),'scheduledTime':scheduled,'generationChanged':generation!=new_generation,'uptimeBefore':uptime_before,'uptimeAfter':uptime_after},indent=2))
 assert scheduled,'Reboot must restore future tasks using cached official times'
 assert generation!=new_generation,'Boot must run the alarm planner again'
 print('real-emulator-reboot: PASS',flush=True)
finally:
 temporary.write_bytes(original);adb('shell','am','force-stop','com.example.waktusolatmalaysia');adb('push',str(temporary),'/data/local/tmp/wsm-boot-reminders.json');adb('shell','run-as','com.example.waktusolatmalaysia','cp','/data/local/tmp/wsm-boot-reminders.json','files/personal_tasks.json');adb('shell','am','start','-n','com.example.waktusolatmalaysia/.views.MainActivity')
