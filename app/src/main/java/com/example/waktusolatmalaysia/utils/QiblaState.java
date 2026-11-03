package com.example.waktusolatmalaysia.utils;

/** Quality gates are application guidance, not a certification of physical compass accuracy. */
public final class QiblaState {
  public enum Quality { ACQUIRING_LOCATION, ACQUIRING_HEADING, READY, LOW_CONFIDENCE, STALE, UNAVAILABLE }
  public double bearing = Double.NaN, heading = Double.NaN, declination, tilt, uncertainty = Double.NaN;
  public long locationAt = -1, headingAt = -1;
  public int accuracy;
  public boolean available;
  public String source = "unavailable", provider = "";
  public double locationAccuracy;
  private long previousAt = -1;
  private double filteredMagnetic = Double.NaN;

  public void location(double latitude, double longitude, double declination, long at, double accuracy, String provider) {
    bearing = QiblaMath.bearing(latitude, longitude);
    this.declination = declination; locationAt = at; locationAccuracy = accuracy; this.provider = provider;
    if (Double.isFinite(filteredMagnetic)) heading = normalize(filteredMagnetic + declination);
  }
  public void sample(double magneticHeading, double tilt, double uncertainty, int accuracy, long at) {
    if (!Double.isFinite(magneticHeading) || !Double.isFinite(tilt)) return;
    double alpha = previousAt < 0 || at - previousAt > 2000 ? 1 : 1 - Math.exp(-Math.max(0, at - previousAt) / 150.0);
    filteredMagnetic = Double.isFinite(filteredMagnetic)
        ? normalize(filteredMagnetic + alpha * delta(magneticHeading, filteredMagnetic)) : normalize(magneticHeading);
    previousAt = at; headingAt = at; this.tilt = Math.abs(tilt); this.uncertainty = uncertainty; this.accuracy = accuracy;
    heading = normalize(filteredMagnetic + declination);
  }
  public void resetHeading() { headingAt = previousAt = -1; heading = filteredMagnetic = Double.NaN; accuracy = 0; }
  public Quality quality(long now) {
    if (!Double.isFinite(bearing) || locationAt < 0) return Quality.ACQUIRING_LOCATION;
    if (!available) return Quality.UNAVAILABLE;
    if (headingAt < 0 || !Double.isFinite(heading)) return Quality.ACQUIRING_HEADING;
    if (now < locationAt || now - locationAt > 120000 || now < headingAt || now - headingAt > 2000) return Quality.STALE;
    if (accuracy < 2 || tilt > 25 || (Double.isFinite(uncertainty) && uncertainty > 10)) return Quality.LOW_CONFIDENCE;
    return Quality.READY;
  }
  public boolean aligned(long now) { return quality(now) == Quality.READY && Math.abs(delta(bearing, heading)) <= 5; }
  public static double normalize(double degrees) { return (degrees % 360 + 360) % 360; }
  public static double delta(double target, double heading) { return normalize(target - heading + 180) - 180; }
  public static long locationAge(long nowNanos,long fixNanos,long nowMillis,long fixMillis) {
    if (fixNanos>0 && nowNanos>=fixNanos) return (nowNanos-fixNanos)/1000000;
    // Monotonic timestamps from a previous boot must not become fresh after reboot.
    return fixMillis>0 && nowMillis>=fixMillis ? nowMillis-fixMillis : Long.MAX_VALUE;
  }
}
