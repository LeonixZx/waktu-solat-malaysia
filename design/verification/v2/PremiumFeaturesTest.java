package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.services.*;
import com.example.waktusolatmalaysia.utils.*;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PremiumFeaturesTest {
  private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

  static void await(ActivityScenario<MainActivity> scenario, Predicate<MainActivity> check) {
    long end = SystemClock.elapsedRealtime() + 40000;
    AtomicBoolean ready = new AtomicBoolean();
    do {
      scenario.onActivity(a -> ready.set(check.test(a)));
      if (ready.get()) return;
      SystemClock.sleep(100);
    } while (SystemClock.elapsedRealtime() < end);
    fail("Feature must finish loading");
  }

  static void open(ActivityScenario<MainActivity> scenario, int id) {
    scenario.onActivity(a -> a.openDestination(id, null));
    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
  }

  static int children(MainActivity a, int id) {
    ViewGroup list = a.findViewById(id);
    return list == null ? 0 : list.getChildCount();
  }

  @Test
  public void quranSearchReaderBookmarksAndCachedTextWork() throws Exception {
    UserStore store = new UserStore(context);
    boolean saved = store.saved("surah:1"), ayah = store.saved("ayah:1:2");
    int last = store.number("last_surah", 1), verse = store.number("last_verse", 1);
    String name = store.text("last_name", "Al-Fatihah");
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      open(scenario, R.id.quranFragment);
      await(scenario, a -> children(a, R.id.quran_list) == 114);
      TabletFeatureTest.capture("premium-quran");
      onView(withId(R.id.quran_search))
          .perform(
              TabletFeatureTest.reveal(),
              replaceText("1"),
              androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
      await(scenario, a -> children(a, R.id.quran_list) > 1);
      onView(withId(R.id.quran_search))
          .perform(
              replaceText("Al-Faatiha"),
              androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
      await(scenario, a -> children(a, R.id.quran_list) == 1);
      scenario.onActivity(
          a -> ((ViewGroup) a.findViewById(R.id.quran_list)).getChildAt(0).performClick());
      await(scenario, a -> children(a, R.id.reader_verses) == 8);
      onView(withId(R.id.reader_bookmark)).perform(TabletFeatureTest.reveal(), click());
      assertEquals(!saved, store.saved("surah:1"));
      TabletFeatureTest.capture("premium-reader");
      var text = new ContentRepository(context).verses(1);
      assertEquals(7, text.size());
      assertTrue(text.get(0).arabic.length() > 10);
      assertTrue(text.get(0).meaning.contains("Allah"));
      assertTrue(text.get(0).audio.startsWith("https://"));
      if (!store.saved("ayah:1:2")) store.toggle("ayah:1:2");
      androidx.test.espresso.Espresso.pressBack();
      await(scenario, a -> a.findViewById(R.id.quran_search) != null);
      onView(withText("Tersimpan")).perform(TabletFeatureTest.reveal(), click());
      onView(withText(containsString("Ayat 2")))
          .perform(TabletFeatureTest.reveal())
          .check(matches(isDisplayed()));
    } finally {
      if (store.saved("surah:1") != saved) store.toggle("surah:1");
      if (store.saved("ayah:1:2") != ayah) store.toggle("ayah:1:2");
      store.bookmark(last, verse, name);
    }
  }

  @Test
  public void allFourCollectionsOpenRealReadingsAndSave() throws Exception {
    UserStore store = new UserStore(context);
    String tab = store.text("collection_tab", "doa");
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      open(scenario, R.id.collectionFragment);
      String[] kinds = {"doa", "hadith", "wirid", "niat"},
          titles = {"Doa", "Hadis", "Wirid", "Niat"};
      for (int i = 0; i < 4; i++) {
        String kind = kinds[i];
        onView(withText(titles[i])).perform(TabletFeatureTest.reveal(), click());
        int count = new ContentRepository(context).readings(kind).size();
        await(scenario, a -> children(a, R.id.collection_list) == count);
        List<Reading> readings = new ContentRepository(context).readings(kind);
        assertFalse(readings.get(0).arabic.isBlank());
        assertFalse(readings.get(0).meaning.isBlank());
        if (i == 1) TabletFeatureTest.capture("premium-hadith");
        scenario.onActivity(
            a -> ((ViewGroup) a.findViewById(R.id.collection_list)).getChildAt(0).performClick());
        await(scenario, a -> a.findViewById(R.id.reading_bookmark) != null);
        boolean initial = store.saved(kind + ":0");
        onView(withId(R.id.reading_bookmark)).perform(TabletFeatureTest.reveal(), click());
        assertEquals(!initial, store.saved(kind + ":0"));
        store.toggle(kind + ":0");
        androidx.test.espresso.Espresso.pressBack();
        await(scenario, a -> a.findViewById(R.id.collection_list) != null);
      }
    } finally {
      store.set("collection_tab", tab);
    }
  }

  @Test
  public void tasbihCountsPersistAndResetIsConfirmed() throws Exception {
    UserStore store = new UserStore(context);
    int count = store.number("tasbih_count", 0), goal = store.number("tasbih_goal", 33);
    store.set("tasbih_count", 0);
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      open(scenario, R.id.tasbihFragment);
      for (int i = 0; i < 5; i++)
        onView(withId(R.id.tasbih_tap)).perform(TabletFeatureTest.reveal(), click());
      assertEquals(5, store.number("tasbih_count", 0));
      scenario.recreate();
      await(scenario, a -> a.findViewById(R.id.tasbih_tap) != null);
      assertEquals(5, store.number("tasbih_count", 0));
      TabletFeatureTest.capture("premium-tasbih");
      onView(withId(R.id.tasbih_reset)).perform(TabletFeatureTest.reveal(), click());
      onView(withText("Batal")).perform(click());
      assertEquals(5, store.number("tasbih_count", 0));
      onView(withId(R.id.tasbih_reset)).perform(click());
      onView(withId(android.R.id.button1)).perform(click());
      assertEquals(0, store.number("tasbih_count", -1));
    } finally {
      store.set("tasbih_count", count);
      store.set("tasbih_goal", goal);
    }
  }

  @Test
  public void scheduleDayWeekMonthAndCalendarWork() throws Exception {
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      open(scenario, R.id.scheduleFragment);
      await(scenario, a -> children(a, R.id.schedule_rows) == 8);
      TabletFeatureTest.capture("premium-schedule");
      onView(withText("Mingguan")).perform(TabletFeatureTest.reveal(), click());
      await(scenario, a -> children(a, R.id.schedule_rows) == 1);
      scenario.onActivity(
          a -> {
            HorizontalScrollView horizontal =
                (HorizontalScrollView)
                    ((ViewGroup) a.findViewById(R.id.schedule_rows)).getChildAt(0);
            assertEquals(8, ((ViewGroup) horizontal.getChildAt(0)).getChildCount());
          });
      onView(withText("Bulanan")).perform(TabletFeatureTest.reveal(), click());
      await(
          scenario,
          a -> {
            if (children(a, R.id.schedule_rows) != 1) return false;
            HorizontalScrollView horizontal =
                (HorizontalScrollView)
                    ((ViewGroup) a.findViewById(R.id.schedule_rows)).getChildAt(0);
            return ((ViewGroup) horizontal.getChildAt(0)).getChildCount()
                == YearMonth.now(PrayerClock.MALAYSIA).lengthOfMonth() + 1;
          });
      TabletFeatureTest.capture("premium-monthly");
      open(scenario, R.id.calendarFragment);
      await(scenario, a -> a.findViewById(R.id.calendar_grid) != null);
      scenario.onActivity(
          a -> {
            GridLayout grid = a.findViewById(R.id.calendar_grid);
            assertEquals(
                7
                    + YearMonth.now(PrayerClock.MALAYSIA).atDay(1).getDayOfWeek().getValue()
                    - 1
                    + YearMonth.now(PrayerClock.MALAYSIA).lengthOfMonth(),
                grid.getChildCount());
          });
      TabletFeatureTest.capture("premium-calendar");
    }
  }

  @Test
  public void reminderSchedulesActualFuturePrayerAndCancels() throws Exception {
    UserStore store = new UserStore(context);
    boolean original = store.flag("reminder_1", false);
    new PrayerRepository(context)
        .month(new AppPreferences(context).zone().code, YearMonth.now(PrayerClock.MALAYSIA), false);
    try {
      store.set("reminder_1", true);
      ReminderScheduler.scheduleNow(context);
      ZonedDateTime scheduled = ZonedDateTime.parse(store.text("scheduled_1", ""));
      assertTrue(scheduled.isAfter(ZonedDateTime.now(PrayerClock.MALAYSIA)));
      var month =
          new PrayerRepository(context)
              .month(new AppPreferences(context).zone().code, YearMonth.from(scheduled), false);
      assertEquals(month.days.get(scheduled.toLocalDate()).times.get(1), scheduled.toLocalTime());
      store.set("reminder_1", false);
      ReminderScheduler.scheduleNow(context);
      assertEquals("", store.text("scheduled_1", ""));
    } finally {
      store.set("reminder_1", original);
      ReminderScheduler.scheduleNow(context);
    }
  }

  @Test
  public void azanOptionPersistsAndAudioResourcesDecodeWithoutPlayback() throws Exception {
    UserStore store = new UserStore(context);
    boolean original = store.flag("azan_mode", false);
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      open(scenario, R.id.scheduleFragment);
      await(scenario, a -> children(a, R.id.schedule_rows) == 8);
      onView(withId(R.id.azan_switch)).perform(TabletFeatureTest.reveal(), click());
      assertEquals(!original, store.flag("azan_mode", false));
      scenario.recreate();
      await(scenario, a -> a.findViewById(R.id.azan_switch) != null);
      onView(withId(R.id.azan_switch))
          .perform(TabletFeatureTest.reveal())
          .check(matches(original ? isNotChecked() : isChecked()));
    } finally {
      store.set("azan_mode", original);
    }
    for (int resource : new int[] {R.raw.adhan_standard, R.raw.adhan_fajr}) {
      android.media.MediaPlayer media = new android.media.MediaPlayer();
      try (var file = context.getResources().openRawResourceFd(resource)) {
        media.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
        media.prepare();
        assertTrue("Azan recording must decode", media.getDuration() > 30000);
      } finally {
        media.release();
      }
    }
    var ayah = new ContentRepository(context).verses(1).get(0);
    android.media.MediaPlayer media = new android.media.MediaPlayer();
    var ready = new java.util.concurrent.CountDownLatch(1);
    AtomicBoolean prepared = new AtomicBoolean();
    try {
      media.setDataSource(ayah.audio);
      media.setOnPreparedListener(
          mp -> {
            prepared.set(true);
            ready.countDown();
          });
      media.setOnErrorListener(
          (mp, a, b) -> {
            ready.countDown();
            return true;
          });
      media.prepareAsync();
      assertTrue(ready.await(30, java.util.concurrent.TimeUnit.SECONDS));
      assertTrue("Quran audio must prepare without starting playback", prepared.get());
    } finally {
      media.release();
    }
  }
}
