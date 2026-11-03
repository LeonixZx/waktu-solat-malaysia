from verify_tablet_v3 import *
import statistics
times=[]
for i in range(3):
 result=adb('shell','am','start','-S','-W','-f','0x10008000','-n','com.example.waktusolatmalaysia/.views.MainActivity')
 (OUT/f'tablet-startup-{i}.txt').write_text(result)
 match=re.search(r'TotalTime:\s*(\d+)',result)
 if match:times.append(int(match[1]))
 time.sleep(.5)
time.sleep(.8)
(OUT/'tablet-memory.txt').write_text(adb('shell','dumpsys','meminfo','com.example.waktusolatmalaysia'))
screenshot('tablet-v3-home-final')
metrics={'build':'debug','coldLaunchMilliseconds':times,'medianColdLaunchMilliseconds':statistics.median(times),'scrollMetrics':'tablet-probe.json; measured in-app because shell touch injection is restricted on this tablet'}
(OUT/'tablet-performance.json').write_text(json.dumps(metrics,indent=2));print(json.dumps(metrics,indent=2))
