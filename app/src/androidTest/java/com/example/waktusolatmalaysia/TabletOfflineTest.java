package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.os.*;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.time.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TabletOfflineTest {
  @Test
  public void savedContentWorksAndMissingSchedulesAreHonest() throws Exception {
    assumeTrue(
        "Explicit offline run required",
        "true".equals(InstrumentationRegistry.getArguments().getString("offline")));
    var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    String zone = new AppPreferences(context).zone().code;
    var saved =
        new PrayerRepository(context).month(zone, YearMonth.now(PrayerClock.MALAYSIA), false);
    assertTrue(saved.cached);
    assertEquals(7, new ContentRepository(context).verses(1).size());
    assertEquals(12, new ContentRepository(context).readings("hadith").size());
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      TabletFeatureTest.waitForSchedule(scenario);
      onView(withId(R.id.refresh)).perform(TabletFeatureTest.reveal(), click());
      TabletFeatureTest.waitForSchedule(scenario);
      scenario.onActivity(
          a -> {
            var state = TabletFeatureTest.model(a).state().getValue();
            assertTrue(state.offline);
            assertEquals(saved.days.get(state.date).times, state.day.times);
            ViewGroup rows = a.findViewById(R.id.prayer_rows);
            assertEquals(5, rows.getChildCount());
          });
      onView(withId(R.id.status))
          .perform(TabletFeatureTest.reveal())
          .check(matches(withText(R.string.offline_status)));
      TabletFeatureTest.capture("premium-offline");
      scenario.onActivity(
          a -> {
            Bundle args = new Bundle();
            args.putString("date", "1901-01-02");
            a.openDestination(R.id.scheduleFragment, args);
          });
      AtomicBoolean done = new AtomicBoolean();
      long end = SystemClock.elapsedRealtime() + 35000;
      do {
        scenario.onActivity(
            a -> {
              TextView status = a.findViewById(R.id.status);
              done.set(
                  status != null
                      && status
                          .getText()
                          .toString()
                          .equals(a.getString(R.string.network_error)));
            });
        if (done.get()) break;
        SystemClock.sleep(100);
      } while (SystemClock.elapsedRealtime() < end);
      assertTrue("Missing timetable should explain network failure", done.get());
      scenario.onActivity(
          a -> assertEquals(0, ((ViewGroup) a.findViewById(R.id.schedule_rows)).getChildCount()));
    }
  }
}
