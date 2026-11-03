package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import com.example.waktusolatmalaysia.fragments.*;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;

public class PhoneUiV330Test {
  private Context context;
  private Map<String, ?> app, personal;

  @Before
  public void before() {
    context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    app = new HashMap<>(context.getSharedPreferences("waktu_solat", 0).getAll());
    personal = new HashMap<>(context.getSharedPreferences("personal_library", 0).getAll());
  }

  private void restore(String name, Map<String, ?> data) {
    SharedPreferences.Editor e = context.getSharedPreferences(name, 0).edit().clear();
    for (Map.Entry<String, ?> item : data.entrySet()) {
      Object v = item.getValue();
      String k = item.getKey();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>) v));
    }
    e.commit();
  }

  @After
  public void after() {
    restore("waktu_solat", app);
    restore("personal_library", personal);
    String theme = new AppPreferences(context).theme();
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () ->
                AppCompatDelegate.setDefaultNightMode(
                    theme.equals("system") ? -1 : theme.equals("dark") ? 2 : 1));
  }

  private void idle() {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    android.os.SystemClock.sleep(250);
  }

  private void capture(String name) throws Exception {
    UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    assertCaptureHealthy(device);
    String run = InstrumentationRegistry.getArguments().getString("capture_run", "");
    assertTrue("Safe capture run ID", run.matches("[A-Za-z0-9_-]*"));
    File folder = new File(context.getExternalFilesDir(null), "ui-v330" + (run.isEmpty() ? "" : "/" + run));
    folder.mkdirs();
    Bitmap b = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull(b);
    assertCaptureHealthy(device);
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    b.recycle();
  }

  private void assertCaptureHealthy(UiDevice device) {
    assertFalse("Refuse a screenshot containing an Android ANR/crash dialog",
        device.hasObject(By.res("android", "aerr_close"))
            || device.hasObject(By.res("android", "aerr_wait"))
            || device.hasObject(By.textContains("isn't responding"))
            || device.hasObject(By.textContains("keeps stopping")));
    assertEquals("Capture requires our app in the foreground, without a system overlay",
        context.getPackageName(), device.getCurrentPackageName());
  }

  private void tapHub(ActivityScenario<MainActivity> scenario, int route) {
    int[] point = new int[2];
    scenario.onActivity(a -> {
      View target = a.findViewById(R.id.bottom_nav).findViewById(route);
      assertNotNull(target);
      assertTrue(target.isShown());
      target.getLocationOnScreen(point);
      point[0] += target.getWidth() / 2;
      point[1] += target.getHeight() / 2;
    });
    assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).click(point[0], point[1]));
    idle();
    scenario.onActivity(a -> assertEquals("Real navigation tap was handled", route,
        ((NavHostFragment) a.getSupportFragmentManager().findFragmentById(R.id.nav_host))
            .getNavController().getCurrentDestination().getId()));
  }

  private void textFits(View root) {
    if (root.getVisibility() != View.VISIBLE) return;
    if (root instanceof TextView) {
      TextView t = (TextView) root;
      assertNotNull(t.getLayout());
      assertTrue(
          t.getText() + " height",
          t.getLayout().getHeight()
              <= t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom() + 2);
      for (int n = 0; n < t.getLineCount(); n++)
        assertEquals(t.getText() + " ellipsis", 0, t.getLayout().getEllipsisCount(n));
    }
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++)
        textFits(((ViewGroup) root).getChildAt(i));
  }

  @Test
  public void choicesWrapAtPhoneWidthsWithoutClipping() {
    try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
      s.onActivity(
          a -> {
            for (float scale : new float[] {1, 1.3f, 2})
              for (int width : new int[] {280, 320, 360, 600}) {
                Configuration cfg = new Configuration(a.getResources().getConfiguration());
                cfg.fontScale = scale;
                Context c =
                    new androidx.appcompat.view.ContextThemeWrapper(
                        a.createConfigurationContext(cfg), R.style.Theme_WaktuSolat);
                ChoiceBar bar =
                    new ChoiceBar(c, new String[] {"Doa", "Wirid", "Niat", "Hadis"}, null, 2, null);
                bar.measure(
                    View.MeasureSpec.makeMeasureSpec(Ui.dp(c, width), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                bar.layout(0, 0, bar.getMeasuredWidth(), bar.getMeasuredHeight());
                assertEquals(2, bar.selected());
                assertTrue(bar.getChildAt(2).isSelected());
                textFits(bar);
                for (int n = 0; n < 4; n++) {
                  View child = bar.getChildAt(n);
                  assertTrue(child.getHeight() >= Ui.dp(c, 48));
                  assertTrue(child.getRight() <= bar.getWidth());
                }
              }
          });
    }
  }

  @Test
  public void settingsFloatsOverHubAndThemeSurvivesRecreation() throws Exception {
    try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
      s.onActivity(a -> a.showSettings());
      idle();
      s.onActivity(
          a -> {
            SettingsPanel p =
                (SettingsPanel)
                    a.getSupportFragmentManager().findFragmentByTag("floating-settings");
            assertNotNull(p);
            assertTrue(p.getDialog().isShowing());
            assertEquals(
                R.id.prayerFragment,
                ((NavHostFragment) a.getSupportFragmentManager().findFragmentById(R.id.nav_host))
                    .getNavController()
                    .getCurrentDestination()
                    .getId());
            ChoiceBar themes = p.getView().findViewById(R.id.theme_choices);
            themes.getChildAt(2).performClick();
          });
      idle();
      s.recreate();
      idle();
      s.onActivity(
          a -> {
            assertEquals(
                Configuration.UI_MODE_NIGHT_YES,
                a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
            assertEquals(Color.parseColor("#171410"), Ui.color(a, R.color.background));
            SettingsPanel p =
                (SettingsPanel)
                    a.getSupportFragmentManager().findFragmentByTag("floating-settings");
            assertNotNull(p);
            assertEquals(2, ((ChoiceBar) p.getView().findViewById(R.id.theme_choices)).selected());
            assertNull(p.getView().findViewById(R.id.dark_switch));
            assertTrue(
                p.getDialog().getWindow().getDecorView().getWidth()
                    < a.getWindow().getDecorView().getWidth());
          });
      capture("settings-dark");
      s.onActivity(a->{SettingsPanel p=(SettingsPanel)a.getSupportFragmentManager().findFragmentByTag("floating-settings");((ChoiceBar)p.getView().findViewById(R.id.theme_choices)).getChildAt(0).performClick();});idle();
      s.onActivity(a->assertEquals(android.content.res.Resources.getSystem().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK,a.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK));
      s.onActivity(
          a -> {
            SettingsPanel p =
                (SettingsPanel)
                    a.getSupportFragmentManager().findFragmentByTag("floating-settings");
            p.getView().findViewById(R.id.settings_close).performClick();
          });
      idle();
      s.onActivity(
          a -> assertNull(a.getSupportFragmentManager().findFragmentByTag("floating-settings")));
    }
  }

  @Test
  public void screenshotAffectedHubsInBothThemesAndRetainFilters() throws Exception {
    // Explicit public sample configuration; restored by @After.
    context.getSharedPreferences("waktu_solat", 0).edit()
        .putString("zone_state", "Wilayah Persekutuan")
        .putString("zone_name", "Kuala Lumpur, Putrajaya")
        .putString("zone_code", "WLY01")
        .putString("language", "ms").putBoolean("twelve_hour", false).commit();
    for (String theme : new String[] {"light", "dark"}) {
      new AppPreferences(context).setTheme(theme);
      InstrumentationRegistry.getInstrumentation()
          .runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(theme.equals("dark") ? 2 : 1));
      try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
        idle();
        capture(theme + "-home");
        s.onActivity(
            a -> {
              View nav = a.findViewById(R.id.bottom_nav);
              if (nav.isShown()) {
                assertEquals(0, nav.getPaddingBottom());
                assertTrue(nav.getHeight() >= Ui.dp(a, 64));
              }
              ShortcutGrid grid = a.findViewById(R.id.shortcut_grid);
              assertNotNull(grid);
              int height = grid.getChildAt(0).getHeight();
              for (int n = 1; n < grid.getChildCount(); n++)
                assertEquals("Shortcuts have equal height", height, grid.getChildAt(n).getHeight());
            });
        s.onActivity(
            a -> {
              View grid = a.findViewById(R.id.shortcut_grid);
              grid.requestRectangleOnScreen(
                  new Rect(0, 0, grid.getWidth(), grid.getHeight()), true);
            });
        idle();
        capture(theme + "-shortcuts");
        for (int route :
            new int[] {R.id.worshipFragment, R.id.collectionFragment, R.id.exploreFragment}) {
          if (route == R.id.worshipFragment || route == R.id.exploreFragment) tapHub(s, route);
          else s.onActivity(a -> a.openDestination(route, null));
          idle();
          capture(
              theme
                  + "-"
                  + (route == R.id.worshipFragment
                      ? "worship"
                      : route == R.id.collectionFragment ? "collection" : "explore"));
        }
        s.onActivity(
            a -> ((ChoiceBar) a.findViewById(R.id.explore_filters)).getChildAt(2).performClick());
        s.recreate();
        idle();
        s.onActivity(
            a -> assertEquals(2, ((ChoiceBar) a.findViewById(R.id.explore_filters)).selected()));
        s.onActivity(a -> a.showSettings());
        idle();
        capture(theme + "-settings");
      }
    }
  }

  @Test
  public void phoneWidgetAllocationsKeepRealPrayerInformation() throws Exception {
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
    try (ActivityScenario<MainActivity> s = ActivityScenario.launch(MainActivity.class)) {
      for(boolean twelve:new boolean[]{false,true})for(String lang:new String[]{"ms","en"}) {
      context.getSharedPreferences("waktu_solat",0).edit().putBoolean("twelve_hour",twelve).putString("language",lang).commit();
      for (WidgetProviderDescriptor d : WidgetProviderDescriptor.ALL) {
        WidgetRenderModel model =
            new WidgetRenderModel(
                new PrayerZone("Wilayah Persekutuan", "Kuala Lumpur", "WLY01"),
                new PrayerDay(date, "1448-05-23", times),
                new PrayerDay(date.plusDays(1), "1448-05-24", times),
                date.atTime(14, 0).atZone(PrayerClock.MALAYSIA),
                d,
                Collections.emptyMap());
        WidgetConfigV2 config =
            new WidgetConfigV2(new JSONObject().put("schemaVersion", 2).put("theme", "light"), d);
        int[][] sizes =
            d == WidgetProviderDescriptor.NEXT
                ? new int[][] {{130, 110}, {150, 130}}
                : d == WidgetProviderDescriptor.STRIP
                    ? new int[][] {{250, 70}, {250, 82}, {280, 90}}
                    : new int[][] {{250, 130}, {280, 150}, {180, 190}};
        for (int[] size : sizes)
          s.onActivity(
              a -> {
                RemoteViews rv =
                    PrayerWidgets.render(a, 0, d, config, model, size[0], size[1], true);
                View root = rv.apply(a, new FrameLayout(a));
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(Ui.dp(a, size[0]), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(Ui.dp(a, size[1]), View.MeasureSpec.EXACTLY));
                root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
                assertNull(
                    d.component.getSimpleName()
                        + " "
                        + Arrays.toString(size)
                        + " must not ask for resizing",
                    root.findViewById(R.id.widget_message));
                assertNull(root.findViewById(R.id.widget_resize_icon));
                if (d != WidgetProviderDescriptor.NEXT)
                  for (int i : PrayerDay.OBLIGATORY)
                    assertNotNull(
                        root.findViewById(
                            a.getResources()
                                .getIdentifier("widget_value_" + i, "id", a.getPackageName())));
                textFits(root);
              });
      }
      }
    }
  }
}
