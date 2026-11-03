from pathlib import Path
R=Path(__file__).resolve().parents[1]/'app/src/main/res'
NS='xmlns:android="http://schemas.android.com/apk/res/android"'
OB=[1,4,5,6,7]
def col(body,extra=''):return '<android.widget.LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" '+extra+'>'+body+'</android.widget.LinearLayout>'
def row(body,extra=''):return '<android.widget.LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" '+extra+'>'+body+'</android.widget.LinearLayout>'
def text(id,size,width='match_parent',extra='',single=True):return f'<android.widget.TextView android:id="@+id/{id}" android:layout_width="{width}" android:layout_height="wrap_content" android:textSize="{size}sp" android:fontFamily="sans" {'android:maxLines="1"' if single else ''} {extra}/>'
def gap(n):return f'<android.widget.LinearLayout android:layout_width="1dp" android:layout_height="{n}dp" android:importantForAccessibility="no"/>'
def art(id,size,src,extra=''):return f'<android.widget.ImageView android:id="@+id/{id}" android:layout_width="{size}dp" android:layout_height="{size}dp" android:src="@drawable/{src}" android:importantForAccessibility="no" {extra}/>'
def button(id,src):return f'<android.widget.ImageButton android:id="@+id/{id}" android:layout_width="48dp" android:layout_height="48dp" android:padding="12dp" android:src="@drawable/{src}" android:background="@drawable/widget_action_light"/>'
def header(settings=False,stack=False):
 logo=art('widget_logo',18,'ic_launcher','android:layout_marginEnd="8dp"')
 if stack:meta=col(text('widget_zone',12)+text('widget_date',12),'android:layout_weight="1"').replace('android:layout_width="match_parent"','android:layout_width="0dp"',1)
 else:meta=text('widget_zone',12,'0dp','android:layout_weight="1"')+text('widget_date',12,'wrap_content','android:layout_marginStart="8dp"')
 return row(logo+meta+(button('widget_settings','ic_settings') if settings else ''))
def cell(i,icons=True,tracking=False):
 body=(art('widget_icon_'+str(i),24,'widget_prayer_'+str(i),'android:layout_gravity="center_horizontal"')+gap(4) if icons else '')+text('widget_name_'+str(i),14,extra='android:gravity="center"')+text('widget_value_'+str(i),18,extra='android:gravity="center"')+(button('widget_check_'+str(i),'widget_check_off_light') if tracking else '')
 return col(body,f'android:id="@+id/widget_cell_{i}" android:layout_weight="1" android:gravity="center" android:padding="4dp" android:layout_marginEnd="2dp"').replace('android:layout_width="match_parent"','android:layout_width="0dp"',1)
def band(indices=OB,icons=True,tracking=False):return row(''.join(cell(i,icons,tracking) for i in indices))
def info_row(i,tracking=False):
 return row(art('widget_icon_'+str(i),24,'widget_prayer_'+str(i),'android:layout_marginEnd="8dp"')+text('widget_name_'+str(i),14,'0dp','android:layout_weight="1" android:layout_marginEnd="8dp"')+text('widget_value_'+str(i),18,'wrap_content')+(button('widget_check_'+str(i),'widget_check_off_light') if tracking else ''),f'android:id="@+id/widget_cell_{i}" android:padding="4dp"')
def auxiliary(horizontal=False):
 if horizontal:body=band([0,2,3],False)
 else:body=''.join(info_row(i) for i in [0,2,3])
 return col(body,'android:id="@+id/widget_aux" android:layout_marginTop="8dp"')
def status():return gap(6)+text('widget_status',12,single=False)
def write(name,body):
 # The visible surface wraps its content inside a transparent launcher allocation.
 surface=col(body,'android:id="@+id/widget_surface" android:layout_gravity="center" android:padding="12dp" android:background="@drawable/widget_bg_light" android:clipToOutline="true"')
 s=f'<android.widget.FrameLayout {NS} android:id="@+id/widget_root" android:layout_width="match_parent" android:layout_height="match_parent">'+surface+'</android.widget.FrameLayout>'
 (R/'layout'/f'{name}.xml').write_text(s,encoding='utf-8')
 modern=s.replace('android:fontFamily="sans"','android:fontFamily="@font/jakarta_regular"')
 import re
 modern=re.sub(r'(android:id="@\+id/widget_(?:time|value_\d)"[^>]*?)android:fontFamily="@font/jakarta_regular"',r'\1android:fontFamily="@font/jakarta_semibold"',modern)
 (R/'layout-v26'/f'{name}.xml').write_text(modern,encoding='utf-8')
hero=col(text('widget_kicker',12)+text('widget_next',18)+text('widget_time',32))
write('widget_next',header(stack=True)+gap(12)+hero)
write('widget_next_overview',header(stack=True)+gap(12)+hero+gap(12)+band(OB[:3],False)+gap(4)+band(OB[3:],False))
write('widget_next_inline',header()+gap(4)+row(text('widget_next',14,'0dp','android:layout_weight="1" android:layout_marginEnd="8dp"')+text('widget_time',32,'wrap_content')))
def compact_cell(i):
 body=text('widget_name_'+str(i),12,extra='android:gravity="center"')+text('widget_value_'+str(i),16,extra='android:gravity="center"')+text('widget_period_'+str(i),10,extra='android:gravity="center" android:visibility="gone"')
 return col(body,f'android:id="@+id/widget_cell_{i}" android:layout_weight="1" android:gravity="center" android:paddingStart="0dp" android:paddingEnd="0dp" android:paddingTop="2dp" android:paddingBottom="2dp" android:layout_marginEnd="2dp"').replace('android:layout_width="match_parent"','android:layout_width="0dp"',1)
write('widget_strip',header()+gap(6)+row(''.join(compact_cell(i) for i in OB))+status())
write('widget_day',header()+gap(8)+band()+status())
write('widget_day_grid',header()+gap(8)+band(OB[:3])+gap(4)+band(OB[3:])+status())
write('widget_day_list',header()+gap(8)+''.join(info_row(i) for i in OB)+status())
for track in [False,True]:
 tail=text('widget_hint',12,single=False)
 head=header(True,True)+text('widget_hijri',12)+gap(8)
 for shape in ['dashboard','grid','list']:
  body=band(OB,True,track) if shape=='dashboard' else band(OB[:3],True,track)+gap(4)+band(OB[3:],True,track) if shape=='grid' else ''.join(info_row(i,track) for i in OB)
  name=('widget_expanded_tracking' if track else 'widget_expanded') if shape=='list' else 'widget_expanded_'+shape+('_tracking' if track else '')
  write(name,head+body+auxiliary(shape!='list')+(status() if not track else '')+tail)
write('widget_state',header(stack=True)+gap(8)+text('widget_message',14,single=False)+gap(4)+text('widget_detail',12,single=False))
# Original line-art glyphs: no reference-publisher artwork is used.
paths={0:'M4,16H20M6,13L4,11M18,13L20,11M8,13a4,4 0,0 1,8 0M12,5V8',1:'M3,18H21M5,15H19M7,14a5,5 0,0 1,10 0M12,3V6M3,8L5,10M21,8L19,10',2:'M3,18H21M7,15a5,5 0,0 1,10 0M12,3V6M4,8L6,10M20,8L18,10',3:'M12,3V5M4,12H6M18,12H20M6,6L8,8M16,16L18,18M18,6L16,8M6,18L8,16M16,12a4,4 0,1 1,-8 0a4,4 0,1 1,8 0',4:'M12,2V5M12,19V22M2,12H5M19,12H22M5,5L7,7M17,17L19,19M19,5L17,7M5,19L7,17M17,12a5,5 0,1 1,-10 0a5,5 0,1 1,10 0',5:'M6,17H18a3,3 0,0 0,0 -6a5,5 0,0 0,-9 -2a4,4 0,0 0,-3 8M16,3V5M21,8H23M20,4L19,5',6:'M3,17H21M5,20H19M7,14a5,5 0,0 1,10 0M12,4V7M4,8L6,10M20,8L18,10',7:'M19,15a8,8 0,0 1,-10 -10a8,8 0,1 0,10 10M18,3V7M16,5H20'}
for i,path in paths.items():
 for dark in [False,True]:
  suffix='_dark' if dark else '';color='#E8B86D' if dark else '#8C581E'
  (R/'drawable'/f'widget_prayer_{i}{suffix}.xml').write_text(f'<vector {NS} android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:pathData="{path}" android:fillColor="@android:color/transparent" android:strokeColor="{color}" android:strokeWidth="1.6" android:strokeLineCap="round" android:strokeLineJoin="round"/></vector>',encoding='utf-8')
for theme,start,end,border in [('light','#FFF9EF','#F2E7D7','#D8C3A0'),('dark','#3A2A20','#211913','#805D38')]:
 (R/'drawable'/f'widget_bg_{theme}.xml').write_text(f'<shape {NS}><gradient android:angle="315" android:startColor="{start}" android:endColor="{end}"/><stroke android:width="0.5dp" android:color="{border}"/><corners android:radius="@dimen/widget_corner"/></shape>',encoding='utf-8')
# Resize state also uses the same content-sized visible surface.
write('widget_resize',art('widget_resize_icon',48,'widget_resize','android:layout_gravity="center_horizontal"'))
print('Original native widget family generated.')
