package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WidgetAppearanceV321Test {
  @Test
  public void exportNativePickerPreviewsAndAssertCompactSurfaces() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    File dir = new File(c.getExternalFilesDir(null), "widget-v321-previews");
    assertTrue(dir.isDirectory() || dir.mkdirs());
    LocalDate date = LocalDate.of(2026, 11, 3);
    List<LocalTime> times =
        Arrays.asList(
            LocalTime.of(5, 35),
            LocalTime.of(5, 45),
            LocalTime.of(7, 0),
            LocalTime.of(7, 25),
            LocalTime.of(13, 5),
            LocalTime.of(16, 25),
            LocalTime.of(19, 2),
            LocalTime.of(20, 15));
    PrayerDay day = new PrayerDay(date, "1448-05-23", times);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL)
        for (String theme : new String[] {"light", "dark"}) {
          int
              w =
                  d == WidgetProviderDescriptor.NEXT
                      ? 280
                      : d == WidgetProviderDescriptor.EXPANDED ? 360 : 480,
              h =
                  d == WidgetProviderDescriptor.NEXT
                      ? 300
                      : d == WidgetProviderDescriptor.EXPANDED
                          ? 400
                          : d == WidgetProviderDescriptor.STRIP ? 112 : 192;
          WidgetRenderModel model =
              new WidgetRenderModel(
                  new PrayerZone("Wilayah Persekutuan", "Kuala Lumpur, Putrajaya", "WLY01"),
                  day,
                  new PrayerDay(date.plusDays(1), "1448-05-24", times),
                  date.atTime(14, 0).atZone(PrayerClock.MALAYSIA),
                  d,
                  Collections.singletonMap("fajr", true));
          JSONObject json =
              new JSONObject()
                  .put("schemaVersion", 2)
                  .put("theme", theme)
                  .put("trackingEnabled", false);
          WidgetConfigV2 cfg = new WidgetConfigV2(json, d);
          scenario.onActivity(
              a -> {
                try {
                  Configuration cf = new Configuration(a.getResources().getConfiguration());
                  cf.fontScale = 1;
                  Context render = a.createConfigurationContext(cf);
                  RemoteViews remote = PrayerWidgets.render(render, 0, d, cfg, model, w, h, true);
                  remote.setTextViewText(R.id.widget_zone, "Kuala Lumpur");
                  remote.setTextViewText(R.id.widget_date, "Contoh · 3/11/2026");
                  View root = remote.apply(render, new FrameLayout(render));
                  root.measure(
                      View.MeasureSpec.makeMeasureSpec(Ui.dp(render, w), View.MeasureSpec.EXACTLY),
                      View.MeasureSpec.makeMeasureSpec(Ui.dp(render, h), View.MeasureSpec.EXACTLY));
                  root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                  assertNull(
                      "Normal preview keeps its family", root.findViewById(R.id.widget_message));
                  View surface = root.findViewById(R.id.widget_surface);
                  assertNotNull(surface);
                  assertTrue(surface.getHeight() > 0);
                  assertTrue(surface.getHeight() <= root.getHeight());
                  ImageView backdrop = root.findViewById(R.id.widget_backdrop);
                  assertNotNull(backdrop);
                  assertEquals(.10f, backdrop.getAlpha(), .001f);
                  assertFalse(backdrop.isClickable());
                  assertFalse(backdrop.isFocusable());
                  assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, backdrop.getImportantForAccessibility());
                  assertTrue(backdrop.getTop() >= 0 && backdrop.getBottom() <= surface.getHeight());
                  if (d == WidgetProviderDescriptor.STRIP)
                    assertTrue(
                        "Strip surface stays shallow", surface.getHeight() <= Ui.dp(render, 112));
                  if (d != WidgetProviderDescriptor.NEXT)
                    for (int index : PrayerDay.OBLIGATORY)
                      assertNotNull(
                          root.findViewById(
                              render
                                  .getResources()
                                  .getIdentifier(
                                      "widget_value_" + index, "id", render.getPackageName())));
                  Bitmap bitmap =
                      Bitmap.createBitmap(
                          surface.getWidth(), surface.getHeight(), Bitmap.Config.ARGB_8888);
                  Canvas canvas = new Canvas(bitmap);
                  canvas.translate(-surface.getLeft(), -surface.getTop());
                  root.draw(canvas);
                  try (FileOutputStream out =
                      new FileOutputStream(
                          new File(dir, d.component.getSimpleName() + "-" + theme + ".png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                  }
                  bitmap.recycle();
                } catch (Exception e) {
                  throw new AssertionError(e);
                }
              });
        }
    }
  }

  @Test
  public void allFourLauncherEntriesHaveRealPreviewImages() {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    int count = 0;
    for (android.appwidget.AppWidgetProviderInfo info :
        android.appwidget.AppWidgetManager.getInstance(c).getInstalledProviders())
      if (info.provider.getPackageName().equals(c.getPackageName())) {
        assertNotEquals("Picker must not fall back to app icon", 0, info.previewImage);
        android.graphics.drawable.Drawable image = info.loadPreviewImage(c, 0);
        assertNotNull(image);
        assertTrue("Native preview has real content dimensions", image.getIntrinsicWidth() >= 300);
        if (android.os.Build.VERSION.SDK_INT >= 31)
          assertEquals("Use image fallback instead of empty layout preview", 0, info.previewLayout);
        count++;
      }
    assertEquals(4, count);
  }

  @Test
  public void compactTwoCellStripFitsBothHourFormatsAndLanguages() throws Exception {
    Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    android.content.SharedPreferences prefs = c.getSharedPreferences("waktu_solat", 0);
    boolean hadHour = prefs.contains("twelve_hour"),
        oldHour = prefs.getBoolean("twelve_hour", false),
        hadLang = prefs.contains("language");
    String oldLang = prefs.getString("language", "ms");
    LocalDate date = LocalDate.of(2026, 11, 3);
    List<LocalTime> times =
        Arrays.asList(
            LocalTime.of(5, 35),
            LocalTime.of(5, 45),
            LocalTime.of(7, 0),
            LocalTime.of(7, 25),
            LocalTime.of(13, 5),
            LocalTime.of(16, 25),
            LocalTime.of(19, 2),
            LocalTime.of(20, 15));
    WidgetProviderDescriptor d = WidgetProviderDescriptor.STRIP;
    WidgetRenderModel model =
        new WidgetRenderModel(
            new PrayerZone("Wilayah Persekutuan", "Kuala Lumpur", "WLY01"),
            new PrayerDay(date, "1448-05-23", times),
            null,
            date.atTime(14, 0).atZone(PrayerClock.MALAYSIA),
            d,
            Collections.emptyMap());
    WidgetConfigV2 cfg =
        new WidgetConfigV2(new JSONObject().put("schemaVersion", 2).put("theme", "dark"), d);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      for (boolean twelve : new boolean[] {false, true})
        for (String lang : new String[] {"ms", "en"})
          for (int width : new int[] {280, 300, 320}) {
            prefs.edit().putBoolean("twelve_hour", twelve).putString("language", lang).commit();
            scenario.onActivity(
                a -> {
                  Configuration cf = new Configuration(a.getResources().getConfiguration());
                  cf.fontScale = 1;
                  Context render = a.createConfigurationContext(cf);
                  RemoteViews remote =
                      PrayerWidgets.render(render, 0, d, cfg, model, width, 130, true);
                  View root = remote.apply(render, new FrameLayout(render));
                  root.measure(
                      View.MeasureSpec.makeMeasureSpec(
                          Ui.dp(render, width), View.MeasureSpec.EXACTLY),
                      View.MeasureSpec.makeMeasureSpec(
                          Ui.dp(render, 130), View.MeasureSpec.EXACTLY));
                  root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                  assertNull(
                      "Two-cell strip retains all five prayers: "
                          + width
                          + " "
                          + lang
                          + " "
                          + twelve,
                      root.findViewById(R.id.widget_message));
                  for (int index : PrayerDay.OBLIGATORY) {
                    TextView value =
                        root.findViewById(
                            render
                                .getResources()
                                .getIdentifier(
                                    "widget_value_" + index, "id", render.getPackageName()));
                    assertNotNull(value);
                    assertTrue(
                        value.getPaint().measureText(value.getText().toString())
                            <= value.getWidth()
                                - value.getCompoundPaddingLeft()
                                - value.getCompoundPaddingRight()
                                + 1);
                    TextView period =
                        root.findViewById(
                            render
                                .getResources()
                                .getIdentifier(
                                    "widget_period_" + index, "id", render.getPackageName()));
                    assertNotNull(period);
                    assertEquals(twelve ? View.VISIBLE : View.GONE, period.getVisibility());
                  }
                  assertTrue(
                      root.findViewById(R.id.widget_surface).getHeight() <= Ui.dp(render, 130));
                });
          }
    } finally {
      android.content.SharedPreferences.Editor e = prefs.edit();
      if (hadHour) e.putBoolean("twelve_hour", oldHour);
      else e.remove("twelve_hour");
      if (hadLang) e.putString("language", oldLang);
      else e.remove("language");
      e.commit();
    }
  }
}
