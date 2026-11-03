package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.*;
import org.junit.*;

/** Native RemoteViews fixtures are labelled and never stored in the production prayer cache. */
public class WidgetTimelineV331Test {
  private Context c;
  private Map<String, ?> preferences, personal;

  @Before
  public void snapshot() {
    c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    preferences = new HashMap<>(c.getSharedPreferences("waktu_solat", 0).getAll());
    personal = new HashMap<>(c.getSharedPreferences("personal_library", 0).getAll());
  }

  @After
  public void restore() {
    restore("waktu_solat", preferences);
    restore("personal_library", personal);
  }

  private void restore(String name, Map<String, ?> values) {
    SharedPreferences.Editor e = c.getSharedPreferences(name, 0).edit().clear();
    for (var x : values.entrySet()) {
      Object v = x.getValue();
      String k = x.getKey();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
    }
    e.commit();
  }

  private PrayerDay day(LocalDate date) {
    return new PrayerDay(
        date,
        "1448-05-23",
        Arrays.asList(
            LocalTime.of(5, 35),
            LocalTime.of(5, 45),
            LocalTime.of(7, 0),
            LocalTime.of(7, 25),
            LocalTime.of(13, 5),
            LocalTime.of(16, 25),
            LocalTime.of(19, 2),
            LocalTime.of(20, 15)));
  }

  private WidgetRenderModel model(
      WidgetProviderDescriptor d, LocalDate date, int hour, boolean tomorrow) {
    return new WidgetRenderModel(
        new PrayerZone("Selangor", "Gombak, Petaling", "SGR01"),
        day(date),
        tomorrow ? day(date.plusDays(1)) : null,
        date.atTime(hour, 0).atZone(PrayerClock.MALAYSIA),
        d,
        Collections.emptyMap());
  }

  private JSONObject json(String theme) throws Exception {
    return new JSONObject()
        .put("schemaVersion", 2)
        .put("layoutStyle", "timeline")
        .put("theme", theme)
        .put("followApp", false)
        .put("zone", "SGR01")
        .put("name", "Gombak, Petaling")
        .put("state", "Selangor")
        .put("trackingEnabled", true)
        .put("showHijri", true)
        .put("showAuxiliary", true);
  }

  private View apply(Context a, RemoteViews remote, int w, int h) {
    View root = remote.apply(a, new FrameLayout(a));
    root.measure(
        View.MeasureSpec.makeMeasureSpec(Ui.dp(a, w), View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(Ui.dp(a, h), View.MeasureSpec.EXACTLY));
    root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
    return root;
  }

  private void fits(View v) {
    if (v.getVisibility() != View.VISIBLE) return;
    if (v instanceof TextView) {
      TextView t = (TextView) v;
      if (t.length() > 0) {
        assertNotNull(t.getLayout());
        assertTrue(
            t.getText() + " height",
            t.getLayout().getHeight()
                <= t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom() + 2);
        for (int i = 0; i < t.getLineCount(); i++)
          assertEquals(t.getText() + " ellipsis", 0, t.getLayout().getEllipsisCount(i));
        if (t.getMaxLines() == 1)
          assertTrue(
              t.getText() + " width",
              t.getPaint().measureText(t.getText().toString())
                  <= t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight() + 1);
      }
    }
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) fits(((ViewGroup) v).getChildAt(i));
  }

  private void export(View root, String name) throws Exception {
    View surface = root.findViewById(R.id.widget_surface);
    assertNotNull(surface);
    Bitmap b =
        Bitmap.createBitmap(surface.getWidth(), surface.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(b);
    canvas.translate(-surface.getLeft(), -surface.getTop());
    root.draw(canvas);
    File dir = new File(c.getExternalFilesDir(null), "widget-v331");
    dir.mkdirs();
    try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    b.recycle();
  }

  @Test
  public void existingStyleIsRetainedAndNewWidgetsDefaultToTimeline() throws Exception {
    WidgetProviderDescriptor d = WidgetProviderDescriptor.EXPANDED;
    JSONObject legacy =
        new JSONObject().put("theme", "dark").put("zone", "SGR01").put("unknownFlag", 42);
    WidgetConfigV2 old = new WidgetConfigV2(legacy, d);
    assertEquals("classic", old.layoutStyle);
    assertEquals(42, old.json().getInt("unknownFlag"));
    assertEquals("dark", old.theme);
    assertEquals("timeline", new WidgetConfigV2(new JSONObject(), d).layoutStyle);
    WidgetConfigV2 modern = new WidgetConfigV2(json("light"), d);
    assertEquals("timeline", new WidgetConfigV2(modern.json(), d).layoutStyle);
    assertEquals("SGR01", modern.zone);
  }

  @Test
  public void allFamiliesRenderNativeTimelineWithSixTimeStripAndBothAppearances() throws Exception {
    try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
      for (String language : new String[] {"ms", "en"})
        for (boolean twelve : new boolean[] {false, true})
          for (String theme : new String[] {"light", "dark"})
            for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL) {
              c.getSharedPreferences("waktu_solat", 0)
                  .edit()
                  .putString("language", language)
                  .putBoolean("twelve_hour", twelve)
                  .commit();
              int w = d.width, h = d.height;
              WidgetConfigV2 config = new WidgetConfigV2(json(theme), d);
              s.onActivity(
                  a -> {
                    View root =
                        apply(
                            a,
                            PrayerWidgets.render(
                                a,
                                0,
                                d,
                                config,
                                model(d, LocalDate.of(2026, 11, 3), 14, true),
                                w,
                                h,
                                true),
                            w,
                            h);
                    assertNotNull(
                        "Timeline must fit " + d.en + " " + language + " " + twelve,
                        root.findViewById(R.id.widget_timeline));
                    fits(root);
                    for (int i :
                        d.preferStrip ? new int[] {1, 2, 4, 5, 6, 7} : PrayerDay.OBLIGATORY) {
                      TextView value =
                          root.findViewById(
                              a.getResources()
                                  .getIdentifier("widget_value_" + i, "id", a.getPackageName()));
                      assertNotNull(value);
                      assertTrue(value.length() > 0);
                    }
                    if (d == WidgetProviderDescriptor.EXPANDED)
                      for (int i : PrayerDay.OBLIGATORY) {
                        View check =
                            root.findViewById(
                                a.getResources()
                                    .getIdentifier("widget_check_" + i, "id", a.getPackageName()));
                        assertNotNull(check);
                        assertTrue(check.getWidth() >= Ui.dp(a, 48));
                        assertFalse(check.isEnabled());
                      }
                    if (language.equals("ms") && !twelve)
                      try {
                        export(root, d.component.getSimpleName() + "-" + theme);
                        WidgetConfigV2 picker =
                            new WidgetConfigV2(json(theme).put("trackingEnabled", false), d);
                        View preview =
                            apply(
                                a,
                                PrayerWidgets.render(
                                    a,
                                    0,
                                    d,
                                    picker,
                                    model(d, LocalDate.of(2026, 11, 3), 14, true),
                                    w,
                                    h,
                                    true),
                                w,
                                h);
                        export(preview, "picker-" + d.component.getSimpleName() + "-" + theme);
                        if (d == WidgetProviderDescriptor.NEXT) {
                          View enlarged =
                              apply(
                                  a,
                                  PrayerWidgets.render(
                                      a,
                                      0,
                                      d,
                                      config,
                                      model(d, LocalDate.of(2026, 11, 3), 14, true),
                                      280,
                                      300,
                                      true),
                                  280,
                                  300);
                          export(enlarged, "large-" + d.component.getSimpleName() + "-" + theme);
                        }
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                    if (language.equals("en") && twelve && theme.equals("dark"))
                      try {
                        export(root, "12hour-" + d.component.getSimpleName());
                      } catch (Exception e) {
                        throw new AssertionError(e);
                      }
                  });
            }
    }
  }

  @Test
  public void compactBoundsTextScalingAndMissingTomorrowKeepValidatedData() throws Exception {
    try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
      for (float scale : new float[] {1, 1.3f, 2})
        for (boolean twelve : new boolean[] {false, true})
          for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL) {
            c.getSharedPreferences("waktu_solat", 0)
                .edit()
                .putBoolean("twelve_hour", twelve)
                .commit();
            WidgetConfigV2 cfg = new WidgetConfigV2(json("light"), d);
            int[][] sizes =
                d == WidgetProviderDescriptor.NEXT
                    ? new int[][] {{150, 130}, {180, 180}}
                    : d.preferStrip
                        ? new int[][] {{250, 70}, {320, 120}}
                        : new int[][] {{280, 180}, {480, 700}};
            for (int[] size : sizes)
              s.onActivity(
                  a -> {
                    Configuration change = new Configuration(a.getResources().getConfiguration());
                    change.fontScale = scale;
                    Context scaled = a.createConfigurationContext(change);
                    View root =
                        apply(
                            scaled,
                            PrayerWidgets.render(
                                scaled,
                                0,
                                d,
                                cfg,
                                model(d, LocalDate.of(2026, 11, 3), 14, true),
                                size[0],
                                size[1],
                                true),
                            size[0],
                            size[1]);
                    fits(root);
                    if (scale == 1)
                      assertNull(
                          "Normal placement keeps its information",
                          root.findViewById(R.id.widget_message));
                  });
          }
      WidgetConfigV2 cfg = new WidgetConfigV2(json("light"), WidgetProviderDescriptor.NEXT);
      s.onActivity(
          a -> {
            View missing =
                apply(
                    a,
                    PrayerWidgets.render(
                        a,
                        0,
                        WidgetProviderDescriptor.NEXT,
                        cfg,
                        model(WidgetProviderDescriptor.NEXT, LocalDate.of(2026, 11, 3), 23, false),
                        280,
                        300,
                        true),
                    280,
                    300);
            assertNotNull(missing.findViewById(R.id.widget_message));
            assertNull(missing.findViewById(R.id.widget_time));
            View next =
                apply(
                    a,
                    PrayerWidgets.render(
                        a,
                        0,
                        WidgetProviderDescriptor.NEXT,
                        cfg,
                        model(WidgetProviderDescriptor.NEXT, LocalDate.of(2026, 12, 31), 23, true),
                        280,
                        300,
                        true),
                    280,
                    300);
            assertTrue(
                ((TextView) next.findViewById(R.id.widget_date))
                    .getText()
                    .toString()
                    .contains("2027"));
          });
    }
  }

  @Test
  public void realTrackingTapsAndStyleDraftStayInstanceSafe() throws Exception {
    AppWidgetHost host = new AppWidgetHost(c, 33108);
    host.startListening();
    int id = host.allocateAppWidgetId();
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    WidgetProviderDescriptor d = WidgetProviderDescriptor.EXPANDED;
    String defaultZone = new AppPreferences(c).zone().code;
    try {
      assertTrue(manager.bindAppWidgetIdIfAllowed(id, new ComponentName(c, d.component)));
      SystemClock.sleep(1000);
      JSONObject saved = json("light");
      c.getSharedPreferences("widget_config", 0)
          .edit()
          .putString("" + id, saved.toString())
          .commit();
      LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
      PrayerTrackingStore records = new PrayerTrackingStore(c);
      records.set(today, "fajr", false);
      for (boolean done : new boolean[] {true, false}) {
        WidgetConfigV2 cfg = new WidgetConfigV2(saved, d);
        WidgetRenderModel m =
            new WidgetRenderModel(
                new PrayerZone("Selangor", "Gombak", "SGR01"),
                day(today),
                day(today.plusDays(1)),
                today.atTime(14, 0).atZone(PrayerClock.MALAYSIA),
                d,
                records.read(today));
        try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
          s.onActivity(
              a -> {
                AppWidgetHostView widget = host.createView(a, id, manager.getAppWidgetInfo(id));
                widget.updateAppWidget(PrayerWidgets.render(a, id, d, cfg, m, 360, 420, false));
                Rect padding =
                    AppWidgetHostView.getDefaultPaddingForWidget(
                        a, new ComponentName(c, d.component), null);
                int width = Ui.dp(a, 360) + padding.left + padding.right,
                    height = Ui.dp(a, 420) + padding.top + padding.bottom;
                a.addContentView(widget, new ViewGroup.LayoutParams(width, height));
                widget.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                widget.layout(0, 0, width, height);
                View root = widget.getChildAt(0);
                assertNotNull(root.findViewById(R.id.widget_timeline));
                assertTrue(
                    "Bound tracking configuration must be enabled",
                    WidgetConfigV2.read(c, id, d).trackingEnabled);
                assertTrue(
                    "Recorded state is the action's source",
                    records.read(today).get("fajr") != done);
                assertTrue(root.findViewById(R.id.widget_check_1).isEnabled());
                assertTrue(root.findViewById(R.id.widget_check_1).performClick());
              });
          long end = SystemClock.uptimeMillis() + 4000;
          while (records.read(today).get("fajr") != done && SystemClock.uptimeMillis() < end)
            SystemClock.sleep(50);
          assertEquals(
              "Tap record; widget config=" + WidgetConfigV2.read(c, id, d).json(),
              done,
              records.read(today).get("fajr"));
        }
      }
      Intent intent =
          new Intent(c, WidgetConfigActivity.class)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
      try (ActivityScenario<WidgetConfigActivity> s = ActivityScenario.launch(intent)) {
        s.onActivity(
            a -> {
              ChoiceBar choices = a.findViewById(R.id.widget_layout_choices);
              assertEquals(0, choices.selected());
              choices.getChildAt(1).performClick();
            });
        s.recreate();
        s.onActivity(
            a -> {
              assertEquals(1, ((ChoiceBar) a.findViewById(R.id.widget_layout_choices)).selected());
              a.findViewById(R.id.widget_cancel).performClick();
            });
      }
      assertEquals(
          saved.toString(), c.getSharedPreferences("widget_config", 0).getString("" + id, ""));
      try (ActivityScenario<WidgetConfigActivity> s = ActivityScenario.launch(intent)) {
        s.onActivity(
            a -> {
              ((ChoiceBar) a.findViewById(R.id.widget_layout_choices)).getChildAt(1).performClick();
              a.findViewById(R.id.widget_save).performClick();
            });
      }
      assertEquals("classic", WidgetConfigV2.read(c, id, d).layoutStyle);
      assertEquals("SGR01", WidgetConfigV2.read(c, id, d).zone);
      assertTrue(WidgetConfigV2.read(c, id, d).trackingEnabled);
      assertEquals(defaultZone, new AppPreferences(c).zone().code);
    } finally {
      host.deleteAppWidgetId(id);
      host.stopListening();
      c.getSharedPreferences("widget_config", 0).edit().remove("" + id).commit();
    }
  }
}
