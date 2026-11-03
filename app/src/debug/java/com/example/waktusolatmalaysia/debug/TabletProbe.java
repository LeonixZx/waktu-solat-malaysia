package com.example.waktusolatmalaysia.debug;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import androidx.navigation.fragment.NavHostFragment;
import com.example.waktusolatmalaysia.R;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Local debug-only checks packed with the existing app to avoid a second APK install. */
public final class TabletProbe extends Instrumentation {
  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    start();
  }

  private int destination(MainActivity activity) {
    AtomicInteger id = new AtomicInteger();
    runOnMainSync(
        () ->
            id.set(
                ((NavHostFragment)
                        activity.getSupportFragmentManager().findFragmentById(R.id.nav_host))
                    .getNavController()
                    .getCurrentDestination()
                    .getId()));
    return id.get();
  }

  private void tap(MainActivity activity, int id, boolean direct) {
    AtomicReference<View> target = new AtomicReference<>();
    int[] point = new int[2];
    runOnMainSync(
        () -> {
          View rail = activity.findViewById(R.id.navigation_rail);
          View nav =
              rail.getVisibility() == View.VISIBLE ? rail
                  : activity.findViewById(R.id.permanent_sidebar).getVisibility() == View.VISIBLE
                  ? activity.findViewById(R.id.permanent_navigation) : activity.findViewById(R.id.bottom_nav);
          View v = nav.findViewById(id);
          target.set(v);
          v.getLocationOnScreen(point);
          point[0] += v.getWidth() / 2;
          point[1] += v.getHeight() / 2;
        });
    long now = SystemClock.uptimeMillis();
    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, point[0], point[1], 0),
        up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, point[0], point[1], 0);
    down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
    try {
      if (direct)
        runOnMainSync(
            () -> {
              activity.getWindow().getDecorView().dispatchTouchEvent(down);
              activity.getWindow().getDecorView().dispatchTouchEvent(up);
            });
      else {
        sendPointerSync(down);
        SystemClock.sleep(80);
        sendPointerSync(up);
      }
    } finally {
      down.recycle();
      up.recycle();
    }
    waitForIdleSync();
    SystemClock.sleep(500);
  }

  private void capture(String name) throws Exception {
    android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
    if (bitmap == null) return;
    try (var out =
        new FileOutputStream(
            new File(getTargetContext().getExternalFilesDir(null), name + ".png"))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    JSONObject report = new JSONObject();
    try {
      MainActivity activity =
          (MainActivity)
              startActivitySync(
                  new Intent(getTargetContext(), MainActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
      waitForIdleSync();
      SystemClock.sleep(1000);
      boolean all = true;
      for (int id :
          new int[] {
            R.id.quranFragment, R.id.worshipFragment, R.id.exploreFragment, R.id.savedFragment
          }) {
        String name = getTargetContext().getResources().getResourceEntryName(id);
        tap(activity, id, false);
        boolean os = destination(activity) == id;
        if (!os) tap(activity, id, true);
        boolean direct = destination(activity) == id;
        all &= direct;
        report.put(
            name, new JSONObject().put("systemInjectedTouch", os).put("windowTouch", direct));
        capture("tablet-probe-" + name);
        tap(activity, R.id.prayerFragment, !os);
        if (destination(activity) != R.id.prayerFragment)
          runOnMainSync(() -> activity.openDestination(R.id.prayerFragment, null));
      }
      report.put("allHubsReachable", all);
      AtomicInteger frames = new AtomicInteger(), slow = new AtomicInteger();
      AtomicLong longest = new AtomicLong();
      float refresh = activity.getWindowManager().getDefaultDisplay().getRefreshRate();
      long budget = (long) (1_000_000_000d / refresh);
      Window.OnFrameMetricsAvailableListener metrics =
          (window, frame, dropped) -> {
            long duration = frame.getMetric(FrameMetrics.TOTAL_DURATION);
            long deadline =
                Build.VERSION.SDK_INT >= 31 ? frame.getMetric(FrameMetrics.DEADLINE) : budget;
            frames.incrementAndGet();
            if (duration > (deadline > 0 ? deadline : budget)) slow.incrementAndGet();
            longest.accumulateAndGet(duration, Math::max);
          };
      runOnMainSync(
          () -> {
            activity
                .getWindow()
                .addOnFrameMetricsAvailableListener(metrics, new Handler(Looper.getMainLooper()));
            activity.openDestination(R.id.quranFragment, null);
          });
      waitForIdleSync();
      SystemClock.sleep(400);
      for (int i = 0; i < 3; i++) {
        runOnMainSync(
            () -> {
              androidx.recyclerview.widget.RecyclerView list =
                  activity.findViewById(R.id.quran_list);
              list.smoothScrollBy(0, 800);
            });
        SystemClock.sleep(600);
      }
      runOnMainSync(
          () -> {
            activity.getWindow().removeOnFrameMetricsAvailableListener(metrics);
            activity.openDestination(R.id.prayerFragment, null);
          });
      waitForIdleSync();
      SystemClock.sleep(400);
      android.os.Debug.MemoryInfo memory = new android.os.Debug.MemoryInfo();
      android.os.Debug.getMemoryInfo(memory);
      report.put(
          "performance",
          new JSONObject()
              .put("build", "debug")
              .put("refreshHz", refresh)
              .put("sampledFrames", frames.get())
              .put("overDeadlineFrames", slow.get())
              .put("longestFrameMs", longest.get() / 1_000_000d)
              .put("nativeHeapBytes", android.os.Debug.getNativeHeapAllocatedSize())
              .put("totalPssKb", memory.getTotalPss()));
      capture("tablet-probe-home");
      try (var out =
          new FileOutputStream(
              new File(getTargetContext().getExternalFilesDir(null), "tablet-probe.json"))) {
        out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
      }
      result.putString("report", report.toString());
      finish(all ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    } catch (Throwable failure) {
      result.putString("failure", failure.toString());
      finish(Activity.RESULT_CANCELED, result);
    }
  }
}
