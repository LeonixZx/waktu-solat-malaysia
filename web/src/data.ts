import { parseReadings } from "./readings";
import { openDB } from "idb";
import { useSyncExternalStore } from "react";
import type {
  Personal,
  PrayerSnapshot,
  Surah,
  Verse,
  Reading,
  CatalogEntry,
} from "./types";
import { defaults, parsePrayers } from "./core";

const dbPromise = openDB("wsm-v3", 1, {
  upgrade(db) {
    db.createObjectStore("content");
    db.createObjectStore("personal");
  },
});
const listeners = new Set<() => void>();
let state: Personal = structuredClone(defaults);
let loaded = false;
let write = Promise.resolve();

export const db = {
  async get<T>(key: string) {
    return (await dbPromise).get("content", key) as Promise<T | undefined>;
  },
  async put(key: string, value: unknown) {
    await (await dbPromise).put("content", value, key);
  },
  async remove(key: string) {
    await (await dbPromise).delete("content", key);
  },
  async keys() {
    return (await dbPromise).getAllKeys("content");
  },
};

export async function initialize() {
  try {
    const saved = await (await dbPromise).get("personal", "state");
    if (saved)
      state = {
        ...defaults,
        ...saved,
        preferences: { ...defaults.preferences, ...saved.preferences },
      };
  } finally {
    loaded = true;
    listeners.forEach((l) => l());
  }
}

export function update(fn: (value: Personal) => Personal) {
  state = fn(state);
  listeners.forEach((l) => l());
  const snapshot = structuredClone(state);
  write = write
    .catch(() => {})
    .then(async () => {
      await (await dbPromise).put("personal", snapshot, "state");
    });
  return write;
}

export function current() {
  return state;
}
export function usePersonal() {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb);
      return () => listeners.delete(cb);
    },
    () => state,
  );
}
export function isLoaded() {
  return loaded;
}
export function saveToggle(id: string) {
  void update((s) => ({
    ...s,
    bookmarks: s.bookmarks.includes(id)
      ? s.bookmarks.filter((k) => k !== id)
      : [...s.bookmarks, id],
  }));
}

export async function staticData<T>(file: string): Promise<T> {
  const response = await fetch("/data/" + file);
  if (!response.ok) throw Error("Content unavailable");
  return response.json();
}

export async function month(
  zone: string,
  yearMonth: string,
  refresh = false,
): Promise<PrayerSnapshot[]> {
  const key = `prayers:${zone}:${yearMonth}`,
    cached = await db.get<{ raw: unknown; retrievedAt: number }>(key);
  if (cached && !refresh)
    return parsePrayers(
      cached.raw,
      zone,
      yearMonth,
      cached.retrievedAt,
      "cached",
    );
  try {
    const response = await fetch(
      `/api/prayers?zone=${encodeURIComponent(zone)}&month=${yearMonth}`,
    );
    if (!response.ok) throw Error("Prayer service unavailable");
    const raw = await response.json();
    const retrievedAt = raw._meta?.retrievedAt ?? Date.now();
    const parsed = parsePrayers(raw, zone, yearMonth, retrievedAt);
    await db.put(key, { raw, retrievedAt });
    return parsed;
  } catch (error) {
    if (cached)
      return parsePrayers(
        cached.raw,
        zone,
        yearMonth,
        cached.retrievedAt,
        "offline",
      );
    throw error;
  }
}

export function parseVerses(
  raw: unknown,
  number: number,
  language: "ms" | "en",
): Verse[] {
  const editions = (
    raw as {
      data: {
        number: number;
        edition: { identifier: string };
        ayahs: {
          number: number;
          numberInSurah: number;
          text: string;
          audio?: string;
        }[];
      }[];
    }
  ).data;
  if (!Array.isArray(editions) || editions.some((e) => e.number !== number))
    throw Error("Wrong surah");
  const arabic = editions.find((e) => e.edition.identifier === "quran-uthmani"),
    meaning = editions.find(
      (e) =>
        e.edition.identifier ===
        (language === "en" ? "en.sahih" : "ms.basmeih"),
    ),
    audio = editions.find((e) => e.edition.identifier === "ar.alafasy");
  if (
    !arabic ||
    !meaning ||
    arabic.ayahs.length !== meaning.ayahs.length ||
    !arabic.ayahs.length ||
    (audio && audio.ayahs.length !== arabic.ayahs.length)
  )
    throw Error("Mismatched Quran editions");
  return arabic.ayahs.map((verse, i) => {
    if (
      verse.numberInSurah !== i + 1 ||
      meaning.ayahs[i].number !== verse.number ||
      meaning.ayahs[i].numberInSurah !== i + 1 ||
      (audio &&
        (audio.ayahs[i].number !== verse.number ||
          audio.ayahs[i].numberInSurah !== i + 1))
    )
      throw Error("Misaligned Quran verse");
    const url = audio?.ayahs[i].audio ?? "";
    if (url && !url.startsWith("https://cdn.islamic.network/"))
      throw Error("Invalid audio provider");
    return {
      number: i + 1,
      globalNumber: verse.number,
      arabic: verse.text.replace(/\uFEFF/g, ""),
      meaning: meaning.ayahs[i].text,
      audio: url,
    };
  });
}

export async function verses(
  number: number,
  language: "ms" | "en",
): Promise<Verse[]> {
  if (!Number.isInteger(number) || number < 1 || number > 114)
    throw Error("Invalid surah");
  const generation = textGenerations.get(number) ?? 0;
  const key = "quran:" + number,
    cached = await db.get<unknown>(key);
  if (cached) {
    try {
      return parseVerses(cached, number, language);
    } catch {}
  }
  const response = await fetch("/api/quran/" + number);
  if (!response.ok) throw Error("Quran service unavailable");
  const raw = await response.json(),
    parsed = parseVerses(raw, number, language);
  parseVerses(raw, number, "ms");
  parseVerses(raw, number, "en");
  if ((textGenerations.get(number) ?? 0) !== generation)
    throw Error("Text deleted during download");
  await db.put(key, raw);
  return parsed;
}

export async function surahs() {
  return (await staticData<{ data: Surah[] }>("quran_surahs.json")).data;
}

const clean = (text: string) =>
  text
    .split(String.fromCharCode(92) + "n")
    .join(String.fromCharCode(10))
    .split(String.fromCharCode(92) + '"')
    .join('"')
    .trim();

export async function readings(
  kind: string,
  language: "ms" | "en",
): Promise<Reading[]> {
  const files: Record<string, string> = {
    doa: "prayer_guides.json",
    wirid: "wirid.json",
    niat: "prayer_intentions.json",
    hadith: "hadiths.json",
  };
  if (!files[kind]) throw Error("Invalid collection");
  const data = await staticData<{ data: Record<string, string | number>[] }>(
    files[kind],
  );
  return parseReadings(kind, data.data, language);
}

export async function catalog() {
  return (await staticData<{ entries: CatalogEntry[] }>("catalog.json"))
    .entries;
}

async function changeQueue(change: (pending: number[]) => number[]) {
  const tx = (await dbPromise).transaction("content", "readwrite");
  const pending = (await tx.store.get("downloadQueue")) as number[] | undefined;
  await tx.store.put(change(pending ?? []), "downloadQueue");
  await tx.done;
}
export async function queueAdd(numbers: number[]) {
  await changeQueue((pending) => [
    ...new Set([
      ...pending,
      ...numbers.filter((n) => Number.isInteger(n) && n >= 1 && n <= 114),
    ]),
  ]);
}
const textGenerations = new Map<number, number>();
export async function deleteText(number: number) {
  textGenerations.set(number, (textGenerations.get(number) ?? 0) + 1);
  await changeQueue((pending) => pending.filter((n) => n !== number));
  await db.remove("quran:" + number);
}

let paused = true,
  running = false;
const progressListeners = new Set<() => void>();
export function watchDownloads(cb: () => void) {
  progressListeners.add(cb);
  return () => {
    progressListeners.delete(cb);
  };
}
export function downloadRunning() {
  return running;
}
export function pauseDownloads() {
  paused = true;
}
export async function runDownloads() {
  if (running) return;
  paused = false;
  running = true;
  progressListeners.forEach((cb) => cb());
  try {
    while (!paused) {
      const queue = (await db.get<number[]>("downloadQueue")) ?? [];
      if (!queue.length) break;
      await verses(queue[0], "en");
      await changeQueue((pending) => pending.filter((n) => n !== queue[0]));
      progressListeners.forEach((cb) => cb());
    }
  } finally {
    running = false;
    progressListeners.forEach((cb) => cb());
  }
}
