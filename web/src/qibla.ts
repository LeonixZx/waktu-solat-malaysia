export type QiblaQuality =
  | "acquiring_location"
  | "acquiring_heading"
  | "ready"
  | "low_confidence"
  | "stale"
  | "unavailable";
export type QiblaState = {
  bearing: number | null;
  heading: number | null;
  locationAt: number;
  headingAt: number;
  source: string;
  trueNorth: boolean;
  tilt: number | null;
  uncertainty: number | null;
  available: boolean;
};
export const normalize = (n: number) => ((n % 360) + 360) % 360;
export const angleDelta = (target: number, heading: number) =>
  normalize(target - heading + 180) - 180;
export function qiblaQuality(s: QiblaState, now: number): QiblaQuality {
  if (s.bearing === null || !Number.isFinite(s.bearing))
    return "acquiring_location";
  if (!s.available) return "unavailable";
  if (s.heading === null) return "acquiring_heading";
  if (
    now - s.locationAt > 120000 ||
    now - s.headingAt > 2000 ||
    now < s.locationAt ||
    now < s.headingAt
  )
    return "stale";
  // Browser orientation generally cannot certify its north reference or sensor accuracy.
  if (
    !s.trueNorth ||
    s.tilt === null ||
    s.tilt > 25 ||
    s.uncertainty === null ||
    s.uncertainty > 10
  )
    return "low_confidence";
  return "ready";
}
export function smoothHeading(
  previous: number | null,
  next: number,
  elapsed: number,
) {
  return previous === null || elapsed > 2000
    ? normalize(next)
    : normalize(
        previous +
          (1 - Math.exp(-Math.max(0, elapsed) / 150)) *
            angleDelta(next, previous),
      );
}
