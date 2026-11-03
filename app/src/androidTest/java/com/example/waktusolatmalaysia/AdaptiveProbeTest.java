package com.example.waktusolatmalaysia;

import static org.junit.Assert.*;

import android.view.*;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.waktusolatmalaysia.ui.Ui;
import com.example.waktusolatmalaysia.views.MainActivity;
import org.junit.*;
import org.junit.runner.RunWith;

/** Run with an emulator window size/font scale; this never changes device settings. */
@RunWith(AndroidJUnit4.class)
public class AdaptiveProbeTest {
  @Test
  public void essentialControlsStayInsideTheAvailableWindow() throws Exception {
    String label = InstrumentationRegistry.getArguments().getString("capture", "adaptive");
    try (var scenario = ActivityScenario.launch(MainActivity.class)) {
      TabletFeatureTest.waitForSchedule(scenario);
      scenario.onActivity(
          a -> {
            View root = a.findViewById(R.id.main_surface);
            androidx.core.graphics.Insets bars = androidx.core.view.ViewCompat.getRootWindowInsets(root)
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            int height = Math.round((root.getHeight() - bars.top - bars.bottom)
                / a.getResources().getDisplayMetrics().density);
            int width =
                Math.round(
                    (root.getWidth() - bars.left - bars.right)
                        / a.getResources().getDisplayMetrics().density);
            boolean compact = width < 600 || height < 480;
            assertEquals(
                compact ? View.VISIBLE : View.GONE,
                a.findViewById(R.id.bottom_nav_shell).getVisibility());
            assertEquals(
                !compact && width < 1200 ? View.VISIBLE : View.GONE,
                a.findViewById(R.id.navigation_rail).getVisibility());
            assertEquals(
                !compact && width >= 1200 ? View.VISIBLE : View.GONE,
                a.findViewById(R.id.permanent_sidebar).getVisibility());
            assertNull("Top-level hubs must not expose a second menu", ((androidx.appcompat.widget.Toolbar)
                a.findViewById(R.id.toolbar)).getNavigationIcon());
            assertTextFits(a.findViewById(R.id.next_time));
            assertTextFits(a.findViewById(R.id.countdown));
            View refresh = a.findViewById(R.id.refresh);
            assertTrue(refresh.getHeight() >= Ui.dp(a, 48));
          });
      TabletFeatureTest.capture(label + "-home");
      for (int destination :
          new int[] {
            R.id.worshipFragment,
            R.id.quranFragment,
            R.id.collectionFragment,
            R.id.tasksFragment,
            R.id.calendarFragment,
            R.id.settingsFragment,
            R.id.exploreFragment
          }) {
        V3FeaturesTest.open(scenario, destination);
        android.os.SystemClock.sleep(250);
        scenario.onActivity(
            a -> {
              View host = a.findViewById(R.id.nav_host);
              assertTrue(host.getWidth() > 0 && host.getHeight() > 0);
            });
      }
      TabletFeatureTest.capture(label + "-explore");
    }
  }

  private static void assertTextFits(TextView view) {
    assertNotNull(view);
    assertNotNull(view.getLayout());
    int content = view.getHeight() - view.getCompoundPaddingTop() - view.getCompoundPaddingBottom();
    assertTrue(
        view.getText() + " is vertically clipped", view.getLayout().getHeight() <= content + 1);
    for (int line = 0; line < view.getLineCount(); line++)
      assertEquals("Essential text is ellipsized", 0, view.getLayout().getEllipsisCount(line));
  }
}
