package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.allOf;
import android.content.*;
import android.hardware.SensorManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.utils.*;
import com.google.android.material.tabs.TabLayout;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IbadahV31Test {
  private final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  private Map<String,?> appBefore, personalBefore;
  @Before public void snapshot() {
    appBefore=new HashMap<>(c.getSharedPreferences("waktu_solat",0).getAll());
    personalBefore=new HashMap<>(c.getSharedPreferences("personal_library",0).getAll());
    com.example.waktusolatmalaysia.ui.I18n.language(c,"ms");
  }
  @After public void restore() {
    V3FeaturesTest.restorePrefs(c.getSharedPreferences("waktu_solat",0),appBefore);
    V3FeaturesTest.restorePrefs(c.getSharedPreferences("personal_library",0),personalBefore);
  }
  private static void waitFor(ActivityScenario<MainActivity> s, java.util.function.Predicate<MainActivity> p) { V3FeaturesTest.waitFor(s,p); }
  private static boolean text(View v,String text) {
    if(v instanceof TextView && ((TextView)v).getText().toString().equals(text))return true;
    if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(text(((ViewGroup)v).getChildAt(i),text))return true;
    return false;
  }
  private static String capturePrefix() {return InstrumentationRegistry.getArguments().getString("capture","v31");}
  @Test public void collectionSelectionSearchAndReaderSurviveRecreation() throws Exception {
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(s,R.id.collectionFragment);
      waitFor(s,a->a.findViewById(R.id.collection_tabs)!=null);
      onView(allOf(withText("Wirid"),isDisplayed(),isDescendantOfA(withId(R.id.collection_tabs)))).perform(click());
      waitFor(s,a->V3FeaturesTest.count(a,R.id.collection_list)==14);
      onView(withId(R.id.collection_search)).perform(replaceText("Istighfar"),closeSoftKeyboard());
      waitFor(s,a->V3FeaturesTest.count(a,R.id.collection_list)==1);
      onView(allOf(withText("Istighfar"),isDescendantOfA(withId(R.id.collection_list)))).perform(TabletFeatureTest.reveal(),click());
      waitFor(s,a->a.findViewById(R.id.reading_bookmark)!=null);
      s.onActivity(a->{assertTrue(text(a.findViewById(R.id.library_detail),"Rumi"));
        assertTrue(text(a.findViewById(R.id.library_detail),"Terjemahan · Bahasa Melayu"));});
      boolean saved=new UserStore(c).saved("wirid:0");
      onView(withId(R.id.reading_bookmark)).perform(click());assertEquals(!saved,new UserStore(c).saved("wirid:0"));
      s.recreate();waitFor(s,a->a.findViewById(R.id.reading_bookmark)!=null);
      s.onActivity(a->{assertEquals("Istighfar",((EditText)a.findViewById(R.id.collection_search)).getText().toString());
        assertEquals(!saved,a.findViewById(R.id.reading_bookmark).isSelected());});
      TabletFeatureTest.capture(capturePrefix()+"-collection");
    }
  }
  @Test public void everySuppliedReadingRendersAllItsSectionsOffline() throws Exception {
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      for(String kind:new String[]{"doa","wirid","niat","hadith"}) {
        var rows=new ContentRepository(c).readings(kind);
        for(int i=0;i<rows.size();i++) {
          final int index=i;var reading=rows.get(i);
          s.onActivity(a->{Bundle b=new Bundle();b.putString("kind",kind);b.putString("reading_id",reading.id);b.putInt("index",index);a.openDestination(R.id.readingFragment,b);});
          waitFor(s,a->a.findViewById(R.id.reading_bookmark)!=null);
          s.onActivity(a->{for(var section:reading.sections)assertTrue("Missing Arabic section "+reading.id+":"+section.id,text(a.findViewById(R.id.nav_host),section.arabic));});
          if(i==0)TabletFeatureTest.capture(capturePrefix()+"-"+kind);
          s.onActivity(a->assertTrue(a.onSupportNavigateUp()));
        }
      }
    }
  }
  @Test public void tasbihUndoValidationResetAndDailyRecordsPersist() throws Exception {
    UserStore store=new UserStore(c);int before=store.number("tasbih_count",0);
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(s,R.id.tasbihFragment);
      onView(withId(R.id.tasbih_tap)).perform(TabletFeatureTest.reveal(),click(),click());
      onView(withText("Undur satu")).perform(TabletFeatureTest.reveal(),click());
      assertEquals(before+1,store.number("tasbih_count",0));
      onView(withId(R.id.tasbih_goal)).perform(TabletFeatureTest.reveal(),click());
      onView(withId(R.id.tasbih_goal_input)).perform(replaceText("0"),closeSoftKeyboard());
      onView(withText("Simpan")).perform(click());onView(withId(R.id.tasbih_goal_input)).check(matches(isDisplayed()));
      onView(withId(R.id.tasbih_goal_input)).perform(replaceText("99"),closeSoftKeyboard());onView(withText("Simpan")).perform(click());
      s.recreate();assertEquals(99,store.number("tasbih_goal",0));assertEquals(before+1,store.number("tasbih_count",0));
      onView(withId(R.id.tasbih_reset)).perform(TabletFeatureTest.reveal(),click());onView(withText("Batal")).perform(click());
      assertEquals(before+1,store.number("tasbih_count",0));
      onView(withId(R.id.tasbih_reset)).perform(TabletFeatureTest.reveal(),click());
      onView(allOf(withText("Mula semula"),isDisplayed(),withClassName(org.hamcrest.Matchers.endsWith("Button")))).perform(click());
      assertEquals(0,store.number("tasbih_count",-1));
      onView(withText("Saya berpuasa hari ini")).perform(TabletFeatureTest.reveal(),click());
      assertEquals(!Boolean.TRUE.equals(personalBefore.get("fasted_"+java.time.LocalDate.now(PrayerClock.MALAYSIA))),store.flag("fasted_"+java.time.LocalDate.now(PrayerClock.MALAYSIA),false));
      TabletFeatureTest.capture(capturePrefix()+"-tasbih");
    }
  }
  @Test public void allIbadahScreensStayReachableAndCaptureTheirInterfaces() throws Exception {
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      int[] screens={R.id.worshipFragment,R.id.tasksFragment,R.id.calendarFragment,R.id.widgetsFragment,R.id.qiblaFragment};
      String[] names={"worship","tasks","calendar","widgets","qibla"};
      for(int i=0;i<screens.length;i++) {
        V3FeaturesTest.open(s,screens[i]);SystemClock.sleep(350);
        s.onActivity(a->{View host=a.findViewById(R.id.nav_host);assertTrue(host.getWidth()>0&&host.getHeight()>0);});
        TabletFeatureTest.capture(capturePrefix()+"-"+names[i]);
      }
    }
  }
  @Test public void reminderValidationCreateEditDisableAndDelete() throws Exception {
    TaskStore tasks=new TaskStore(c);var before=tasks.load();
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      V3FeaturesTest.open(s,R.id.tasksFragment);
      onView(withId(R.id.task_add)).perform(TabletFeatureTest.reveal(),click());
      onView(withId(R.id.v31_task_title)).perform(replaceText("V31 verification task"),closeSoftKeyboard());
      onView(withId(R.id.v31_task_offset)).perform(TabletFeatureTest.reveal(),replaceText("361"),closeSoftKeyboard());
      onView(withId(R.id.v31_task_enabled)).perform(TabletFeatureTest.reveal(),click());
      onView(withText("Simpan")).perform(click());
      onView(withId(R.id.v31_task_title)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
      onView(withId(R.id.v31_task_offset)).perform(TabletFeatureTest.reveal(),replaceText("360"),closeSoftKeyboard());
      onView(withId(R.id.v31_task_anchor)).perform(TabletFeatureTest.reveal(), new androidx.test.espresso.ViewAction() {
        public org.hamcrest.Matcher<View> getConstraints(){return isAssignableFrom(Spinner.class);}
        public String getDescription(){return "select Isha anchor";}
        public void perform(androidx.test.espresso.UiController ui,View view){((Spinner)view).setSelection(7);ui.loopMainThreadUntilIdle();}
      });
      TabletFeatureTest.capture(capturePrefix()+"-task-form");
      onView(withText("Simpan")).perform(click());
      var created=tasks.load().stream().filter(t->t.title.equals("V31 verification task")).findFirst().orElseThrow();
      assertFalse(created.enabled);assertEquals(360,created.offsetMinutes);assertEquals(7,created.anchor);
      onView(allOf(withContentDescription("Sunting"),isDescendantOfA(withTagValue(org.hamcrest.Matchers.is("task:"+created.id))))).perform(TabletFeatureTest.reveal(),click());
      onView(withId(R.id.v31_task_offset)).perform(TabletFeatureTest.reveal(),replaceText("30"),closeSoftKeyboard());
      onView(withText("Simpan")).perform(click());
      s.recreate();var edited=tasks.load().stream().filter(t->t.id.equals(created.id)).findFirst().orElseThrow();
      assertEquals(30,edited.offsetMinutes);assertFalse(edited.enabled);
      onView(allOf(withContentDescription("Padam"),isDescendantOfA(withTagValue(org.hamcrest.Matchers.is("task:"+created.id))))).perform(TabletFeatureTest.reveal(),click());
      onView(withText("Padam")).perform(click());assertFalse(tasks.load().stream().anyMatch(t->t.id.equals(created.id)));
    } finally {tasks.replace(before);}
  }
  @Test public void englishInterfaceLabelsTheSuppliedMalayFallback() throws Exception {
    com.example.waktusolatmalaysia.ui.I18n.language(c,"en");new UserStore(c).set("collection_tab","wirid");
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      s.onActivity(a->{Bundle b=new Bundle();b.putString("kind","wirid");b.putInt("index",0);a.openDestination(R.id.readingFragment,b);});
      waitFor(s,a->a.findViewById(R.id.reading_bookmark)!=null);
      s.onActivity(a->{assertTrue(text(a.findViewById(R.id.nav_host),"English translation unavailable. The supplied translation is labelled below."));
        assertTrue(text(a.findViewById(R.id.nav_host),"Translation · Bahasa Melayu"));});
      TabletFeatureTest.capture(capturePrefix()+"-english-reading");
    }
  }
  @Test public void sensorCoordinatesMatchAllFourDisplayRotationsAndFallback() {
    float[] out=new float[9],angle=new float[3];
    for(int north:new int[]{0,90,180,270})for(int rotation=0;rotation<4;rotation++) {
      double r=Math.toRadians(north);float co=(float)Math.cos(r),si=(float)Math.sin(r);
      float[] matrix={co,si,0,-si,co,0,0,0,1};
      assertTrue(QiblaOrientation.remap(matrix,rotation,out));SensorManager.getOrientation(out,angle);
      assertEquals("natural heading="+north+", display rotation="+rotation,0,
          QiblaState.delta(Math.toDegrees(angle[0]),QiblaState.normalize(north+rotation*90)),.01);
      assertEquals(1,out[8],.001);
    }
    float[] matrix=new float[9];assertTrue(SensorManager.getRotationMatrix(matrix,null,new float[]{0,0,9.81f},new float[]{0,50,0}));
    SensorManager.getOrientation(matrix,angle);assertEquals(0,Math.toDegrees(angle[0]),.001);
  }
}
