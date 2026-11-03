"""Generate the project's native vector icon family and warm design tokens."""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'app/src/main/res'
def write(name, text):
    path=root/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(text, encoding='utf-8')
day={'colorPrimary':'#A46A2A','colorPrimaryDark':'#67411F','colorAccent':'#E8B86D','background':'#FCF9F3','surface':'#FFFFFF','text_primary':'#28231E','text_secondary':'#83786C','prayer_text':'#9A642C','prayer_selected':'#F6E8D1','divider':'#EEE6DC','hero_background':'#F5E2C3','hero_muted':'#886B4A','on_accent':'#4B301B','white':'#FFFFFF','art_text':'#59391E','soft_blue':'#EAF0FC','soft_purple':'#F0E9F8','soft_rose':'#FAEAE6','soft_gold':'#F8EEDB'}
night={'background':'#171410','surface':'#25201A','text_primary':'#F7EEDD','text_secondary':'#BDAF9B','prayer_text':'#E8BB7D','prayer_selected':'#483523','divider':'#44382C','soft_blue':'#28303D','soft_purple':'#352C3D','soft_rose':'#402D29','soft_gold':'#3D3020'}
for folder, colors in [('values',day),('values-night',night)]:
    write(f'{folder}/colors.xml','<resources>\n'+''.join(f'    <color name="{k}">{v}</color>\n' for k,v in colors.items())+'</resources>\n')
common='''
<item name="colorPrimary">@color/colorPrimary</item><item name="colorPrimaryVariant">@color/colorPrimaryDark</item><item name="colorPrimaryDark">@color/colorPrimaryDark</item><item name="colorOnPrimary">@color/white</item><item name="colorSecondary">@color/colorAccent</item><item name="colorOnSecondary">@color/on_accent</item><item name="colorSurface">@color/surface</item><item name="colorOnSurface">@color/text_primary</item><item name="android:colorBackground">@color/background</item><item name="android:textColorPrimary">@color/text_primary</item><item name="android:textColorSecondary">@color/text_secondary</item><item name="fontFamily">@font/jakarta_regular</item><item name="android:fontFamily">@font/jakarta_regular</item><item name="android:statusBarColor">@color/background</item><item name="android:navigationBarColor">@color/background</item><item name="android:windowLightStatusBar">LIGHT</item><item name="android:windowActionModeOverlay">true</item><item name="materialButtonStyle">@style/Widget.App.Button</item>'''
styles='''
<style name="Text.Heading" parent="TextAppearance.MaterialComponents.Headline6"><item name="fontFamily">@font/jakarta_bold</item><item name="android:fontFamily">@font/jakarta_bold</item><item name="android:textColor">@color/text_primary</item><item name="android:textSize">24sp</item><item name="android:letterSpacing">-0.02</item></style>
<style name="Text.Eyebrow" parent="TextAppearance.MaterialComponents.Caption"><item name="android:textSize">11sp</item><item name="android:fontFamily">@font/jakarta_semibold</item><item name="android:letterSpacing">0.08</item><item name="android:textColor">@color/text_secondary</item></style>
<style name="Text.Body" parent="TextAppearance.MaterialComponents.Body1"><item name="android:textSize">14sp</item><item name="android:lineSpacingExtra">4dp</item><item name="android:textColor">@color/text_secondary</item></style>
<style name="Text.Toolbar" parent="TextAppearance.MaterialComponents.Subtitle1"><item name="android:fontFamily">@font/jakarta_bold</item><item name="android:textSize">15sp</item><item name="android:letterSpacing">0.02</item></style>
<style name="Text.Nav" parent="TextAppearance.MaterialComponents.Caption"><item name="android:fontFamily">@font/jakarta_semibold</item><item name="android:textSize">10sp</item></style>
<style name="Widget.WaktuSolat.Card" parent="Widget.MaterialComponents.CardView"><item name="cardCornerRadius">22dp</item><item name="cardElevation">0dp</item><item name="strokeWidth">0dp</item></style>
<style name="Widget.App.Button" parent="Widget.MaterialComponents.Button"><item name="cornerRadius">18dp</item><item name="android:textAllCaps">false</item><item name="android:letterSpacing">0</item><item name="android:minHeight">50dp</item><item name="fontFamily">@font/jakarta_semibold</item></style>
<style name="Widget.App.OutlinedButton" parent="Widget.MaterialComponents.Button.OutlinedButton"><item name="cornerRadius">18dp</item><item name="strokeColor">@color/divider</item><item name="android:textColor">@color/prayer_text</item><item name="android:textAllCaps">false</item><item name="android:letterSpacing">0</item><item name="android:minHeight">48dp</item></style>
<style name="Widget.App.TextButton" parent="Widget.MaterialComponents.Button.TextButton"><item name="android:textColor">@color/prayer_text</item><item name="android:textAllCaps">false</item><item name="android:letterSpacing">0</item></style>'''
write('values/themes.xml','<resources><style name="Base.Theme.WaktuSolat" parent="Theme.MaterialComponents.DayNight.NoActionBar">'+common.replace('LIGHT','true')+'</style><style name="Theme.WaktuSolat" parent="Base.Theme.WaktuSolat"/>'+styles+'</resources>')
write('values-night/themes.xml','<resources><style name="Base.Theme.WaktuSolat" parent="Theme.MaterialComponents.DayNight.NoActionBar">'+common.replace('LIGHT','false')+'</style></resources>')
for folder, light in [('values-v27','true'),('values-night-v27','false')]:
    write(f'{folder}/themes.xml',f'<resources><style name="Theme.WaktuSolat" parent="Base.Theme.WaktuSolat"><item name="android:windowLightNavigationBar">{light}</item></style></resources>')
write('drawable/bg_navigation.xml','<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="@color/surface"/><corners android:radius="30dp"/><stroke android:width="1dp" android:color="@color/divider"/></shape>')
write('drawable/bg_art_scrim.xml','<shape xmlns:android="http://schemas.android.com/apk/res/android"><gradient android:angle="0" android:startColor="#F2FFF9ED" android:centerColor="#A6FFF8EA" android:endColor="#00FFF9ED"/></shape>')
write('drawable/bg_hero_scrim.xml','<shape xmlns:android="http://schemas.android.com/apk/res/android"><gradient android:angle="0" android:startColor="#DAFFF9ED" android:centerColor="#66FFF8EA" android:endColor="#00FFF9ED"/></shape>')
write('drawable/bg_soft.xml','<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="@color/prayer_selected"/><corners android:radius="18dp"/></shape>')
icons={
'ic_home':('M3,10l9,-7l9,7v10a1,1 0,0 1,-1,1H4a1,1 0,0 1,-1,-1z','M9,21v-8h6v8','#D29C57'),
'ic_prayer':('M3,16h18M6,20h12M7,13a5,5 0,0 1,10,0M12,2v3M4,5l2,2M20,5l-2,2','M7,13a5,5 0,0 1,10,0z','#E3A44D'),
'ic_quran':('M12,6C8,3 3,4 3,4v15s5,-1 9,2c4,-3 9,-2 9,-2V4s-5,-1 -9,2zM12,6v15','M3,4c4,-1 6,0 9,2v15c-4,-3 -9,-2 -9,-2z','#8B88C8'),
'ic_doa':('M4,16l3,4h10l3,-4M4,16V9c0,-2 3,-2 3,0v4l3,3M20,16V9c0,-2 -3,-2 -3,0v4l-3,3M10,5l2,-2l2,2','M7,13l3,3h4l3,-3v7H7z','#C58DB0'),
'ic_hadith':('M4,3h14a2,2 0,0 1,2,2v16H5a2,2 0,0 1,-2,-2V5a2,2 0,0 1,1,-2M7,3v18M11,8h5M11,12h5','M7,3h11a2,2 0,0 1,2,2v16H7z','#D18D76'),
'ic_tasbih':('M12,3a8,8 0,1 0,8,8M12,3l3,3M18,3l3,3M20,11l-3,-3M12,19v3M9,22h6','M12,3a8,8 0,1 0,8,8l-3,-3l-2,-2z','#AD8BC8'),
'ic_calendar':('M5,4h14a2,2 0,0 1,2,2v14H3V6a2,2 0,0 1,2,-2M7,2v4M17,2v4M3,9h18M7,13h2M12,13h2M7,17h2','M3,9h18v11H3z','#C7A45B'),
'ic_compass':('M12,2a10,10 0,1 0,0,20a10,10 0,0 0,0,-20M16,8l-3,5l-5,3l3,-5z','M16,8l-3,5l-2,-2z','#B18A48'),
'ic_location':('M19,10c0,5 -7,11 -7,11S5,15 5,10a7,7 0,0 1,14,0M12,7a3,3 0,1 0,0,6a3,3 0,0 0,0,-6','M19,10c0,5 -7,11 -7,11S5,15 5,10a7,7 0,0 1,14,0z','#B58B54'),
'ic_bell':('M5,16h14l-2,-3V8a5,5 0,0 0,-10,0v5zM10,20h4','M7,8a5,5 0,0 1,10,0v5l2,3H5l2,-3z','#C39A5B'),
'ic_bookmark':('M6,3h12v18l-6,-4l-6,4z','','#B68E51'),
'ic_bookmark_filled':('M6,3h12v18l-6,-4l-6,4z','M6,3h12v18l-6,-4l-6,4z','#B68E51'),
'ic_play':('M8,4l12,8l-12,8z','M8,4l12,8l-12,8z','#AE7636'),
'ic_pause':('M8,5v14M16,5v14','','#AE7636'),
'ic_share':('M8,11l8,-5M8,13l8,5M5,9a3,3 0,1 0,0,6a3,3 0,0 0,0,-6M19,2a3,3 0,1 0,0,6a3,3 0,0 0,0,-6M19,16a3,3 0,1 0,0,6a3,3 0,0 0,0,-6','','#A28560'),
'ic_settings':('M4,6h9M17,6h3M4,12h3M11,12h9M4,18h10M18,18h2M15,4v4M9,10v4M16,16v4','','#9E8A71'),
'ic_info':('M12,2a10,10 0,1 0,0,20a10,10 0,0 0,0,-20M12,10v7M12,7v0.1','','#9E8A71'),
'ic_refresh':('M20,7v5h-5M4,17v-5h5M19,11a7,7 0,0 0,-12,-5M5,13a7,7 0,0 0,12,5','','#AC8149'),
'ic_check':('M5,12l4,4l10,-10','','#AC8149'),
'ic_chevron':('M9,5l7,7l-7,7','','#AC8149'),
'ic_search':('M10,3a7,7 0,1 0,0,14a7,7 0,0 0,0,-14M15,15l6,6','','#9C876B'),
'ic_reset':('M5,8a8,8 0,1 1,-1,7M5,3v5h5','','#AC8149')}
for name,(stroke,fill,color) in icons.items():
    content=(f'<path android:fillColor="{color}" android:fillAlpha="0.22" android:pathData="{fill}"/>' if fill else '')
    content+=f'<path android:strokeColor="{color}" android:strokeWidth="1.65" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000" android:pathData="{stroke}"/>'
    write(f'drawable/{name}.xml','<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">'+content+'</vector>')
write('drawable/ic_launcher.xml','''<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#A76C2E" android:pathData="M0,0h108v108H0z"/><path android:fillColor="#FFF3D8" android:pathData="M28,66h52v14H28zM32,62c0,-16 17,-16 22,-28c5,12 22,12 22,28zM20,43h5v37h-5zM83,43h5v37h-5zM20,39l2.5,-8l2.5,8zM83,39l2.5,-8l2.5,8z"/><path android:fillColor="#F6CC89" android:pathData="M63,17a8,8 0,1 0,5,13a6,6 0,1 1,-5,-13"/></vector>''')
print('Premium colors, typography, backgrounds and',len(icons),'vector icons generated')
