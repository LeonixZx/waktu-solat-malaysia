package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;

/** Date fixtures are kept outside production timetable caches. */
public class WidgetDatesV333Test {
  private final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  private Map<String,?> original;
  @Before public void snapshot(){original=new HashMap<>(c.getSharedPreferences("waktu_solat",0).getAll());}
  @After public void restore(){
    SharedPreferences.Editor e=c.getSharedPreferences("waktu_solat",0).edit().clear();
    for(var x:original.entrySet()){
      Object v=x.getValue();String k=x.getKey();
      if(v instanceof String)e.putString(k,(String)v);
      else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
      else if(v instanceof Integer)e.putInt(k,(Integer)v);
      else if(v instanceof Long)e.putLong(k,(Long)v);
      else if(v instanceof Float)e.putFloat(k,(Float)v);
      else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));
    }e.commit();
  }
  private PrayerDay day(LocalDate date,String hijri){
    return new PrayerDay(date,hijri,Arrays.asList(LocalTime.of(5,35),LocalTime.of(5,48),
        LocalTime.of(7,0),LocalTime.of(7,25),LocalTime.of(13,0),LocalTime.of(16,20),
        LocalTime.of(18,58),LocalTime.of(20,9)));
  }
  private View render(Context a,WidgetProviderDescriptor d,String style,String theme,
      float w,float h,LocalDate date,String hijri,int hour,boolean showHijri)throws Exception{
    WidgetRenderModel model=new WidgetRenderModel(new PrayerZone("Wilayah Persekutuan",
        "Kuala Lumpur, Putrajaya","WLY01"),day(date,hijri),day(date.plusDays(1),"1448-05-24"),
        date.atTime(hour,0).atZone(PrayerClock.MALAYSIA),d,Collections.emptyMap());
    WidgetConfigV2 cfg=new WidgetConfigV2(new JSONObject().put("schemaVersion",2)
        .put("layoutStyle",style).put("theme",theme).put("showHijri",showHijri),d);
    View root=PrayerWidgets.render(a,0,d,cfg,model,w,h,true).apply(a,new FrameLayout(a));
    root.measure(View.MeasureSpec.makeMeasureSpec(Ui.dp(a,w),View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(Ui.dp(a,h),View.MeasureSpec.EXACTLY));
    root.layout(0,0,root.getMeasuredWidth(),root.getMeasuredHeight());
    assertTrue(PrayerWidgets.fits(a,PrayerWidgets.render(a,0,d,cfg,model,w,h,true),w,h));
    return root;
  }
  private String text(View v,int id){TextView t=v.findViewById(id);return t.getText().toString();}
  private void export(View root,String name)throws Exception{
    Bitmap b=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(b));File dir=new File(c.getExternalFilesDir(null),"widget-v333");dir.mkdirs();
    try(var out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
  }

  @Test public void compactAndSpaciousDailyShowBothDatesWithoutLosingTheCurve()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      for(String language:new String[]{"ms","en"})for(String theme:new String[]{"light","dark"}){
        c.getSharedPreferences("waktu_solat",0).edit().putString("language",language).commit();
        scenario.onActivity(a->{try{
          for(float[] size:new float[][]{{277.57007f,137.90343f},{360,180}}){
            View root=render(a,WidgetProviderDescriptor.DAILY,"timeline",theme,size[0],size[1],
                LocalDate.of(2026,11,3),"1448-05-23",8,true);
            assertNotNull("Keep the compact curved row",root.findViewById(R.id.widget_arc_curve));
            String visible=text(root,R.id.widget_date);
            if(root.findViewById(R.id.widget_hijri).getVisibility()==View.VISIBLE)
              visible+=" "+text(root,R.id.widget_hijri);
            assertTrue(visible,visible.contains("1448"));
            assertEquals(text(root,R.id.widget_hijri), WidgetDates.hijri("1448-05-23",language.equals("en")));
            assertTrue(visible,visible.contains("3 Nov 2026"));
            assertTrue(root.findViewById(R.id.widget_date).getContentDescription().toString().contains("Hijri (JAKIM)"));
            export(root,"daily-"+(size[0]<300?"compact":"spacious")+"-"+language+"-"+theme);
            View longestMonth=render(a,WidgetProviderDescriptor.DAILY,"timeline",theme,size[0],size[1],
                LocalDate.of(2026,11,3),"1448-06-23",8,true);
            assertNotNull(longestMonth.findViewById(R.id.widget_arc_curve));
            assertEquals(View.VISIBLE,longestMonth.findViewById(R.id.widget_hijri).getVisibility());
            assertEquals(WidgetDates.hijri("1448-06-23",language.equals("en")),text(longestMonth,R.id.widget_hijri));
          }
        }catch(Exception e){throw new AssertionError(e);}});
      }
    }
  }

  @Test public void expandedBothStylesShowReadableHijriAndRespectDisabledOption()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      for(String language:new String[]{"ms","en"})for(String style:new String[]{"timeline","classic"})
        for(String theme:new String[]{"light","dark"}){
          c.getSharedPreferences("waktu_solat",0).edit().putString("language",language).commit();
          scenario.onActivity(a->{try{
            View root=render(a,WidgetProviderDescriptor.EXPANDED,style,theme,360,460,
                LocalDate.of(2026,11,3),"1448-05-23",8,true);
            assertEquals(View.VISIBLE,root.findViewById(R.id.widget_hijri).getVisibility());
            assertTrue(text(root,R.id.widget_hijri).contains(language.equals("ms")?"Jamadil Awal":"Jumada al-Ula"));
            assertTrue(text(root,R.id.widget_date).contains("2026"));
            export(root,"expanded-"+style+"-"+language+"-"+theme);
            View disabled=render(a,WidgetProviderDescriptor.EXPANDED,style,theme,360,460,
                LocalDate.of(2026,11,3),"1448-05-23",8,false);
            assertEquals(View.GONE,disabled.findViewById(R.id.widget_hijri).getVisibility());
          }catch(Exception e){throw new AssertionError(e);}});
        }
    }
  }

  @Test public void dailyPairsTheOfficialRowAtMidnightAndNeverInventsMissingHijri()throws Exception{
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      scenario.onActivity(a->{try{
        for(int hour:new int[]{19,23}){
          View root=render(a,WidgetProviderDescriptor.DAILY,"timeline","dark",277.57007f,137.90343f,
              LocalDate.of(2026,11,3),"1448-05-23",hour,true);
          assertTrue(text(root,R.id.widget_date).contains("3 Nov 2026"));
          assertTrue(text(root,R.id.widget_hijri).contains("23 "));
          assertFalse(text(root,R.id.widget_hijri).contains("/"));
        }
        View midnight=render(a,WidgetProviderDescriptor.DAILY,"timeline","dark",277.57007f,137.90343f,
            LocalDate.of(2026,11,4),"1448-05-24",0,true);
        assertTrue(text(midnight,R.id.widget_date).contains("4 Nov 2026"));
        assertTrue(text(midnight,R.id.widget_hijri).startsWith("24 "));
        for(String value:new String[]{"","1448-13-23"}){
          View missing=render(a,WidgetProviderDescriptor.DAILY,"timeline","dark",277.57007f,137.90343f,
              LocalDate.of(2026,11,3),value,8,true);
          assertFalse(text(missing,R.id.widget_date).contains(" H"));
          assertEquals(View.GONE,missing.findViewById(R.id.widget_hijri).getVisibility());
        }
      }catch(Exception e){throw new AssertionError(e);}});
    }
  }

  @Test public void allFamiliesUseLocationNamesAtPreferredAndCompactSizes()throws Exception{
    c.getSharedPreferences("waktu_solat",0).edit().putString("language","ms").putBoolean("twelve_hour",false).commit();
    try(var scenario=ActivityScenario.launch(MainActivity.class)){
      for(WidgetProviderDescriptor d:WidgetProviderDescriptor.ALL)
        for(String style:new String[]{"timeline","classic"})
          for(String theme:new String[]{"light","dark"}){
            scenario.onActivity(a->{try{
              for(float[] size:new float[][]{{d.width,d.height},{d==WidgetProviderDescriptor.NEXT?180:277.57007f,
                  d==WidgetProviderDescriptor.EXPANDED?300:d==WidgetProviderDescriptor.NEXT?180:d.preferStrip?82:137.90343f}}){
                View root=render(a,d,style,theme,size[0],size[1],LocalDate.of(2026,11,3),"1448-05-23",8,true);
                assertTrue(text(root,R.id.widget_zone).contains("Kuala Lumpur"));
                assertNoVisibleZoneCode(root);
                if(style.equals("timeline")&&size[0]==d.width&&size[1]==d.height)
                  export(root,"picker-"+d.component.getSimpleName()+"-"+theme);
              }
            }catch(Exception e){throw new AssertionError(e);}});
          }
    }
  }
  private void assertNoVisibleZoneCode(View v){
    if(v.getVisibility()!=View.VISIBLE)return;
    if(v instanceof TextView)assertFalse(((TextView)v).getText().toString().contains("WLY01"));
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)assertNoVisibleZoneCode(((ViewGroup)v).getChildAt(i));
  }
}
