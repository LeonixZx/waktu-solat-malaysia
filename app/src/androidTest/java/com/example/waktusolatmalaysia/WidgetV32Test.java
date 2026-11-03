package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.appwidget.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.time.*;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WidgetV32Test {
  private Context c;
  private Map<String, ?> app, personal, configs;

  @Before
  public void before() {
    c = InstrumentationRegistry.getInstrumentation().getTargetContext();
    app = new HashMap<>(c.getSharedPreferences("waktu_solat", 0).getAll());
    personal = new HashMap<>(c.getSharedPreferences("personal_library", 0).getAll());
    configs = new HashMap<>(c.getSharedPreferences("widget_config", 0).getAll());
  }

  @After
  public void after() {
    restore("waktu_solat", app);
    restore("personal_library", personal);
    restore("widget_config", configs);
    PrayerWidgets.refreshAsync(c);
  }

  private void restore(String name, Map<String, ?> values) {
    SharedPreferences.Editor e = c.getSharedPreferences(name, 0).edit().clear();
    for (Map.Entry<String, ?> v : values.entrySet()) {
      Object x = v.getValue();
      String k = v.getKey();
      if (x instanceof String) e.putString(k, (String) x);
      else if (x instanceof Boolean) e.putBoolean(k, (Boolean) x);
      else if (x instanceof Integer) e.putInt(k, (Integer) x);
      else if (x instanceof Long) e.putLong(k, (Long) x);
      else if (x instanceof Float) e.putFloat(k, (Float) x);
      else if (x instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) x));
    }
    e.commit();
  }

  private PrayerDay day(LocalDate date) {
    return new PrayerDay(
        date,
        "1448-05-22",
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

  private WidgetRenderModel fixture(WidgetProviderDescriptor d) {
    LocalDate date = LocalDate.of(2026, 11, 2);
    return new WidgetRenderModel(
        new PrayerZone("Wilayah Persekutuan", "Kuala Lumpur, Putrajaya", "WLY01"),
        day(date),
        day(date.plusDays(1)),
        date.atTime(14, 0).atZone(PrayerClock.MALAYSIA),
        d,
        Collections.singletonMap("fajr", true));
  }

  private JSONObject cfg(String theme, boolean track) throws Exception {
    return new JSONObject()
        .put("schemaVersion", 2)
        .put("theme", theme)
        .put("trackingEnabled", track)
        .put("followApp", false)
        .put("zone", "WLY01")
        .put("name", "Kuala Lumpur, Putrajaya");
  }

  private static void visibleText(View v) {
    if (v.getVisibility() != View.VISIBLE) return;
    if (v instanceof TextView) {
      TextView t = (TextView) v;
      if (t.getText().length() > 0) {
        assertTrue(
            "Text height: " + t.getText(),
            t.getLayout() != null
                && t.getLayout().getHeight()
                    <= t.getHeight()
                        - t.getCompoundPaddingTop()
                        - t.getCompoundPaddingBottom()
                        + 2);
        if (t.getMaxLines() == 1)
          assertTrue(
              "Text width: " + t.getText(),
              t.getPaint().measureText(t.getText().toString())
                  <= t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight() + 1);
      }
    }
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        View child = ((ViewGroup) v).getChildAt(i);
        if (child.getVisibility() != View.GONE)
          assertTrue(
              "Child stays inside: " + child.getId(), child.getBottom() <= v.getHeight() + 1);
        visibleText(child);
      }
  }

  private static String tree(View v) {
    String s =
        v.getClass().getSimpleName()
            + "#"
            + v.getId()
            + ((v instanceof TextView) ? ":" + ((TextView) v).getText() : "");
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        s += " [" + tree(((ViewGroup) v).getChildAt(i)) + "]";
    return s;
  }

  @Test
  public void nativeMeasuredLayoutsKeepPurposeAtAllTextScales() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      for (float scale : new float[] {1, 1.3f, 2})
        for (String lang : new String[] {"ms", "en"})
          for (boolean twelve : new boolean[] {false, true})
            for (String theme : new String[] {"light", "dark"}) {
              c.getSharedPreferences("waktu_solat", 0)
                  .edit()
                  .putString("language", lang)
                  .putBoolean("twelve_hour", twelve)
                  .commit();
              for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL)
                for (int[] bounds :
                    new int[][] {
                      {120, 72}, {180, 180}, {280, 180}, {360, 180}, {320, 420}, {480, 700}
                    }) {
                  WidgetConfigV2 config = new WidgetConfigV2(cfg(theme, true), d);
                  scenario.onActivity(
                      a -> {
                        Configuration cf = new Configuration(a.getResources().getConfiguration());
                        cf.fontScale = scale;
                        Context scaled = a.createConfigurationContext(cf);
                        RemoteViews remote =
                            PrayerWidgets.render(
                                scaled, 0, d, config, fixture(d), bounds[0], bounds[1], true);
                        FrameLayout p = new FrameLayout(scaled);
                        View v = remote.apply(scaled, p);
                        int w = Ui.dp(scaled, bounds[0]), h = Ui.dp(scaled, bounds[1]);
                        v.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                        v.layout(0, 0, w, h);
                        visibleText(v);
                        if (bounds[0] == 480
                            || (scale == 1
                                && !twelve
                                && bounds[0] == 360
                                && bounds[1] == 180
                                && d.family != WidgetProviderDescriptor.Family.EXPANDED)) {
                          assertNull(
                              "Sufficient space retains widget purpose",
                              v.findViewById(R.id.widget_message));
                          if (d.family != WidgetProviderDescriptor.Family.NEXT)
                            for (int i : PrayerDay.OBLIGATORY)
                              assertNotNull(
                                  v.findViewById(
                                      scaled
                                          .getResources()
                                          .getIdentifier(
                                              "widget_value_" + i, "id", scaled.getPackageName())));
                        }
                      });
                }
            }
    }
  }

  @Test
  public void modelHandlesMalaysiaMidnightMissingTomorrowAndYearRollover() {
    LocalDate date = LocalDate.of(2026, 12, 31);
    PrayerZone z = new AppPreferences(c).zone();
    ZonedDateTime late = date.atTime(23, 59).atZone(PrayerClock.MALAYSIA);
    WidgetRenderModel missing =
        new WidgetRenderModel(
            z, day(date), null, late, WidgetProviderDescriptor.NEXT, Collections.emptyMap());
    assertNull(missing.next);
    assertEquals(WidgetRenderModel.Availability.MISSING_TOMORROW, missing.availability);
    WidgetRenderModel next =
        new WidgetRenderModel(
            z,
            day(date),
            day(date.plusDays(1)),
            late.withZoneSameInstant(ZoneId.of("America/New_York")),
            WidgetProviderDescriptor.NEXT,
            Collections.emptyMap());
    assertEquals(LocalDate.of(2027, 1, 1), next.displayDate);
    assertEquals(1, next.next.index);
    assertTrue(next.next.tomorrow);
    WidgetRenderModel absent =
        new WidgetRenderModel(
            z,
            null,
            day(date.plusDays(1)),
            late,
            WidgetProviderDescriptor.DAILY,
            Collections.emptyMap());
    assertEquals(WidgetRenderModel.Availability.MISSING_TODAY, absent.availability);
  }

  @Test
  public void legacyMigrationRetainsZoneAppearanceAndUnknownFields() throws Exception {
    JSONObject original =
        new JSONObject()
            .put("theme", "dark")
            .put("zone", "SGR03")
            .put("followApp", false)
            .put("name", "Legacy name")
            .put("futureField", 7)
            .put("trackingEnabled", true);
    WidgetConfigV2 parsed = new WidgetConfigV2(original, WidgetProviderDescriptor.EXPANDED);
    assertFalse(parsed.trackingEnabled);
    assertTrue(parsed.showHijri);
    assertTrue(parsed.showAuxiliary);
    assertEquals("dark", parsed.theme);
    assertEquals("SGR03", parsed.resolvedZone(c).code);
    assertEquals(7, parsed.json().getInt("futureField"));
    assertFalse(original.has("schemaVersion"));
    assertEquals(2, parsed.json().getInt("schemaVersion"));
    assertFalse(
        new WidgetConfigV2(cfg("light", true), WidgetProviderDescriptor.DAILY).trackingEnabled);
  }

  @Test
  public void trackingUsesLegacyKeysAndIntendedStateIsIdempotent() {
    LocalDate d = LocalDate.now(PrayerClock.MALAYSIA);
    String k = PrayerTrackingStore.key(d, "fajr");
    c.getSharedPreferences("personal_library", 0).edit().remove(k).commit();
    PrayerTrackingStore store = new PrayerTrackingStore(c);
    assertTrue(store.set(d, "fajr", true));
    assertFalse(store.set(d, "fajr", true));
    assertTrue(store.read(d).get("fajr"));
    assertEquals("prayed_" + d + "_Subuh", k);
    assertTrue(c.getSharedPreferences("personal_library", 0).getBoolean(k, false));
    assertTrue(store.set(d, "fajr", false));
    assertFalse(store.set(d, "fajr", false));
    assertFalse(store.read(d).get("fajr"));
    try {
      store.set(d, "Fajr", true);
      fail("Labels must not become persistence IDs");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void realHostFourComponentsBothAppearancesAndSafeActions() throws Exception {
    assumeTrue(InstrumentationRegistry.getArguments().getString("widgets", "").equals("true"));
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    AppWidgetHost host = new AppWidgetHost(c, 32008);
    List<Integer> ids = new ArrayList<>();
    String defaultZone = new AppPreferences(c).zone().code;
    host.startListening();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL) {
        int id = host.allocateAppWidgetId();
        ids.add(id);
        assertTrue(manager.bindAppWidgetIdIfAllowed(id, new ComponentName(c, d.component)));
        assertSame(d, WidgetProviderDescriptor.instance(c, id));
        for (String theme : new String[] {"light", "dark"}) {
          JSONObject config = cfg(theme, d == WidgetProviderDescriptor.EXPANDED);
          c.getSharedPreferences("widget_config", 0)
              .edit()
              .putString("" + id, config.toString())
              .commit();
          int width = d.width, height = d.height;
          Bundle opts = new Bundle();
          opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width);
          opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width);
          opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height);
          opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height);
          if (Build.VERSION.SDK_INT >= 31)
            opts.putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                new ArrayList<>(Collections.singletonList(new android.util.SizeF(width, height))));
          manager.updateAppWidgetOptions(id, opts);
          PrayerWidgets.update(c, id, d);
          final FrameLayout[] stage = {null};
          scenario.onActivity(
              a -> {
                AppWidgetHostView widget = host.createView(a, id, manager.getAppWidgetInfo(id));
                FrameLayout frame = new FrameLayout(a);
                frame.setBackgroundColor(0xffe5e1d9);
                android.graphics.Rect pad =
                    AppWidgetHostView.getDefaultPaddingForWidget(
                        a, new ComponentName(c, d.component), null);
                frame.addView(
                    widget,
                    new FrameLayout.LayoutParams(
                        Ui.dp(a, width) + pad.left + pad.right,
                        Ui.dp(a, height) + pad.top + pad.bottom,
                        Gravity.CENTER));
                a.addContentView(frame, new ViewGroup.LayoutParams(-1, -1));
                stage[0] = frame;
              });
          SystemClock.sleep(1000);
          TabletFeatureTest.capture("v32-host-diagnostic");
          scenario.onActivity(
              a -> {
                View root = ((AppWidgetHostView) stage[0].getChildAt(0)).getChildAt(0);
                assertEquals(
                    d.component.getSimpleName() + " " + tree(root), R.id.widget_root, root.getId());
                visibleText(root);
              });
          TabletFeatureTest.capture("v32-" + d.component.getSimpleName() + "-" + theme);
          scenario.onActivity(a -> ((ViewGroup) stage[0].getParent()).removeView(stage[0]));
        }
        if (d == WidgetProviderDescriptor.EXPANDED) {
          LocalDate date = LocalDate.now(PrayerClock.MALAYSIA);
          String key = PrayerTrackingStore.key(date, "fajr");
          c.getSharedPreferences("personal_library", 0).edit().remove(key).commit();
          Intent action =
              new Intent()
                  .putExtra("widgetId", id)
                  .putExtra("date", date.toString())
                  .putExtra("prayer", "fajr")
                  .putExtra("completed", true);
          assertTrue(WidgetTrackingReceiver.apply(c, action, date));
          assertFalse(WidgetTrackingReceiver.apply(c, action, date));
          assertFalse(WidgetTrackingReceiver.apply(c, action, date.plusDays(1)));
          assertTrue(new PrayerTrackingStore(c).read(date).get("fajr"));
          action.putExtra("completed", false);
          assertTrue(WidgetTrackingReceiver.apply(c, action, date));
          JSONObject cfg = cfg("light", false);
          c.getSharedPreferences("widget_config", 0)
              .edit()
              .putString("" + id, cfg.toString())
              .commit();
          action.putExtra("completed", true);
          assertFalse(WidgetTrackingReceiver.apply(c, action, date));
          assertFalse(new PrayerTrackingStore(c).read(date).get("fajr"));
        }
      }
      assertEquals(defaultZone, new AppPreferences(c).zone().code);
    } finally {
      for (int id : ids) c.getSharedPreferences("widget_config", 0).edit().remove("" + id).commit();
      host.stopListening();
      host.deleteHost();
    }
  }

  @Test
  public void configurationDraftSurvivesRecreationAndCancelDoesNotSave() throws Exception {
    assumeTrue(InstrumentationRegistry.getArguments().getString("widgets", "").equals("true"));
    AppWidgetHost host = new AppWidgetHost(c, 32009);
    int id = host.allocateAppWidgetId();
    try {
      assertTrue(
          AppWidgetManager.getInstance(c)
              .bindAppWidgetIdIfAllowed(id, new ComponentName(c, ExpandedPrayerWidget.class)));
      String original = cfg("light", false).toString();
      c.getSharedPreferences("widget_config", 0).edit().putString("" + id, original).commit();
      Intent intent =
          new Intent(c, WidgetConfigActivity.class)
              .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try (ActivityScenario<WidgetConfigActivity> s = ActivityScenario.launch(intent)) {
        s.onActivity(a -> ((CompoundButton) a.findViewById(R.id.widget_tracking)).setChecked(true));
        s.recreate();
        s.onActivity(
            a -> {
              assertTrue(((CompoundButton) a.findViewById(R.id.widget_tracking)).isChecked());
              a.findViewById(R.id.widget_cancel).performClick();
            });
      }
      assertEquals(original, c.getSharedPreferences("widget_config", 0).getString("" + id, ""));
    } finally {
      host.deleteHost();
    }
  }

  @Test
  public void configurationSaveAndImmutableActionsStayInstanceSpecific() throws Exception {
    assumeTrue(InstrumentationRegistry.getArguments().getString("widgets", "").equals("true"));
    AppWidgetHost host = new AppWidgetHost(c, 32011);
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    int id = host.allocateAppWidgetId(), other = host.allocateAppWidgetId();
    String zone = new AppPreferences(c).zone().code;
    try {
      assertTrue(
          manager.bindAppWidgetIdIfAllowed(id, new ComponentName(c, ExpandedPrayerWidget.class)));
      assertTrue(
          manager.bindAppWidgetIdIfAllowed(other, new ComponentName(c, DayPrayerWidget.class)));
      String neighbor = cfg("dark", false).toString();
      c.getSharedPreferences("widget_config", 0)
          .edit()
          .putString("" + id, cfg("light", false).put("zone", "SGR03").toString())
          .putString("" + other, neighbor)
          .commit();
      try (ActivityScenario<WidgetConfigActivity> scenario =
          ActivityScenario.launch(
              new Intent(c, WidgetConfigActivity.class)
                  .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
        scenario.onActivity(
            a -> {
              ((CompoundButton) a.findViewById(R.id.widget_tracking)).setChecked(true);
              a.findViewById(R.id.widget_save).performClick();
            });
      }
      assertTrue(WidgetConfigV2.read(c, id, WidgetProviderDescriptor.EXPANDED).trackingEnabled);
      assertEquals("SGR03", PrayerWidgets.zone(c, id).code);
      assertEquals(neighbor, c.getSharedPreferences("widget_config", 0).getString("" + other, ""));
      assertEquals(zone, new AppPreferences(c).zone().code);
      LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
      new PrayerTrackingStore(c).set(today, "isha", false);
      android.app.PendingIntent mark = WidgetTrackingReceiver.pending(c, id, today, "isha", true);
      if (Build.VERSION.SDK_INT >= 31) assertTrue(mark.isImmutable());
      mark.send();
      mark.send();
      long deadline = SystemClock.elapsedRealtime() + 5000;
      while (!new PrayerTrackingStore(c).read(today).get("isha")
          && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50);
      assertTrue(new PrayerTrackingStore(c).read(today).get("isha"));
      WidgetTrackingReceiver.pending(c, id, today, "isha", false).send();
      deadline = SystemClock.elapsedRealtime() + 5000;
      while (new PrayerTrackingStore(c).read(today).get("isha")
          && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50);
      assertFalse(new PrayerTrackingStore(c).read(today).get("isha"));
    } finally {
      host.deleteHost();
    }
  }

  @Test
  public void mainSurfaceOpensDisplayedDateAndZoneWithoutChangingDefault() throws Exception {
    String original = new AppPreferences(c).zone().code;
    LocalDate date = LocalDate.of(2026, 12, 31);
    WidgetProviderDescriptor d = WidgetProviderDescriptor.NEXT;
    WidgetRenderModel model =
        new WidgetRenderModel(
            new PrayerZone("Selangor", "Independent widget", "SGR03"),
            day(date),
            day(date.plusDays(1)),
            date.atTime(23, 59).atZone(PrayerClock.MALAYSIA),
            d,
            Collections.emptyMap());
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          a -> {
            RemoteViews remote =
                PrayerWidgets.render(
                    a, 97531, d, new WidgetConfigV2(new JSONObject(), d), model, 360, 180, false);
            FrameLayout parent = new FrameLayout(a);
            View root = remote.apply(a, parent);
            assertTrue(root.performClick());
          });
      final boolean[] opened = {false};
      long deadline = SystemClock.elapsedRealtime() + 5000;
      while (!opened[0] && SystemClock.elapsedRealtime() < deadline) {
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync(
                () -> {
                  for (android.app.Activity a :
                      androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                          .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
                    if (a instanceof MainActivity) {
                      androidx.navigation.fragment.NavHostFragment nav =
                          (androidx.navigation.fragment.NavHostFragment)
                              ((MainActivity) a)
                                  .getSupportFragmentManager()
                                  .findFragmentById(R.id.nav_host);
                      if (nav != null) {
                        androidx.fragment.app.Fragment f =
                            nav.getChildFragmentManager().getPrimaryNavigationFragment();
                        Bundle args = f == null ? null : f.getArguments();
                        if (args != null
                            && "SGR03".equals(args.getString("zone"))
                            && "2027-01-01".equals(args.getString("date"))) opened[0] = true;
                      }
                    }
                });
        if (!opened[0]) SystemClock.sleep(50);
      }
      assertTrue("Surface opened its own schedule date/zone", opened[0]);
      assertEquals(original, new AppPreferences(c).zone().code);
    }
  }

  @Test
  public void deletedInstanceRejectsQueuedRenderingAndConfigurationWrites() throws Exception {
    assumeTrue(InstrumentationRegistry.getArguments().getString("widgets", "").equals("true"));
    AppWidgetHost host = new AppWidgetHost(c, 32012);
    int id = host.allocateAppWidgetId();
    try {
      assertTrue(
          AppWidgetManager.getInstance(c)
              .bindAppWidgetIdIfAllowed(id, new ComponentName(c, ExpandedPrayerWidget.class)));
      WidgetConfigV2 draft =
          new WidgetConfigV2(cfg("dark", true), WidgetProviderDescriptor.EXPANDED);
      draft.save(c, id);
      host.deleteAppWidgetId(id);
      new ExpandedPrayerWidget().onDeleted(c, new int[] {id});
      PrayerWidgets.update(c, id, WidgetProviderDescriptor.EXPANDED);
      draft.save(c, id);
      assertNull(WidgetProviderDescriptor.instance(c, id));
      assertFalse(c.getSharedPreferences("widget_config", 0).contains("" + id));
    } finally {
      host.deleteHost();
    }
  }
}
