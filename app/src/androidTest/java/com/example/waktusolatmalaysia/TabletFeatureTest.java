package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.model.PrayerZone;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import com.example.waktusolatmalaysia.vms.PrayerViewModel;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Real-device checks. Changes stay inside this app; no device-wide settings are changed. */
@RunWith(AndroidJUnit4.class)
public class TabletFeatureTest {
  private static final Locale MALAY = Locale.forLanguageTag("ms-MY");

  static ViewAction reveal() {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isAssignableFrom(View.class);
      }

      @Override
      public String getDescription() {
        return "Reveal control within its scrolling parent";
      }

      @Override
      public void perform(UiController ui, View view) {
        view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), view.getHeight()), true);
        ui.loopMainThreadUntilIdle();
      }
    };
  }

  static void capture(String name) throws Exception {
    var instrumentation = InstrumentationRegistry.getInstrumentation();
    instrumentation.waitForIdleSync();
    // Fragment views can be attached before their navigation transition reaches the display.
    SystemClock.sleep(450);
    instrumentation.waitForIdleSync();
    File directory =
        new File(
            instrumentation.getTargetContext().getExternalFilesDir(null), "tablet-verification");
    assertTrue(directory.isDirectory() || directory.mkdirs());
    File target=new File(directory,name+".png");
    // Shell capture follows the display's current rotation. UiAutomation.takeScreenshot
    // can retain the old display bounds on the tablet after an orientation change.
    try (var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.getUiAutomation().executeShellCommand("screencap -p"));
        FileOutputStream output = new FileOutputStream(target)) {
      byte[] buffer=new byte[65536];int count;
      while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
    }
    var bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    android.graphics.BitmapFactory.decodeFile(target.getAbsolutePath(),bounds);
    var device=androidx.test.uiautomator.UiDevice.getInstance(instrumentation);
    assertEquals("Screenshot width follows current display",device.getDisplayWidth(),bounds.outWidth);
    assertEquals("Screenshot height follows current display",device.getDisplayHeight(),bounds.outHeight);
  }

  static PrayerViewModel model(MainActivity activity) {
    NavHostFragment host =
        (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host);
    assertNotNull(host);
    Fragment fragment = host.getChildFragmentManager().getPrimaryNavigationFragment();
    assertNotNull(fragment);
    return new ViewModelProvider(fragment).get(PrayerViewModel.class);
  }

  static void waitForHomeSchedule(ActivityScenario<MainActivity> scenario) {
    long deadline = SystemClock.elapsedRealtime() + 35000;
    AtomicReference<PrayerViewModel.State> latest = new AtomicReference<>();
    do {
      scenario.onActivity(activity -> latest.set(model(activity).state().getValue()));
      if (latest.get() != null && !latest.get().loading) break;
      SystemClock.sleep(100);
    } while (SystemClock.elapsedRealtime() < deadline);
    assertNotNull("A timetable state must be published", latest.get());
    assertFalse("Timetable load must finish", latest.get().loading);
    assertFalse("Selected timetable must load", latest.get().error);
    assertNotNull(latest.get().day);
  }

  static void waitForSchedule(ActivityScenario<MainActivity> scenario) {
    AtomicBoolean schedule = new AtomicBoolean();
    scenario.onActivity(a -> schedule.set(a.findViewById(R.id.schedule_rows) != null));
    if (!schedule.get()) {
      waitForHomeSchedule(scenario);
      return;
    }
    long deadline = SystemClock.elapsedRealtime() + 35000;
    AtomicBoolean ready = new AtomicBoolean();
    do {
      scenario.onActivity(
          a -> {
            android.view.ViewGroup rows = a.findViewById(R.id.schedule_rows);
            ready.set(rows != null && rows.getChildCount() > 0);
          });
      if (ready.get()) return;
      SystemClock.sleep(100);
    } while (SystemClock.elapsedRealtime() < deadline);
    fail("Schedule rows must load");
  }

  private static void assertDate(LocalDate date) {
    onView(withId(R.id.date_button))
        .check(matches(withText(date.format(DateTimeFormatter.ofPattern("d MMM yyyy", MALAY)))));
  }

  private static void homeTool(ActivityScenario<MainActivity> scenario, int item) {
    onView(withId(item == R.id.scheduleFragment ? R.id.all_times : R.id.location_button))
        .perform(reveal(), click());
  }

  private static void waitForDestination(ActivityScenario<MainActivity> scenario, int destination) {
    long deadline = SystemClock.elapsedRealtime() + 10000;
    AtomicBoolean ready = new AtomicBoolean();
    AtomicReference<String> current = new AtomicReference<>();
    do {
      scenario.onActivity(
          activity -> {
            NavHostFragment host =
                (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.nav_host);
            var navigation = host.getNavController();
            current.set(
                navigation.getCurrentDestination().getDisplayName()
                    + "/"
                    + navigation.getCurrentBackStackEntry().getLifecycle().getCurrentState());
            ready.set(
                navigation.getCurrentDestination() != null
                    && navigation.getCurrentDestination().getId() == destination
                    && activity.getLifecycle().getCurrentState() == Lifecycle.State.RESUMED);
          });
      if (ready.get()) return;
      SystemClock.sleep(100);
    } while (SystemClock.elapsedRealtime() < deadline);
    try {
      capture("navigation-failure");
    } catch (Exception ignored) {
    }
    assertTrue(
        "Navigation must settle on destination " + destination + "; current=" + current.get(),
        ready.get());
  }

  @Test
  public void dailyTimesAndCountdownMatchTheDownloadedSchedule() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      waitForSchedule(scenario);
      scenario.onActivity(
          activity -> {
            var state = model(activity).state().getValue();
            assertNotNull(state);
            android.view.ViewGroup rows = activity.findViewById(R.id.prayer_rows);
            assertEquals(5, rows.getChildCount());
            boolean twelveHour = new AppPreferences(activity).isTwelveHour();
            var format =
                DateTimeFormatter.ofPattern(twelveHour ? "h:mm a" : "HH:mm", Locale.ENGLISH);
            for (int index = 0; index < 5; index++)
              assertEquals(
                  state
                      .day
                      .times
                      .get(com.example.waktusolatmalaysia.model.PrayerDay.OBLIGATORY[index])
                      .format(format),
                  ((TextView) rows.getChildAt(index).findViewById(R.id.time)).getText().toString());
            var next =
                PrayerClock.next(
                    state.day, state.tomorrow, java.time.ZonedDateTime.now(PrayerClock.MALAYSIA));
            if (next != null)
              assertEquals(
                  next.time.format(format),
                  ((TextView) activity.findViewById(R.id.next_time)).getText().toString());
          });
      capture("today");
    }
  }

  @Test
  public void previousNextDatePickerAndReturnToTodayWork() {
    LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      waitForSchedule(scenario);
      homeTool(scenario, R.id.scheduleFragment);
      waitForDestination(scenario, R.id.scheduleFragment);
      waitForSchedule(scenario);
      onView(withId(R.id.next)).perform(reveal(), click());
      waitForSchedule(scenario);
      assertDate(today.plusDays(1));
      onView(withId(R.id.previous)).perform(reveal(), click());
      waitForSchedule(scenario);
      assertDate(today);
      onView(withId(R.id.previous)).perform(reveal(), click());
      waitForSchedule(scenario);
      assertDate(today.minusDays(1));
      scenario.recreate();
      assertDate(today.minusDays(1));
      onView(withId(R.id.today)).perform(reveal(), click());
      waitForSchedule(scenario);
      assertDate(today);
      onView(withId(R.id.date_button)).perform(reveal(), click());
      LocalDate chosen = today.withDayOfMonth(today.getDayOfMonth() == 15 ? 16 : 15);
      onView(isAssignableFrom(DatePicker.class))
          .perform(
              new ViewAction() {
                @Override
                public Matcher<View> getConstraints() {
                  return isAssignableFrom(DatePicker.class);
                }

                @Override
                public String getDescription() {
                  return "Choose another date in the displayed month";
                }

                @Override
                public void perform(UiController ui, View view) {
                  ((DatePicker) view)
                      .updateDate(
                          chosen.getYear(), chosen.getMonthValue() - 1, chosen.getDayOfMonth());
                  ui.loopMainThreadUntilIdle();
                }
              });
      onView(withId(android.R.id.button1)).perform(click());
      waitForSchedule(scenario);
      assertDate(chosen);
      onView(withId(R.id.today)).perform(reveal(), click());
      waitForSchedule(scenario);
      assertDate(today);
    }
  }

  @Test
  public void stateAndDistrictSelectionPersistAndLoadTheirOwnTimetable() {
    var preferences =
        new AppPreferences(InstrumentationRegistry.getInstrumentation().getTargetContext());
    PrayerZone original = preferences.zone();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      homeTool(scenario, R.id.locationFragment);
      waitForDestination(scenario, R.id.locationFragment);
      onView(
              allOf(
                  withId(com.google.android.material.R.id.text_input_end_icon),
                  isDescendantOfA(withId(R.id.state_input))))
          .perform(reveal(), click());
      onData(equalTo("Selangor")).inRoot(isPlatformPopup()).perform(click());
      onView(
              allOf(
                  withId(com.google.android.material.R.id.text_input_end_icon),
                  isDescendantOfA(withId(R.id.district_input))))
          .perform(reveal(), click());
      onData(instanceOf(PrayerZone.class)).inRoot(isPlatformPopup()).atPosition(2).perform(click());
      onView(withId(R.id.district)).check(matches(withText(containsString("SGR03"))));
      scenario.recreate();
      waitForDestination(scenario, R.id.locationFragment);
      onView(withId(R.id.state)).check(matches(withText("Selangor")));
      onView(withId(R.id.district)).check(matches(withText(containsString("SGR03"))));
      try {
        capture("zone-before-save");
      } catch (Exception failure) {
        throw new AssertionError(failure);
      }
      onView(withId(R.id.save)).perform(reveal(), click());
      waitForDestination(scenario, R.id.prayerFragment);
      assertEquals("SGR03", preferences.zone().code);
      waitForSchedule(scenario);
      scenario.onActivity(
          activity -> assertEquals("SGR03", model(activity).state().getValue().zone.code));
    } finally {
      preferences.setZone(original);
    }
  }

  @Test
  public void portraitAndLandscapeKeepControlsReachable() throws Exception {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      waitForDestination(scenario, R.id.prayerFragment);
      for (int orientation :
          new int[] {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          }) {
        scenario.onActivity(activity -> activity.setRequestedOrientation(orientation));
        long deadline = SystemClock.elapsedRealtime() + 10000;
        AtomicBoolean correct = new AtomicBoolean();
        int expected =
            orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
        do {
          scenario.onActivity(
              activity ->
                  correct.set(activity.getResources().getConfiguration().orientation == expected));
          if (correct.get()) break;
          SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertTrue("Requested app orientation must take effect", correct.get());
        waitForDestination(scenario, R.id.prayerFragment);
        onView(withId(R.id.clock)).perform(reveal()).check(matches(isDisplayed()));
        onView(withId(R.id.refresh)).perform(reveal(), click());
        waitForSchedule(scenario);
        capture(
            expected == Configuration.ORIENTATION_PORTRAIT
                ? "portrait-timetable"
                : "landscape-timetable");
        scenario.onActivity(a -> a.openDestination(R.id.qiblaFragment, null));
        waitForDestination(scenario, R.id.qiblaFragment);
        onView(withId(R.id.locate)).perform(reveal()).check(matches(isDisplayed()));
        capture(
            expected == Configuration.ORIENTATION_PORTRAIT ? "portrait-qibla" : "landscape-qibla");
        scenario.onActivity(a -> a.openDestination(R.id.prayerFragment, null));
        waitForDestination(scenario, R.id.prayerFragment);
      }
    }
  }
}
