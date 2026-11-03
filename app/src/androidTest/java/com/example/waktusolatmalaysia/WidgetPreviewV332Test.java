package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.SizeF;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import com.google.android.material.tabs.TabLayout;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.*;
import org.junit.Test;

public class WidgetPreviewV332Test {
  private final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();

  private Bundle options(float w, float h) {
    Bundle b = new Bundle();
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, (int) w);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (int) w);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, (int) h);
    b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (int) h);
    if (Build.VERSION.SDK_INT >= 31)
      b.putParcelableArrayList(
          AppWidgetManager.OPTION_APPWIDGET_SIZES,
          new ArrayList<>(Collections.singletonList(new SizeF(w, h))));
    return b;
  }

  private void layout(View v, int w, int h) {
    v.measure(
        View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
    v.layout(0, 0, w, h);
  }

  private RemoteViews fixture(Context context, float w, float h) throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 3);
    List<LocalTime> times =
        Arrays.asList(
            LocalTime.of(5, 35),
            LocalTime.of(5, 48),
            LocalTime.of(7, 0),
            LocalTime.of(7, 25),
            LocalTime.of(13, 0),
            LocalTime.of(16, 20),
            LocalTime.of(18, 58),
            LocalTime.of(20, 9));
    WidgetProviderDescriptor d = WidgetProviderDescriptor.DAILY;
    WidgetRenderModel m =
        new WidgetRenderModel(
            new PrayerZone("Wilayah Persekutuan", "Kuala Lumpur, Putrajaya", "WLY01"),
            new PrayerDay(date, "1448-05-23", times),
            new PrayerDay(date.plusDays(1), "1448-05-24", times),
            date.atTime(8, 0).atZone(PrayerClock.MALAYSIA),
            d,
            Collections.emptyMap());
    return PrayerWidgets.render(
        context,
        0,
        d,
        new WidgetConfigV2(
            new JSONObject()
                .put("schemaVersion", 2)
                .put("layoutStyle", "timeline")
                .put("theme", "dark"),
            d),
        m,
        w,
        h,
        true);
  }

  private Bitmap bitmap(View root) {
    Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(b));
    return b;
  }

  private void save(Bitmap b, String name) throws Exception {
    File dir = new File(c.getExternalFilesDir(null), "widget-v332");
    dir.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
  }

  @Test
  public void reportedPairsAreUsedInsteadOfSyntheticExtrema() {
    WidgetProviderDescriptor d = WidgetProviderDescriptor.DAILY;
    Bundle b = options(277.57007f, 137.90343f);
    SizeF selected = PrayerWidgets.presentationSize(b, false, d);
    if (Build.VERSION.SDK_INT >= 31) assertEquals(new SizeF(277.57007f, 137.90343f), selected);
    else assertEquals(new SizeF(277, 137), selected);
    if (Build.VERSION.SDK_INT >= 31) {
      b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 600);
      b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180);
      b.putParcelableArrayList(
          AppWidgetManager.OPTION_APPWIDGET_SIZES,
          new ArrayList<>(Arrays.asList(new SizeF(277.57007f, 137.90343f), new SizeF(600, 180))));
      assertEquals(new SizeF(277.57007f, 137.90343f), PrayerWidgets.presentationSize(b, false, d));
      assertEquals(new SizeF(600, 180), PrayerWidgets.presentationSize(b, true, d));
    }
  }

  @Test
  public void previewAndBoundHostDrawIdenticallyAtActualPhoneAndWideSizes() throws Exception {
    AppWidgetHost host = new AppWidgetHost(c, 33201);
    int id = host.allocateAppWidgetId();
    host.startListening();
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    WidgetProviderDescriptor d = WidgetProviderDescriptor.DAILY;
    try {
      assertTrue(manager.bindAppWidgetIdIfAllowed(id, new ComponentName(c, d.component)));
      try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
        for (SizeF size : Arrays.asList(new SizeF(277.57007f, 137.90343f), new SizeF(600, 180))) {
          manager.updateAppWidgetOptions(id, options(size.getWidth(), size.getHeight()));
          s.onActivity(
              a -> {
                try {
                  RemoteViews remote = fixture(a, size.getWidth(), size.getHeight());
                  AppWidgetHostView real = host.createView(a, id, manager.getAppWidgetInfo(id));
                  real.updateAppWidget(remote);
                  Rect pad =
                      AppWidgetHostView.getDefaultPaddingForWidget(
                          a, new ComponentName(c, d.component), null);
                  layout(
                      real,
                      Ui.dp(a, size.getWidth()) + pad.left + pad.right,
                      Ui.dp(a, size.getHeight()) + pad.top + pad.bottom);
                  View nativeRoot = real.getChildAt(0);
                  assertNotNull(nativeRoot.findViewById(R.id.widget_arc_curve));
                  WidgetPreviewView preview = new WidgetPreviewView(a);
                  preview.show(remote, size);
                  int panel = Ui.dp(a, 180);
                  preview.measure(
                      View.MeasureSpec.makeMeasureSpec(panel, View.MeasureSpec.EXACTLY),
                      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                  preview.layout(0, 0, preview.getMeasuredWidth(), preview.getMeasuredHeight());
                  View shown = preview.getChildAt(0);
                  assertEquals(Ui.dp(a, size.getWidth()), shown.getMeasuredWidth());
                  assertEquals(nativeRoot.getWidth(), shown.getWidth());
                  assertEquals(nativeRoot.getHeight(), shown.getHeight());
                  Bitmap actual = bitmap(nativeRoot), sample = bitmap(shown);
                  assertTrue(
                      "Native host and the unscaled preview must paint exactly the same layout",
                      actual.sameAs(sample));
                  save(actual, size.getWidth() < 300 ? "phone-actual-host" : "wide-actual-host");
                  Bitmap scaled = bitmap(preview);
                  save(
                      scaled,
                      size.getWidth() < 300 ? "phone-preview-scaled" : "wide-preview-scaled");
                  actual.recycle();
                  sample.recycle();
                  scaled.recycle();
                } catch (Exception e) {
                  throw new AssertionError(e);
                }
              });
        }
      }
    } finally {
      host.deleteAppWidgetId(id);
      host.stopListening();
      c.getSharedPreferences("widget_config", 0).edit().remove("" + id).commit();
    }
  }

  private <T extends View> T find(View root, Class<T> type) {
    if (type.isInstance(root)) return type.cast(root);
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        T result = find(((ViewGroup) root).getChildAt(i), type);
        if (result != null) return result;
      }
    return null;
  }

  @Test
  public void myWidgetsAndConfigurationUseThePlacedInstanceBounds() throws Exception {
    AppWidgetHost host = new AppWidgetHost(c, 33202);
    int id = host.allocateAppWidgetId();
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    WidgetProviderDescriptor d = WidgetProviderDescriptor.DAILY;
    host.startListening();
    try {
      assertTrue(manager.bindAppWidgetIdIfAllowed(id, new ComponentName(c, d.component)));
      manager.updateAppWidgetOptions(id, options(600, 180));
      try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
        s.onActivity(a -> a.openDestination(R.id.widgetsFragment, null));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        s.onActivity(
            a -> {
              TabLayout tabs = find(a.getWindow().getDecorView(), TabLayout.class);
              assertNotNull(tabs);
              tabs.selectTab(tabs.getTabAt(1));
            });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(150);
        s.onActivity(
            a -> {
              WidgetPreviewView preview =
                  a.getWindow().getDecorView().findViewWithTag("widget-preview-" + id);
              assertNotNull(preview);
              assertEquals(new SizeF(600, 180), preview.hostSize());
              assertEquals(Ui.dp(a, 600), preview.getChildAt(0).getMeasuredWidth());
            });
      }
      try (ActivityScenario<WidgetConfigActivity> s =
          ActivityScenario.launch(
              new Intent(c, WidgetConfigActivity.class)
                  .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(150);
        s.onActivity(
            a -> {
              WidgetPreviewView preview = a.findViewById(R.id.widget_config_preview);
              assertEquals(new SizeF(600, 180), preview.hostSize());
              assertEquals(Ui.dp(a, 600), preview.getChildAt(0).getMeasuredWidth());
              a.findViewById(R.id.widget_cancel).performClick();
            });
      }
    } finally {
      host.deleteAppWidgetId(id);
      host.stopListening();
      c.getSharedPreferences("widget_config", 0).edit().remove("" + id).commit();
    }
  }
}
