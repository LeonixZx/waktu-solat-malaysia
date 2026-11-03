export type Language = "ms" | "en";
export const keys = [
  "imsak",
  "fajr",
  "syuruk",
  "dhuha",
  "dhuhr",
  "asr",
  "maghrib",
  "isha",
] as const;
export const obligatory = [1, 4, 5, 6, 7];
export type Zone = { code: string; state: string; districts: string };
export type PrayerSnapshot = {
  zone: string;
  date: string;
  timeZone: "Asia/Kuala_Lumpur";
  times: string[];
  hijri: string;
  source: "JAKIM e-Solat";
  retrievedAt: number;
  freshness: "live" | "cached" | "offline";
};
export type Surah = {
  number: number;
  name: string;
  englishName: string;
  numberOfAyahs: number;
  revelationType: string;
};
export type Verse = {
  number: number;
  globalNumber: number;
  arabic: string;
  meaning: string;
  audio: string;
};
export type Reading = {
  id: string;
  title: string;
  arabic: string;
  meaning: string;
  reference: string;
  kind: string;
  sections: ReadingSection[];
  requestedLanguage: Language;
};
export type ReadingSection = {
  id: string;
  title: string;
  arabic: string;
  transliteration: string;
  localTranslation: string;
  englishTranslation: string;
  localLanguage: "ms" | "id";
};
export type CatalogEntry = {
  id: string;
  type: "video" | "article" | "channel";
  provider: string;
  language: Language;
  title: string;
  summary: string;
  url: string;
  thumbnail: string;
  youtubeId: string;
};
export type PrayerRelativeReminder = {
  id: string;
  title: string;
  anchor: (typeof keys)[number];
  offsetMinutes: number;
  weekdays: number[];
  enabled: boolean;
};
export type Preferences = {
  zone_code: string;
  zone_state: string;
  zone_name: string;
  twelve_hour: boolean;
  dark: boolean;
  theme: "light" | "dark" | "system";
  language: Language;
  arabic_size: number;
  show_translation: boolean;
  quran_language?: Language;
};
export type Personal = {
  preferences: Preferences;
  bookmarks: string[];
  reading: { surah: number; verse: number; name: string };
  tracker: Record<string, boolean | number>;
  reminders: PrayerRelativeReminder[];
};
export type BackupBundle = Personal & { schemaVersion: 1; exportedAt: string };
