package com.example.waktusolatmalaysia.utils;

import android.hardware.SensorManager;
import android.view.Surface;

public final class QiblaOrientation {
  private QiblaOrientation() {}
  /** Sensor axes are tied to natural orientation; map them to the current screen. */
  public static boolean remap(float[] matrix, int rotation, float[] output) {
    int x = SensorManager.AXIS_X, y = SensorManager.AXIS_Y;
    if (rotation == Surface.ROTATION_90) { x = SensorManager.AXIS_Y; y = SensorManager.AXIS_MINUS_X; }
    else if (rotation == Surface.ROTATION_180) { x = SensorManager.AXIS_MINUS_X; y = SensorManager.AXIS_MINUS_Y; }
    else if (rotation == Surface.ROTATION_270) { x = SensorManager.AXIS_MINUS_Y; y = SensorManager.AXIS_X; }
    return SensorManager.remapCoordinateSystem(matrix, x, y, output);
  }
}
