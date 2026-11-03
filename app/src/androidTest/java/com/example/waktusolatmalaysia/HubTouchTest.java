package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;

import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HubTouchTest {
  @Test
  public void actualHubTapsOpenEachScreenAndReturnHome() {
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      AtomicInteger surface = new AtomicInteger();
      s.onActivity(
          a ->
              surface.set(
                  a.findViewById(R.id.navigation_rail).getVisibility() == View.VISIBLE
                      ? R.id.navigation_rail
                      : a.findViewById(R.id.permanent_sidebar).getVisibility() == View.VISIBLE
                      ? R.id.permanent_navigation
                      : R.id.bottom_nav));
      for (int id :
          new int[] {
            R.id.quranFragment, R.id.worshipFragment, R.id.exploreFragment, R.id.savedFragment
          }) {
        onView(allOf(withId(id), isDescendantOfA(withId(surface.get())))).perform(click());
        s.onActivity(
            a -> {
              var host =
                  (androidx.navigation.fragment.NavHostFragment)
                      a.getSupportFragmentManager().findFragmentById(R.id.nav_host);
              org.junit.Assert.assertEquals(
                  id, host.getNavController().getCurrentDestination().getId());
            });
        onView(allOf(withId(R.id.prayerFragment), isDescendantOfA(withId(surface.get()))))
            .perform(click());
        onView(withId(R.id.clock)).check(matches(isDisplayed()));
      }
    }
  }

  @Test
  public void singleNavigationAndSettingsEntrySurviveResizing() {
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      AtomicInteger settings = new AtomicInteger();
      s.onActivity(a -> {
        int visible = 0;
        for (int id : new int[] {R.id.bottom_nav_shell, R.id.navigation_rail_shell, R.id.permanent_sidebar})
          if (a.findViewById(id).getVisibility() == View.VISIBLE) visible++;
        org.junit.Assert.assertEquals("Exactly one primary menu", 1, visible);
        org.junit.Assert.assertNull(((androidx.appcompat.widget.Toolbar) a.findViewById(R.id.toolbar)).getNavigationIcon());
        settings.set(a.findViewById(R.id.navigation_rail_shell).getVisibility() == View.VISIBLE
            ? R.id.rail_settings : a.findViewById(R.id.permanent_sidebar).getVisibility() == View.VISIBLE
            ? R.id.sidebar_settings : R.id.action_settings);
      });
      onView(withId(settings.get())).perform(click());
      onView(withId(R.id.format_switch)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
      s.onActivity(HubTouchTest::assertOnlySettingsSelected);
      s.onActivity(a -> {
        org.junit.Assert.assertNotNull("Detail screens retain Up navigation", ((androidx.appcompat.widget.Toolbar)
            a.findViewById(R.id.toolbar)).getNavigationIcon());
        org.junit.Assert.assertTrue(a.onSupportNavigateUp());
      });
      onView(withId(R.id.clock)).perform(TabletFeatureTest.reveal()).check(matches(isDisplayed()));
    }
  }

  private static android.view.Menu[] menus(MainActivity a) {
    return new android.view.Menu[] {
      ((com.google.android.material.bottomnavigation.BottomNavigationView) a.findViewById(R.id.bottom_nav)).getMenu(),
      ((com.google.android.material.navigationrail.NavigationRailView) a.findViewById(R.id.navigation_rail)).getMenu(),
      ((com.google.android.material.navigation.NavigationView) a.findViewById(R.id.permanent_navigation)).getMenu()
    };
  }

  private static void assertOnlySettingsSelected(MainActivity a) {
    for (android.view.Menu menu : menus(a))
      for (int i = 0; i < menu.size(); i++)
        org.junit.Assert.assertFalse("Settings must not select a hub: " + menu.getItem(i).getTitle(), menu.getItem(i).isChecked());
    org.junit.Assert.assertTrue(a.findViewById(R.id.rail_settings).isSelected());
    org.junit.Assert.assertTrue(a.findViewById(R.id.sidebar_settings).isSelected());
  }

  @Test
  public void savedAndSettingsHaveIndependentSelectionAfterRecreationAndBack() throws Exception {
    try (var s = ActivityScenario.launch(MainActivity.class)) {
      AtomicInteger surface = new AtomicInteger(), settings = new AtomicInteger();
      s.onActivity(a -> {
        boolean rail = a.findViewById(R.id.navigation_rail_shell).getVisibility() == View.VISIBLE;
        boolean sidebar = a.findViewById(R.id.permanent_sidebar).getVisibility() == View.VISIBLE;
        surface.set(rail ? R.id.navigation_rail : sidebar ? R.id.permanent_navigation : R.id.bottom_nav);
        settings.set(rail ? R.id.rail_settings : sidebar ? R.id.sidebar_settings : R.id.action_settings);
      });
      onView(allOf(withId(R.id.savedFragment), isDescendantOfA(withId(surface.get())))).perform(click());
      s.onActivity(a -> {
        for (android.view.Menu menu : menus(a)) org.junit.Assert.assertTrue(menu.findItem(R.id.savedFragment).isChecked());
      });
      onView(withId(settings.get())).perform(click());
      s.onActivity(HubTouchTest::assertOnlySettingsSelected);
      s.recreate();
      s.onActivity(HubTouchTest::assertOnlySettingsSelected);
      TabletFeatureTest.capture("settings-selection");
      s.onActivity(a -> org.junit.Assert.assertTrue(a.onSupportNavigateUp()));
      s.onActivity(a -> {
        for (android.view.Menu menu : menus(a)) org.junit.Assert.assertTrue(menu.findItem(R.id.savedFragment).isChecked());
        org.junit.Assert.assertFalse(a.findViewById(R.id.rail_settings).isSelected());
        org.junit.Assert.assertFalse(a.findViewById(R.id.sidebar_settings).isSelected());
      });
      // The other Settings-owned screens also remain outside Saved.
      for (int id : new int[] {R.id.backupFragment, R.id.aboutFragment}) {
        s.onActivity(a -> a.openDestination(id, null));
        s.onActivity(HubTouchTest::assertOnlySettingsSelected);
        s.onActivity(a -> org.junit.Assert.assertTrue(a.onSupportNavigateUp()));
      }
    }
  }
}
