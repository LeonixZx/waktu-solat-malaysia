package com.example.waktusolatmalaysia.utils;

public final class QiblaMath {
  private QiblaMath() {}

  /** Initial great-circle bearing to the Kaaba, clockwise from true north. */
  public static double bearing(double latitude, double longitude) {
    if (!Double.isFinite(latitude) || !Double.isFinite(longitude) || latitude < -90 || latitude > 90
        || longitude < -180 || longitude > 180) return Double.NaN;
    if (Math.abs(latitude - 21.422487) < 1e-7 && Math.abs(longitude - 39.826206) < 1e-7) return Double.NaN;
    double origin = Math.toRadians(latitude);
    double destination = Math.toRadians(21.422487);
    double delta = Math.toRadians(39.826206 - longitude);
    double y = Math.sin(delta) * Math.cos(destination);
    double x =
        Math.cos(origin) * Math.sin(destination)
            - Math.sin(origin) * Math.cos(destination) * Math.cos(delta);
    return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
  }
}
