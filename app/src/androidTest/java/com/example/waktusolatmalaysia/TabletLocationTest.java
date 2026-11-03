package com.example.waktusolatmalaysia;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.example.waktusolatmalaysia.views.MainActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Run separately after the normal UI suite, with location initially ungranted. */
@RunWith(AndroidJUnit4.class)
public class TabletLocationTest {
  @Test
  public void realPermissionDialogDenialAndApproximateGrant() throws Exception {
    var instrumentation = InstrumentationRegistry.getInstrumentation();
    Context context = instrumentation.getTargetContext();
    assumeTrue(
        "The real permission-dialog check needs an initially ungranted app",
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_DENIED);
    UiDevice device = UiDevice.getInstance(instrumentation);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(a -> a.openDestination(R.id.qiblaFragment, null));
      onView(withId(R.id.locate)).perform(TabletFeatureTest.reveal(), click());
      UiObject2 deny =
          device.wait(
              Until.findObject(By.res(Pattern.compile(".*:id/permission_deny_button"))), 10000);
      assertNotNull("Android location permission dialog should appear", deny);
      deny.click();
      device.wait(Until.gone(By.res(Pattern.compile(".*:id/permission_deny_button"))), 5000);
      onView(withId(R.id.coordinates)).check(matches(withText(R.string.location_denied)));
      TabletFeatureTest.capture("location-denied");

      onView(withId(R.id.locate)).perform(TabletFeatureTest.reveal(), click());
      UiObject2 allow =
          device.wait(
              Until.findObject(
                  By.res(Pattern.compile(".*:id/permission_allow_foreground_only_button"))),
              10000);
      assertNotNull("Android should allow permission retry after the first denial", allow);
      UiObject2 approximate =
          device.findObject(
              By.res(Pattern.compile(".*:id/permission_location_accuracy_radio_coarse")));
      if (approximate != null) approximate.click();
      allow.click();
      device.wait(
          Until.gone(By.res(Pattern.compile(".*:id/permission_allow_foreground_only_button"))),
          5000);
      assertEquals(
          PackageManager.PERMISSION_GRANTED,
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));

      long deadline = SystemClock.elapsedRealtime() + 25000;
      AtomicReference<String> message = new AtomicReference<>();
      do {
        scenario.onActivity(
            activity ->
                message.set(
                    ((TextView) activity.findViewById(R.id.coordinates)).getText().toString()));
        if (!context.getString(R.string.location_wait).equals(message.get())) break;
        SystemClock.sleep(100);
      } while (SystemClock.elapsedRealtime() < deadline);
      assertNotEquals(
          "Location lookup must complete or report failure",
          context.getString(R.string.location_wait),
          message.get());
      boolean fixReceived = message.get().startsWith("Lokasi:");
      assertTrue(
          "Result must be a real location or a clear provider error",
          fixReceived || context.getString(R.string.location_failed).equals(message.get()));
      onView(withId(R.id.locate)).check(matches(isEnabled()));
      scenario.recreate();
      if (fixReceived) onView(withId(R.id.coordinates)).check(matches(withText(message.get())));
      TabletFeatureTest.capture("location-result");
      JSONObject result =
          new JSONObject()
              .put("coarsePermissionGranted", true)
              .put(
                  "precisePermissionGranted",
                  ContextCompat.checkSelfPermission(
                          context, Manifest.permission.ACCESS_FINE_LOCATION)
                      == PackageManager.PERMISSION_GRANTED)
              .put("realLocationFixReceived", fixReceived);
      File directory = new File(context.getExternalFilesDir(null), "tablet-verification");
      try (FileOutputStream output =
          new FileOutputStream(new File(directory, "location-result.json"))) {
        output.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  @Test
  public void physicalCompassSensorProducesOrientationEvents() throws Exception {
    var instrumentation = InstrumentationRegistry.getInstrumentation();
    SensorManager manager =
        (SensorManager) instrumentation.getTargetContext().getSystemService(Context.SENSOR_SERVICE);
    Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    if (sensor == null) sensor = manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
    assumeTrue("Physical sensor check requires a compass-capable rotation sensor", sensor != null);
    CountDownLatch received = new CountDownLatch(1);
    AtomicReference<float[]> reading = new AtomicReference<>();
    SensorEventListener listener =
        new SensorEventListener() {
          @Override
          public void onSensorChanged(SensorEvent event) {
            reading.set(event.values.clone());
            received.countDown();
          }

          @Override
          public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    assertTrue(
        manager.registerListener(
            listener, sensor, SensorManager.SENSOR_DELAY_UI, new Handler(Looper.getMainLooper())));
    try {
      assertTrue("A real rotation event must arrive", received.await(5, TimeUnit.SECONDS));
      float[] matrix = new float[9];
      SensorManager.getRotationMatrixFromVector(matrix, reading.get());
      float[] orientation = SensorManager.getOrientation(matrix, new float[3]);
      assertFalse(Float.isNaN(orientation[0]));
      assertFalse(Float.isInfinite(orientation[0]));
    } finally {
      manager.unregisterListener(listener);
    }
  }
}
