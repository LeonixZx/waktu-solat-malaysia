import "fake-indexeddb/auto";
import { describe, it, expect } from "vitest";
import {
  parsePrayers,
  nextPrayer,
  nextTask,
  addDays,
  defaults,
  mergeBackup,
  validateBackup,
  calendarExport,
  qibla,
} from "./core";
import { parseVerses } from "./data";
import prayers from "../../shared/fixtures/prayers-wly01-2026-11.json";
import quran from "../../shared/fixtures/quran-1.json";
import { route } from "../worker/index";
import type { PrayerRelativeReminder, BackupBundle } from "./types";
const days = parsePrayers(prayers, "WLY01", "2026-11");
const task: PrayerRelativeReminder = {
  id: "cross-midnight",
  title: "Reading",
  anchor: "isha",
  offsetMinutes: 360,
  weekdays: [1],
  enabled: true,
};
describe("shared prayer and content contracts", () => {
  it("validates official monthly times", () => {
    expect(days).toHaveLength(30);
    expect(days[1].times).toHaveLength(8);
    expect(() => parsePrayers(prayers, "SGR01", "2026-11")).toThrow();
    expect(() => parsePrayers(prayers, "WLY01", "2027-01")).toThrow();
  });
  it("uses Malaysia instants and tomorrow after Isha", () => {
    const now = new Date("2026-11-02T23:00:00+08:00"),
      next = nextPrayer(
        days.filter((d) => d.date >= "2026-11-02"),
        now,
      );
    expect(next?.index).toBe(1);
    expect(next?.day.date).toBe("2026-11-03");
  });
  it("retains the anchor weekday when task crosses midnight", () => {
    const next = nextTask(task, days, new Date("2026-11-02T21:00:00+08:00"));
    expect(next?.anchorDate).toBe("2026-11-02");
    expect(next?.time.toISOString()).toBe(
      new Date("2026-11-03T02:09:00+08:00").toISOString(),
    );
  });
  it("selects future occurrences and respects disabled tasks", () => {
    expect(nextTask({ ...task, enabled: false }, days)).toBeNull();
    expect(nextTask(task, [], new Date())).toBeNull();
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
  });
  it("keeps Arabic, Malay, English and audio aligned", () => {
    expect(parseVerses(quran, 1, "en")).toHaveLength(7);
    expect(parseVerses(quran, 1, "ms")[0].meaning).toContain("Allah");
    expect(() => parseVerses(quran, 2, "ms")).toThrow();
    const bad = structuredClone(quran);
    bad.data[1].ayahs[0].number = 99;
    expect(() => parseVerses(bad, 1, "ms")).toThrow();
  });
  it("exports interoperable UTC calendar events", () => {
    const text = calendarExport([days[1]], "en");
    expect(text.split("BEGIN:VEVENT")).toHaveLength(6);
    expect(text).toContain("\r\n");
    expect(text).toMatch(/DTSTART:\d{8}T\d{6}Z/);
    expect(text).not.toContain(".000Z");
  });
  it("calculates Qibla for Kuala Lumpur", () =>
    expect(qibla(3.139, 101.6869)).toBeCloseTo(292.5, 0));
});
const incoming: BackupBundle = {
  ...structuredClone(defaults),
  schemaVersion: 1,
  exportedAt: new Date().toISOString(),
  bookmarks: ["surah:1", "ayah:1:2"],
  reminders: [task],
};
describe("backup validation", () => {
  it("merges stable IDs and disables imported tasks", () => {
    const result = mergeBackup(
      { ...defaults, bookmarks: ["surah:2"] },
      incoming,
      true,
    );
    expect(result.bookmarks.sort()).toEqual(["ayah:1:2", "surah:1", "surah:2"]);
    expect(result.reminders[0].enabled).toBe(false);
  });
  it("rejects private/system fields and unsupported version", () => {
    expect(() => validateBackup({ ...incoming, schemaVersion: 2 })).toThrow();
    expect(() =>
      validateBackup({ ...incoming, tracker: { alarm_generation: "x" } }),
    ).toThrow();
    expect(() =>
      validateBackup({
        ...incoming,
        reminders: [{ ...task, offsetMinutes: 361 }],
      }),
    ).toThrow();
  });
});
describe("allowlisted API adapter", () => {
  const env = { ASSETS: { fetch: async () => new Response("{}") } };
  it("rejects methods and invalid paths before any provider call", async () => {
    expect(
      (await route(new Request("https://example.org/api/quran/0"), env)).status,
    ).toBe(404);
    expect(
      (
        await route(
          new Request(
            "https://example.org/api/prayers?zone=../../&month=2026-11",
          ),
          env,
        )
      ).status,
    ).toBe(400);
    expect(
      (
        await route(
          new Request("https://example.org/api/quran/1", { method: "POST" }),
          env,
        )
      ).status,
    ).toBe(405);
    expect(
      (
        await route(
          new Request("https://example.org/api/proxy?url=https://example.com"),
          env,
        )
      ).status,
    ).toBe(404);
  });
});
