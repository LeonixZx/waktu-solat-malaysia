package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.preferences.AppPreferences;
import com.example.waktusolatmalaysia.repo.PrayerRepository;
import com.example.waktusolatmalaysia.repo.ZoneRepository;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NativeAppSmokeTest {
  private void capture(String name) {
    try {
      var instrumentation = InstrumentationRegistry.getInstrumentation();
      instrumentation.waitForIdleSync();
      Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
      if (bitmap == null) return;
      File file =
          new File(instrumentation.getTargetContext().getExternalFilesDir(null), name + ".png");
      try (FileOutputStream output = new FileOutputStream(file)) {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
      }
      bitmap.recycle();
    } catch (Exception failure) {
      throw new AssertionError("Cannot capture verification screen", failure);
    }
  }

  @Test
  public void homeToolsAndSecondaryScreensRemainReachable() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      onView(withId(R.id.clock)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
      onView(withId(R.id.location_button)).perform(TabletFeatureTest.reveal(), click());
      onView(withId(R.id.state)).check(matches(isDisplayed()));
      capture("zone-screen");
      onView(withId(R.id.save)).perform(click());
      onView(withId(R.id.clock)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
      scenario.onActivity(a -> a.openDestination(R.id.qiblaFragment, null));
      onView(withId(R.id.compass)).check(matches(isDisplayed()));
      capture("qibla-screen");
      scenario.onActivity(a -> a.openDestination(R.id.settingsFragment, null));
      onView(withId(R.id.format_switch)).check(matches(isDisplayed()));
      capture("preferences-screen");
      scenario.onActivity(a -> a.openDestination(R.id.aboutFragment, null));
      onView(withText("Versi " + BuildConfig.VERSION_NAME)).check(matches(isDisplayed()));
      capture("about-screen");
    }
  }

  @Test
  public void officialZoneCatalogHasSixtyUniqueZones() {
    var zones =
        ZoneRepository.load(InstrumentationRegistry.getInstrumentation().getTargetContext());
    HashSet<String> codes = new HashSet<>();
    for (var state : zones.values())
      for (var zone : state) {
        assertFalse(zone.name.isEmpty());
        assertTrue("Zone codes must be unique", codes.add(zone.code));
      }
    assertEquals(60, codes.size());
    assertTrue(codes.contains("WLY01"));
    assertTrue(codes.contains("WLY02"));
    assertTrue(codes.contains("SGR01"));
  }

  @Test
  public void settingsPersistAcrossActivityRecreation() {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    AppPreferences preferences = new AppPreferences(context);
    boolean original = preferences.isTwelveHour();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.openDestination(R.id.settingsFragment, null));
      onView(withId(R.id.format_switch)).perform(click());
      assertEquals(!original, preferences.isTwelveHour());
      scenario.recreate();
      onView(withId(R.id.format_switch)).check(matches(original ? isNotChecked() : isChecked()));
    } finally {
      preferences.setTwelveHour(original);
    }
  }

  @Test
  public void bottomNavigationOpensQiblaAndPreferences() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.openDestination(R.id.qiblaFragment, null));
      onView(withId(R.id.compass)).check(matches(isDisplayed()));
      scenario.onActivity(a -> a.openDestination(R.id.settingsFragment, null));
      onView(withId(R.id.format_switch)).check(matches(isDisplayed()));
      scenario.onActivity(a -> a.openDestination(R.id.prayerFragment, null));
      onView(withId(R.id.clock)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
    }
  }

  @Test
  public void nightModeSurvivesRecreation() {
    var instrumentation = InstrumentationRegistry.getInstrumentation();
    AppPreferences preferences = new AppPreferences(instrumentation.getTargetContext());
    boolean original = preferences.isDark();
    String originalTheme = preferences.theme();
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.openDestination(R.id.settingsFragment, null));
      scenario.onActivity(a -> ((com.example.waktusolatmalaysia.ui.ChoiceBar)a.findViewById(R.id.theme_choices)).getChildAt(original ? 1 : 2).performClick());
      assertEquals(!original, preferences.isDark());
      scenario.recreate();
      scenario.onActivity(a -> assertEquals(original ? 1 : 2,((com.example.waktusolatmalaysia.ui.ChoiceBar)a.findViewById(R.id.theme_choices)).selected()));
      capture("night-preferences-screen");
    } finally {
      preferences.setTheme(originalTheme);
      instrumentation.runOnMainSync(
          () ->
              AppCompatDelegate.setDefaultNightMode(
                  originalTheme.equals("system")
                      ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                      : originalTheme.equals("dark")
                          ? AppCompatDelegate.MODE_NIGHT_YES
                          : AppCompatDelegate.MODE_NIGHT_NO));
    }
  }

  /** Integration check: requires access to the public JAKIM endpoint. */
  @Test
  public void officialTimetableDownloadsAndCanBeReadFromDiskOffline() throws Exception {
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
    YearMonth month = YearMonth.from(today);
    PrayerRepository.Month live = new PrayerRepository(context).month("WLY01", month, true);
    assertFalse("The live integration check must reach JAKIM", live.offline);
    assertNotNull(live.days.get(today));
    assertEquals(8, live.days.get(today).times.size());
    PrayerRepository.Month cached = new PrayerRepository(context).month("WLY01", month, false);
    assertTrue(cached.cached);
    assertEquals(live.days.get(today).times, cached.days.get(today).times);
  }
}
