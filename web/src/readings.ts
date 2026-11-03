import type { Language, Reading, ReadingSection } from "./types";

const clean = (s: unknown) =>
  String(s ?? "")
    .replaceAll("\\n", "\n")
    .replaceAll('\\"', '"')
    .trim();
export function sectionTranslation(s: ReadingSection, language: Language) {
  return language === "en" && s.englishTranslation
    ? s.englishTranslation
    : s.localTranslation;
}
export function sectionLanguage(s: ReadingSection, language: Language) {
  return language === "en" && s.englishTranslation
    ? "English"
    : s.localLanguage === "id"
      ? "Bahasa Indonesia"
      : "Bahasa Melayu";
}
export function parseReadings(
  kind: string,
  rows: Record<string, unknown>[],
  language: Language,
): Reading[] {
  if (!["doa", "wirid", "niat", "hadith"].includes(kind))
    throw new Error("Unknown collection");
  return rows.map((r, i) => {
    const sections: ReadingSection[] = [];
    let title = "",
      reference = "";
    if (kind === "hadith") {
      title = "Sahih al-Bukhari · " + r.id;
      reference = clean(r.source) || "Sahih al-Bukhari " + r.id;
      sections.push({
        id: "1",
        title: "",
        arabic: clean(r.arabic),
        transliteration: "",
        localTranslation: clean(r.translation),
        englishTranslation: clean(r.english_translation),
        localLanguage: "id",
      });
    } else if (kind === "wirid") {
      title = clean(r.title).replace(/^\d+\)\s*/, "");
      reference =
        language === "en"
          ? "Dhikr after prayer · supplied collection"
          : "Wirid selepas solat · koleksi sumber";
      sections.push({
        id: "1",
        title: "",
        arabic: clean(r.arabic_text),
        transliteration: clean(r.arabic_translation),
        localTranslation: clean(r.malay_translation),
        englishTranslation: clean(r.english_translation),
        localLanguage: "ms",
      });
    } else {
      title =
        clean(r[language === "en" ? "title_english" : "title_malay"]) ||
        clean(r.title_malay);
      reference =
        kind === "niat"
          ? language === "en"
            ? "Prayer intentions"
            : "Niat solat fardu"
          : language === "en"
            ? "Prayer guides"
            : "Doa dan bacaan solat";
      for (let n = 1; n <= 5; n++) {
        if (!clean(r["arabic_text_" + n])) continue;
        sections.push({
          id: String(n),
          title:
            clean(
              r[`head_text_${n}_${language === "en" ? "english" : "malay"}`],
            ) || clean(r[`head_text_${n}_malay`]),
          arabic: clean(r["arabic_text_" + n]),
          transliteration: clean(r["transliteration_" + n]),
          localTranslation: clean(r["malay_translation_" + n]),
          englishTranslation: clean(r["english_translation_" + n]),
          localLanguage: "ms",
        });
      }
    }
    return {
      id: kind + ":" + i,
      title,
      reference,
      kind,
      sections,
      requestedLanguage: language,
      arabic: sections.map((s) => s.arabic).join("\n\n"),
      meaning: sections
        .map((s) => sectionTranslation(s, language))
        .join("\n\n"),
    };
  });
}
