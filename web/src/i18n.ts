import words from "../../shared/i18n.json";
import type { Language } from "./types";
const dictionary = words as Record<string, string>;
export const t = (language: Language, ms: string, en?: string) =>
  language === "en" ? (en ?? dictionary[ms] ?? ms) : ms;
