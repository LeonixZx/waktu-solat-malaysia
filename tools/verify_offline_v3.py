from verify_android_v3 import *
wifi=adb('shell','settings','get','global','wifi_on').strip();data=adb('shell','settings','get','global','mobile_data').strip()
try:
 adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
 instrument('TabletOfflineTest','native-offline-verified',offline='true')
finally:
 adb('shell','svc','wifi','enable' if wifi=='1' else 'disable');adb('shell','svc','data','enable' if data=='1' else 'disable')
