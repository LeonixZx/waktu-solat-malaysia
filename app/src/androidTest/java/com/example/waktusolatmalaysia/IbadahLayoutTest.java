package com.example.waktusolatmalaysia;
import static org.junit.Assert.*;
import android.view.*;
import android.widget.*;
import android.graphics.Rect;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.preferences.UserStore;
import com.example.waktusolatmalaysia.ui.Ui;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IbadahLayoutTest {
  @Test public void ibadahControlsAndAdaptiveReaderFitTheAvailableSpace() throws Exception {
    var c=InstrumentationRegistry.getInstrumentation().getTargetContext();
    UserStore store=new UserStore(c);String original=store.text("collection_tab","doa");
    String label=InstrumentationRegistry.getArguments().getString("capture","v31-layout");
    boolean multi=Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("expectMultiWindow","false"));
    int originalNight=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
    if(Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("night","false")))
      androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
    androidx.test.uiautomator.UiDevice device=androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    if(multi)device.executeShellCommand("am start -n com.android.settings/.Settings");
    try(var s=ActivityScenario.launch(MainActivity.class)) {
      if(multi) {
        device.pressRecentApps();
        var icon=device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.res("com.google.android.apps.nexuslauncher","icon")),5000);
        assertNotNull("Recent app icon",icon);icon.click();
        var split=device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.text("Split screen")),5000);
        assertNotNull("Real split-screen command",split);split.click();
        device.wait(androidx.test.uiautomator.Until.gone(androidx.test.uiautomator.By.text("Split screen")),5000);
        android.os.SystemClock.sleep(350);
        device.click(device.getDisplayWidth()/2,device.getDisplayHeight()/2);
        V3FeaturesTest.waitFor(s,MainActivity::isInMultiWindowMode);
        Rect bounds=new Rect();s.onActivity(a->a.findViewById(R.id.nav_host).getGlobalVisibleRect(bounds));
        device.click(bounds.centerX(),bounds.top+10);
        V3FeaturesTest.waitFor(s,MainActivity::hasWindowFocus);
      }
      store.set("collection_tab","wirid");
      for(int id:new int[]{R.id.collectionFragment,R.id.worshipFragment,R.id.tasbihFragment,R.id.tasksFragment,R.id.calendarFragment,R.id.widgetsFragment,R.id.qiblaFragment,R.id.quranFragment}) {
        V3FeaturesTest.open(s,id);android.os.SystemClock.sleep(350);
        if(id==R.id.collectionFragment)V3FeaturesTest.waitFor(s,a->V3FeaturesTest.count(a,R.id.collection_list)==14);
        s.onActivity(a->{
          if(Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("expectMultiWindow","false")))assertTrue(a.isInMultiWindowMode());
          View host=a.findViewById(R.id.nav_host);assertTrue(host.getWidth()>0&&host.getHeight()>0);
          if(id==R.id.collectionFragment) {
            androidx.recyclerview.widget.RecyclerView rows=a.findViewById(R.id.collection_list);
            assertTrue("A visible collection row is required", rows.getChildCount()>0);
            for(int rowIndex=0;rowIndex<rows.getChildCount();rowIndex++) {
              View row=rows.getChildAt(rowIndex);
              assertEquals("Collection rows fill the list width", rows.getWidth(), row.getWidth());
              assertTrue("A reading title must have visible width", hasReadableTitle(row));
            }
            float density=a.getResources().getDisplayMetrics().density;
            View detail=a.findViewById(R.id.library_detail);
            boolean expected=host.getWidth()/density>=680 && (host.getHeight()-host.getPaddingBottom())/density>=480
                && a.getResources().getConfiguration().fontScale<=1.3f;
            assertEquals("Reader pane must follow the content width",expected?View.VISIBLE:View.GONE,detail.getVisibility());
            if(expected) {assertTrue(detail.getWidth()>=Ui.dp(a,340));assertNotNull(a.findViewById(R.id.reading_bookmark));}
          }
          checkText(host);
        });
        if(id!=R.id.quranFragment)TabletFeatureTest.capture(label+"-"+aName(id));
        if(id==R.id.tasksFragment) {
          androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.task_add)).perform(TabletFeatureTest.reveal(),androidx.test.espresso.action.ViewActions.click());
          androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.v31_task_offset)).perform(TabletFeatureTest.reveal()).check((view,error)->{
            if(error!=null)throw error;checkText(view.getRootView());assertTrue(view.getWidth()>0);
          });
          TabletFeatureTest.capture(label+"-task-form");
          androidx.test.espresso.Espresso.pressBack();
        }
      }
    } finally {c.getSharedPreferences("personal_library",0).edit().putString("collection_tab",original).commit();androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(originalNight);if(multi)device.pressHome();}
  }
  private static String aName(int id) {return id==R.id.collectionFragment?"collection":id==R.id.worshipFragment?"worship":id==R.id.tasbihFragment?"tasbih":id==R.id.tasksFragment?"tasks":id==R.id.calendarFragment?"calendar":id==R.id.widgetsFragment?"widgets":"qibla";}
  private static boolean hasReadableTitle(View view) {
    if(view instanceof TextView && !((TextView)view).getText().toString().isEmpty() && view.getWidth()>100 && view.getHeight()>0)return true;
    if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)if(hasReadableTitle(((ViewGroup)view).getChildAt(i)))return true;
    return false;
  }
  private static void checkText(View view) {
    if(view instanceof TextView && !(view instanceof EditText) && view.isShown() && view.getGlobalVisibleRect(new Rect())) {
      TextView t=(TextView)view;
      if(t.getLayout()!=null && t.getMaxLines()!=1 && t.getEllipsize()==null)
        assertTrue("Clipped text: "+t.getText(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom()+2);
    }
    if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checkText(((ViewGroup)view).getChildAt(i));
  }
}
