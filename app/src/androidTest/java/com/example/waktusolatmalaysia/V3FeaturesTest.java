package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.ui.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.widgets.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class V3FeaturesTest {
  private final Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
  private Map<String, ?> appBefore, personalBefore, widgetsBefore;
  private byte[] recoveryBefore;
  private List<PrayerRelativeReminder> tasksBefore;

  @Before
  public void snapshot() throws Exception {
    appBefore = new HashMap<>(c.getSharedPreferences("waktu_solat", 0).getAll());
    personalBefore = new HashMap<>(c.getSharedPreferences("personal_library", 0).getAll());
    tasksBefore = new TaskStore(c).load();
    widgetsBefore = new HashMap<>(c.getSharedPreferences("widget_config", 0).getAll());
    android.util.AtomicFile recovery =
        new android.util.AtomicFile(new java.io.File(c.getFilesDir(), "backup-before-import.json"));
    recoveryBefore = recovery.getBaseFile().exists() ? recovery.readFully() : null;
  }

  static void restorePrefs(android.content.SharedPreferences prefs, Map<String, ?> data) {
    var e = prefs.edit().clear();
    for (var item : data.entrySet()) {
      String k = item.getKey();
      Object v = item.getValue();
      if (v instanceof String) e.putString(k, (String) v);
      else if (v instanceof Integer) e.putInt(k, (Integer) v);
      else if (v instanceof Long) e.putLong(k, (Long) v);
      else if (v instanceof Float) e.putFloat(k, (Float) v);
      else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
      else if (v instanceof Set) e.putStringSet(k, (Set<String>) v);
    }
    e.commit();
  }

  @After
  public void restore() throws Exception {
    new TaskStore(c).replace(tasksBefore);
    restorePrefs(c.getSharedPreferences("waktu_solat", 0), appBefore);
    restorePrefs(c.getSharedPreferences("personal_library", 0), personalBefore);
    restorePrefs(c.getSharedPreferences("widget_config", 0), widgetsBefore);
    android.util.AtomicFile recovery =
        new android.util.AtomicFile(new java.io.File(c.getFilesDir(), "backup-before-import.json"));
    if (recoveryBefore == null) recovery.delete();
    else {
      java.io.FileOutputStream output = recovery.startWrite();
      output.write(recoveryBefore);
      recovery.finishWrite(output);
    }
    ReminderScheduler.scheduleNow(c);
  }

  static void waitFor(ActivityScenario<MainActivity> s, Predicate<MainActivity> condition) {
    long end = SystemClock.elapsedRealtime() + 40000;
    AtomicBoolean ready = new AtomicBoolean();
    do {
      s.onActivity(a -> ready.set(condition.test(a)));
      if (ready.get()) return;
      SystemClock.sleep(100);
    } while (SystemClock.elapsedRealtime() < end);
    fail("Feature did not become ready");
  }

  static int count(MainActivity a, int id) {
    View v = a.findViewById(id);
    return v instanceof RecyclerView
        ? ((RecyclerView) v).getAdapter() == null
            ? 0
            : ((RecyclerView) v).getAdapter().getItemCount()
        : v instanceof ViewGroup ? ((ViewGroup) v).getChildCount() : 0;
  }

  static void open(ActivityScenario<MainActivity> s, int id) {
    s.onActivity(a -> a.openDestination(id, null));
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  @Test
  public void fiveHubsAndNewFeaturesNavigateWithoutCrashes() throws Exception {
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      int[] ids = {
        R.id.worshipFragment,
        R.id.exploreFragment,
        R.id.savedFragment,
        R.id.tasksFragment,
        R.id.downloadsFragment,
        R.id.backupFragment,
        R.id.widgetsFragment,
        R.id.onboardingFragment,
        R.id.prayerFragment
      };
      for (int id : ids) {
        open(s, id);
        SystemClock.sleep(250);
        s.onActivity(a -> assertNotNull(a.findViewById(R.id.nav_host)));
      }
      waitFor(s, a -> a.findViewById(R.id.clock) != null);
      TabletFeatureTest.capture("v3-home");
    }
  }

  @Test
  public void virtualQuranReaderAndSavedSurahPreserveIds() throws Exception {
    UserStore store = new UserStore(c);
    boolean saved = store.saved("surah:1");
    int n = store.number("last_surah", 1), v = store.number("last_verse", 1);
    String name = store.text("last_name", "Al-Fatihah");
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      open(s, R.id.quranFragment);
      waitFor(s, a -> count(a, R.id.quran_list) == 114);
      onView(withId(R.id.quran_search)).perform(replaceText("Al-Faatiha"), closeSoftKeyboard());
      waitFor(s, a -> count(a, R.id.quran_list) == 1);
      s.onActivity(
          a -> {
            Bundle b = new Bundle();
            b.putInt("surah", 1);
            a.openDestination(R.id.quranReaderFragment, b);
          });
      waitFor(s, a -> count(a, R.id.reader_verses) == 7);
      onView(withId(R.id.reader_bookmark)).perform(click());
      assertEquals(!saved, store.saved("surah:1"));
      var en = new ContentRepository(c).verses(1, "en");
      assertEquals(7, en.size());
      assertTrue(en.get(0).audio.startsWith("https://"));
      TabletFeatureTest.capture("v3-reader");
      open(s, R.id.savedFragment);
      waitFor(s, a -> a.findViewById(R.id.saved_list) != null);
    } finally {
      if (store.saved("surah:1") != saved) store.toggle("surah:1");
      store.bookmark(n, v, name);
    }
  }

  @Test
  public void suppliedCollectionsAndEnglishReadingsStayNonEmpty() throws Exception {
    String language = c.getSharedPreferences("waktu_solat", 0).getString("language", "ms");
    String tab = new UserStore(c).text("collection_tab", "doa");
    try {
      for (String lang : new String[] {"ms", "en"}) {
        I18n.language(c, lang);
        Context localized = I18n.wrap(c);
        for (String kind : new String[] {"doa", "hadith", "wirid", "niat"}) {
          var readings = new ContentRepository(localized).readings(kind);
          assertFalse(readings.isEmpty());
          assertFalse(readings.get(0).arabic.isEmpty());
          assertFalse(readings.get(0).meaning.isEmpty());
        }
      }
    } finally {
      I18n.language(c, language);
      new UserStore(c).set("collection_tab", tab);
    }
  }

  @Test
  public void reminderModesMigrateAndPersonalTasksScheduleAndCancel() throws Exception {
    UserStore store = new UserStore(c);
    PrayerMode original = PrayerMode.get(c, 1);
    List<PrayerRelativeReminder> before = new TaskStore(c).load();
    try {
      new PrayerRepository(c)
          .month(
              new AppPreferences(c).zone().code,
              YearMonth.now(com.example.waktusolatmalaysia.utils.PrayerClock.MALAYSIA),
              false);
      PrayerMode.set(c, 1, PrayerMode.NOTIFICATION);
      var task =
          new PrayerRelativeReminder(
              "v3-test", "Test task", 7, 360, Set.of(1, 2, 3, 4, 5, 6, 7), true);
      new TaskStore(c).put(task);
      ReminderScheduler.scheduleNow(c);
      assertFalse(store.text("scheduled_1", "").isEmpty());
      assertFalse(store.text("scheduled_task_v3-test", "").isEmpty());
      String generation = store.text("alarm_generation", "");
      new TaskStore(c).remove(task.id);
      PrayerMode.set(c, 1, PrayerMode.OFF);
      ReminderScheduler.scheduleNow(c);
      assertFalse(generation.equals(store.text("alarm_generation", "")));
      assertTrue(store.text("scheduled_1", "").isEmpty());
      Intent stale =
          new Intent()
              .putExtra("generation", generation)
              .putExtra("zone", new AppPreferences(c).zone().code)
              .putExtra("when", System.currentTimeMillis());
      assertFalse(ReminderScheduler.current(c, stale));
    } finally {
      new TaskStore(c).replace(before);
      PrayerMode.set(c, 1, original);
      ReminderScheduler.scheduleNow(c);
    }
  }

  @Test
  public void widgetLayoutsRenderAllSizesAndThemes() throws Exception {
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      for (String theme : new String[] {"light", "dark", "system"}) {
        c.getSharedPreferences("widget_config", 0)
            .edit()
            .putString(
                "98765",
                new org.json.JSONObject()
                    .put("followApp", false)
                    .put("zone", "WLY01")
                    .put("name", "Kuala Lumpur")
                    .put("theme", theme)
                    .toString())
            .commit();
        for (int kind = 0; kind < 3; kind++) {
          final int type = kind;
          s.onActivity(
              a -> {
                FrameLayout parent = new FrameLayout(a);
                View widget = PrayerWidgets.render(a, 98765, type).apply(a, parent);
                widget.measure(
                    View.MeasureSpec.makeMeasureSpec(
                        Ui.dp(a, type == 0 ? 180 : 320), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                        Ui.dp(a, type == 2 ? 360 : 180), View.MeasureSpec.EXACTLY));
                widget.layout(0, 0, widget.getMeasuredWidth(), widget.getMeasuredHeight());
                assertNotNull(widget.findViewById(R.id.widget_zone));
                assertTrue(
                    ((TextView) widget.findViewById(R.id.widget_zone))
                        .getText()
                        .toString()
                        .contains("WLY01"));
              });
        }
      }
    } finally {
      c.getSharedPreferences("widget_config", 0).edit().remove("98765").commit();
    }
  }

  @Test
  public void backupRoundTripValidatesAndDisablesImportedAlarms() throws Exception {
    var original = BackupStore.export(c);
    try {
      var file = new org.json.JSONObject(original.toString());
      file.getJSONArray("bookmarks").put("surah:114");
      file.getJSONArray("reminders")
          .put(new PrayerRelativeReminder("backup-test", "Read", 1, 15, Set.of(1), true).json());
      BackupStore.validate(file);
      BackupStore.apply(c, file, true);
      assertTrue(new UserStore(c).saved("surah:114"));
      for (PrayerRelativeReminder t : new TaskStore(c).load())
        if (t.id.equals("backup-test")) assertFalse(t.enabled);
      for (int i : PrayerDay.OBLIGATORY) assertEquals(PrayerMode.OFF, PrayerMode.get(c, i));
    } finally {
      BackupStore.apply(c, original, false);
    }
  }

  @Test
  public void languageAndThemeSurviveRecreation() throws Exception {
    String lang = c.getSharedPreferences("waktu_solat", 0).getString("language", "ms");
    try {
      I18n.language(c, "en");
      try (var s = ActivityScenario.launch(MainActivity.class)) {
        open(s, R.id.settingsFragment);
        waitFor(s, a -> a.findViewById(R.id.locale_button) != null);
        onView(withId(R.id.locale_button)).check(matches(withText("Language · English")));
        s.recreate();
        waitFor(s, a -> a.findViewById(R.id.locale_button) != null);
        onView(withId(R.id.locale_button)).check(matches(withText("Language · English")));
        TabletFeatureTest.capture("v3-settings-en");
      }
    } finally {
      I18n.language(c, lang);
    }
  }
}
