import { it, expect } from "vitest";
import { qiblaQuality, angleDelta, smoothHeading } from "./qibla";
import type { QiblaState } from "./qibla";
import { qibla } from "./core";
const ready: QiblaState = {
  bearing: 292.5,
  heading: 292.5,
  locationAt: 10000,
  headingAt: 10000,
  source: "fixture",
  trueNorth: true,
  tilt: 0,
  uncertainty: 2,
  available: true,
};
it("does not certify an unknown browser north reference", () => {
  expect(qiblaQuality({ ...ready, trueNorth: false }, 10000)).toBe(
    "low_confidence",
  );
  expect(qiblaQuality({ ...ready, uncertainty: null }, 10000)).toBe(
    "low_confidence",
  );
  expect(qiblaQuality(ready, 10000)).toBe("ready");
  expect(qiblaQuality(ready, 12001)).toBe("stale");
  expect(qiblaQuality({ ...ready, tilt: 26 }, 10000)).toBe("low_confidence");
  expect(qiblaQuality({ ...ready, available: false }, 10000)).toBe(
    "unavailable",
  );
});
it("wraps headings smoothly and rejects invalid bearings", () => {
  expect(angleDelta(1, 359)).toBe(2);
  expect(Math.abs(angleDelta(smoothHeading(359, 1, 150), 0))).toBeLessThan(1);
  expect(qibla(3.139, 101.6869)).toBeCloseTo(292.5, 0);
  expect(qibla(100, 101)).toBeNaN();
  expect(qibla(21.422487, 39.826206)).toBeNaN();
});
