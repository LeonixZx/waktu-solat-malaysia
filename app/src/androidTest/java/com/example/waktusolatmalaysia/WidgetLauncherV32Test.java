package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.content.*;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import org.junit.*;
import org.junit.runner.RunWith;

/** Opt-in real launcher placement. Leaves only newly placed app widgets for visual inspection. */
@RunWith(AndroidJUnit4.class)
public class WidgetLauncherV32Test {
  @Test
  public void requestFirstWidgetThroughProductionGallery() throws Exception {
    org.junit.Assume.assumeTrue(
        InstrumentationRegistry.getArguments().getString("launcher", "").equals("true"));
    ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class);
    s.onActivity(a -> a.openDestination(R.id.widgetsFragment, null));
    SystemClock.sleep(1000);
    s.onActivity(
        a -> {
          android.widget.FrameLayout preview = a.findViewById(R.id.widget_preview);
          assertTrue("Native preview populated", preview.getChildCount() > 0);
          assertTrue("Preview measured", preview.getChildAt(0).getWidth() > 0);
        });
    TabletFeatureTest.capture("v32-gallery");
    UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    UiObject2 add = device.wait(Until.findObject(By.desc("Tambah Solat seterusnya")), 5000);
    if (add == null) add = device.wait(Until.findObject(By.desc("Add Next prayer")), 5000);
    assertNotNull(add);
    add.click();
    SystemClock.sleep(1000);
    TabletFeatureTest.capture("v32-pin-request");
    SystemClock.sleep(
        45000); // Keep the test session alive while the host-side script inspects the launcher
    // dialog.
  }
 @Test public void removeOnlyConfirmedTemporaryConfigurations(){String list=InstrumentationRegistry.getArguments().getString("cleanup","");org.junit.Assume.assumeTrue(!list.isEmpty());android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();for(String key:list.split(",")){int id=Integer.parseInt(key);assertNull("Never delete a placed widget",android.appwidget.AppWidgetManager.getInstance(c).getAppWidgetInfo(id));com.example.waktusolatmalaysia.widgets.WidgetConfigV2.remove(c,id);}}

}
