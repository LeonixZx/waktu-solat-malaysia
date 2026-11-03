package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.util.SizeF;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.BackupStore;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WidgetHostTest {
  @Test
  public void actualHostBindsResizesAndKeepsIndependentZones() throws Exception {
    assumeTrue(
        "Opt-in emulator binding test",
        InstrumentationRegistry.getArguments().getString("widgets", "").equals("true"));
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    AppWidgetHost host = new AppWidgetHost(c, 30003);
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    String appZone = new AppPreferences(c).zone().code;
    List<Integer> ids = new ArrayList<>();
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      host.startListening();
      for (int kind = 0; kind < 3; kind++) {
        int id = host.allocateAppWidgetId();
        ids.add(id);
        final int type = kind;
        assertTrue(
            manager.bindAppWidgetIdIfAllowed(
                id, new ComponentName(c, PrayerWidgets.PROVIDERS[kind])));
        c.getSharedPreferences("widget_config", 0)
            .edit()
            .putString(
                String.valueOf(id),
                new JSONObject()
                    .put("followApp", false)
                    .put("zone", kind == 1 ? "SGR03" : "WLY01")
                    .put("name", "Test zone")
                    .put("theme", "light")
                    .toString())
            .commit();
        for (int[] size : new int[][] {{180, 180}, {320, 200}, {320, 380}}) {
          Bundle options = new Bundle();
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, size[0]);
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, size[1]);
          if (Build.VERSION.SDK_INT >= 31)
            options.putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                new ArrayList<>(List.of(new SizeF(size[0], size[1]))));
          manager.updateAppWidgetOptions(id, options);
          PrayerWidgets.update(c, id, kind);
          scenario.onActivity(
              a -> {
                AppWidgetHostView view = host.createView(a, id, manager.getAppWidgetInfo(id));
                assertNotNull(view);
                assertTrue(view.getChildCount() > 0);
              });
        }
        final java.util.concurrent.atomic.AtomicReference<android.widget.FrameLayout> preview =
            new java.util.concurrent.atomic.AtomicReference<>();
        scenario.onActivity(
            a -> {
              AppWidgetHostView v = host.createView(a, id, manager.getAppWidgetInfo(id));
              android.widget.FrameLayout stage=new android.widget.FrameLayout(a);
              stage.setBackgroundColor(com.example.waktusolatmalaysia.ui.Ui.color(a,com.example.waktusolatmalaysia.R.color.background));
              stage.addView(v,new android.widget.FrameLayout.LayoutParams(
                  com.example.waktusolatmalaysia.ui.Ui.dp(a,320),
                  com.example.waktusolatmalaysia.ui.Ui.dp(a,type==2?380:200),android.view.Gravity.CENTER));
              a.addContentView(
                  stage,new android.view.ViewGroup.LayoutParams(-1,-1));
              preview.set(stage);
            });
        SystemClock.sleep(250);
        scenario.onActivity(a->{
          for(int key:new int[]{R.id.widget_zone,R.id.widget_next,R.id.widget_time}) {
            TextView value=preview.get().findViewById(key);
            if(value==null||!value.isShown())continue;
            android.graphics.Rect bounds=new android.graphics.Rect();
            assertTrue("Essential widget text is visible",value.getGlobalVisibleRect(bounds));
            assertEquals("Essential widget text is not clipped",value.getHeight(),bounds.height());
            assertTrue("Widget text fits its view",value.getLayout()!=null&&value.getLayout().getHeight()<=value.getHeight()-value.getCompoundPaddingTop()-value.getCompoundPaddingBottom()+2);
          }
        });
        TabletFeatureTest.capture("widget-host-" + kind);
        scenario.onActivity(
            a -> ((android.view.ViewGroup) preview.get().getParent()).removeView(preview.get()));
        assertEquals(kind == 1 ? "SGR03" : "WLY01", PrayerWidgets.zone(c, id).code);
      }
      assertEquals(appZone, new AppPreferences(c).zone().code);
      try (var out =
          new java.io.FileOutputStream(
              new java.io.File(c.getExternalFilesDir(null), "android-backup.json"))) {
        out.write(
            BackupStore.export(c).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      java.io.File web = new java.io.File(c.getExternalFilesDir(null), "web-backup.json");
      if (web.exists())
        BackupStore.validate(
            new JSONObject(
                new String(
                    java.nio.file.Files.readAllBytes(web.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8)));
    } finally {
      for (int id : ids)
        c.getSharedPreferences("widget_config", 0).edit().remove(String.valueOf(id)).commit();
      host.stopListening();
      host.deleteHost();
    }
  }
}
