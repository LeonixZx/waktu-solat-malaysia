import { keys, obligatory } from "./types";
import type {
  PrayerSnapshot,
  PrayerRelativeReminder,
  BackupBundle,
  Personal,
} from "./types";

export const zoneTime = "Asia/Kuala_Lumpur";
export const names = {
  ms: [
    "Imsak",
    "Subuh",
    "Syuruk",
    "Dhuha",
    "Zohor",
    "Asar",
    "Maghrib",
    "Isyak",
  ],
  en: ["Imsak", "Fajr", "Sunrise", "Duha", "Dhuhr", "Asr", "Maghrib", "Isha"],
};

export function malaysiaDate(now = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: zoneTime,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}

export function addDays(date: string, n: number) {
  return new Date(Date.parse(date + "T12:00:00+08:00") + n * 86400000)
    .toISOString()
    .slice(0, 10);
}

export function at(date: string, time: string) {
  return new Date(
    date + "T" + (time.length === 5 ? time + ":00" : time) + "+08:00",
  );
}

export function nextPrayer(days: PrayerSnapshot[], now = new Date()) {
  return (
    days
      .flatMap((day) =>
        obligatory.map((index) => ({
          index,
          time: at(day.date, day.times[index]),
          day,
        })),
      )
      .filter((p) => p.time.getTime() > now.getTime())
      .sort((a, b) => +a.time - +b.time)[0] ?? null
  );
}

export function nextTask(
  task: PrayerRelativeReminder,
  days: PrayerSnapshot[],
  now = new Date(),
) {
  if (!task.enabled) return null;
  return (
    days
      .filter((d) =>
        task.weekdays.includes(
          ((new Date(d.date + "T12:00:00+08:00").getUTCDay() + 6) % 7) + 1,
        ),
      )
      .map((d) => ({
        anchorDate: d.date,
        time: new Date(
          +at(d.date, d.times[keys.indexOf(task.anchor)]) +
            task.offsetMinutes * 60000,
        ),
      }))
      .filter((d) => +d.time > +now)
      .sort((a, b) => +a.time - +b.time)[0] ?? null
  );
}

export function parsePrayers(
  raw: unknown,
  zone: string,
  month: string,
  retrievedAt = Date.now(),
  freshness: PrayerSnapshot["freshness"] = "live",
): PrayerSnapshot[] {
  const root = raw as { zone?: string; prayerTime?: Record<string, string>[] };
  if (
    (root.zone && root.zone !== zone) ||
    !Array.isArray(root.prayerTime) ||
    !root.prayerTime.length
  )
    throw Error("Invalid prayer timetable");
  const months = [
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
  ];
  const dates = new Set<string>();
  return root.prayerTime.map((row) => {
    const parts = /^(\d{2})-([A-Za-z]{3})-(\d{4})$/.exec(row.date);
    if (!parts) throw Error("Invalid prayer date");
    const date = `${parts[3]}-${String(months.indexOf(parts[2]) + 1).padStart(2, "0")}-${parts[1]}`;
    if (
      !date.startsWith(month + "-") ||
      dates.has(date) ||
      months.indexOf(parts[2]) < 0
    )
      throw Error("Wrong prayer month");
    dates.add(date);
    const times = keys.map((k) => row[k]);
    if (times.some((t) => !/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(t)))
      throw Error("Invalid prayer time");
    return {
      zone,
      date,
      timeZone: zoneTime,
      times,
      hijri: row.hijri ?? "",
      source: "JAKIM e-Solat",
      retrievedAt,
      freshness,
    };
  });
}

export function qibla(lat: number, lon: number) {
  if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) return NaN;
  if (Math.abs(lat-21.422487)<1e-7 && Math.abs(lon-39.826206)<1e-7) return NaN;
  const rad = Math.PI / 180,
    a = lat * rad,
    b = 21.422487 * rad,
    d = (39.826206 - lon) * rad;
  return (
    (Math.atan2(
      Math.sin(d),
      Math.cos(a) * Math.tan(b) - Math.sin(a) * Math.cos(d),
    ) /
      rad +
      360) %
    360
  );
}

export const defaults: Personal = {
  preferences: {
    zone_code: "WLY01",
    zone_state: "Wilayah Persekutuan",
    zone_name: "Kuala Lumpur, Putrajaya",
    language: "ms",
    theme: "light",
    dark: false,
    twelve_hour: false,
    arabic_size: 30,
    show_translation: true,
  },
  bookmarks: [],
  reading: { surah: 1, verse: 1, name: "Al-Fatihah" },
  tracker: {},
  reminders: [],
};

export function validateBackup(value: unknown): BackupBundle {
  const d = value as BackupBundle;
  if (
    !d ||
    d.schemaVersion !== 1 ||
    !d.preferences ||
    !d.reading ||
    !d.tracker ||
    !Array.isArray(d.bookmarks) ||
    !Array.isArray(d.reminders) ||
    d.bookmarks.length > 20000 ||
    d.reminders.length > 1000
  )
    throw Error("Unsupported backup");
  if (
    d.bookmarks.some(
      (id) =>
        typeof id !== "string" ||
        !/^(surah:[1-9]\d{0,2}|ayah:[1-9]\d{0,2}:[1-9]\d{0,2}|(doa|wirid|niat|hadith):\d+|catalog:[A-Za-z0-9_-]+)$/.test(
          id,
        ),
    )
  )
    throw Error("Invalid bookmark");
  if (
    !Number.isInteger(d.reading.surah) ||
    d.reading.surah < 1 ||
    d.reading.surah > 114 ||
    !Number.isInteger(d.reading.verse) ||
    d.reading.verse < 1 ||
    d.reading.verse > 286
  )
    throw Error("Invalid reading");
  const p = d.preferences;
  if (
    (p.zone_code && !/^[A-Z]{3}\d{2}$/.test(p.zone_code)) ||
    (p.language && !["ms", "en"].includes(p.language)) ||
    (p.theme && !["light", "dark", "system"].includes(p.theme)) ||
    (p.arabic_size && (p.arabic_size < 20 || p.arabic_size > 60))
  )
    throw Error("Invalid preferences");
  for (const id of d.bookmarks)
    if (id.startsWith("surah:") || id.startsWith("ayah:")) {
      const [, n, v] = id.split(":");
      if (+n > 114 || (v && +v > 286)) throw Error("Invalid bookmark range");
    }
  for (const key of ["twelve_hour", "dark", "show_translation"] as const)
    if (key in p && typeof p[key] !== "boolean") throw Error("Invalid toggle");
  for (const t of d.reminders)
    if (
      !/^[A-Za-z0-9_-]{1,80}$/.test(t.id) ||
      !t.title?.trim() ||
      t.title.length > 80 ||
      !keys.includes(t.anchor) ||
      !Number.isInteger(t.offsetMinutes) ||
      Math.abs(t.offsetMinutes) > 360 ||
      !t.weekdays?.length ||
      t.weekdays.some((n) => !Number.isInteger(n) || n < 1 || n > 7) ||
      typeof t.enabled !== "boolean"
    )
      throw Error("Invalid reminder");
  for (const [k, v] of Object.entries(d.tracker)) {
    if (
      !/^(prayed_\d{4}-\d{2}-\d{2}_.+|fasted_\d{4}-\d{2}-\d{2}|tasbih_count|tasbih_goal)$/.test(
        k,
      ) ||
      !["boolean", "number"].includes(typeof v)
    )
      throw Error("Unexpected backup field");
    if (
      (k === "tasbih_count" && (typeof v !== "number" || v < 0)) ||
      (k === "tasbih_goal" && (typeof v !== "number" || v < 1 || v > 10000))
    )
      throw Error("Invalid counter");
  }
  return d;
}

export function mergeBackup(
  current: Personal,
  incoming: BackupBundle,
  merge: boolean,
): Personal {
  validateBackup(incoming);
  const tasks = new Map<string, PrayerRelativeReminder>(
    (merge ? current.reminders : []).map((t) => [t.id, t]),
  );
  for (const t of incoming.reminders) tasks.set(t.id, { ...t, enabled: false });
  return {
    preferences: merge
      ? {
          ...defaults.preferences,
          ...incoming.preferences,
          ...current.preferences,
        }
      : { ...defaults.preferences, ...incoming.preferences },
    bookmarks: [
      ...new Set([...(merge ? current.bookmarks : []), ...incoming.bookmarks]),
    ],
    reading: merge ? current.reading : incoming.reading,
    tracker: merge
      ? { ...incoming.tracker, ...current.tracker }
      : { ...incoming.tracker },
    reminders: [...tasks.values()],
  };
}

function escapeIcs(s: string) {
  return s
    .replaceAll(String.fromCharCode(92), String.fromCharCode(92, 92))
    .replaceAll(String.fromCharCode(10), String.fromCharCode(92) + "n")
    .replaceAll(",", String.fromCharCode(92) + ",")
    .replaceAll(";", String.fromCharCode(92) + ";");
}
export function calendarExport(days: PrayerSnapshot[], language: "ms" | "en") {
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Waktu Solat Malaysia//3.0//EN",
    "CALSCALE:GREGORIAN",
  ];
  for (const day of days)
    for (const i of obligatory) {
      const start = at(day.date, day.times[i]),
        end = new Date(+start + 60000),
        stamp = (d: Date) =>
          d
            .toISOString()
            .split(".")[0]
            .replaceAll("-", "")
            .replaceAll(":", "") + "Z";
      lines.push(
        "BEGIN:VEVENT",
        `UID:${day.zone}-${day.date}-${keys[i]}@waktusolatmalaysia`,
        `DTSTAMP:${stamp(new Date())}`,
        `DTSTART:${stamp(start)}`,
        `DTEND:${stamp(end)}`,
        `SUMMARY:${escapeIcs(names[language][i])}`,
        `DESCRIPTION:JAKIM e-Solat ${day.zone}`,
        "END:VEVENT",
      );
    }
  lines.push("END:VCALENDAR");
  return lines.join(String.fromCharCode(13, 10)) + String.fromCharCode(13, 10);
}
