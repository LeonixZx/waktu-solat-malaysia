import { describe, it, expect } from "vitest";
import { readFileSync } from "node:fs";
import { parseReadings, sectionLanguage } from "./readings";
describe("supplied devotional catalog", () => {
  for (const language of ["ms", "en"] as const)
    it(`audits all 36 readings in ${language}`, () => {
      const files = {
        doa: "prayer_guides.json",
        wirid: "wirid.json",
        niat: "prayer_intentions.json",
        hadith: "hadiths.json",
      };
      const ids = new Set<string>();
      for (const [kind, file] of Object.entries(files)) {
        const rows = parseReadings(
          kind,
          JSON.parse(readFileSync(`../app/src/main/assets/${file}`, "utf8"))
            .data,
          language,
        );
        for (const [i, r] of rows.entries()) {
          expect(r.id).toBe(kind + ":" + i);
          ids.add(r.id);
          expect(r.title).toBeTruthy();
          expect(r.sections.length).toBeGreaterThan(0);
          for (const s of r.sections) {
            expect(s.arabic).toBeTruthy();
            expect(sectionLanguage(s, language)).toBeTruthy();
          }
        }
      }
      expect(ids.size).toBe(36);
    });
  it("keeps Rumi separate from the Malay fallback", () => {
    const row = parseReadings(
      "wirid",
      JSON.parse(readFileSync("../app/src/main/assets/wirid.json", "utf8"))
        .data,
      "en",
    )[0];
    expect(row.sections[0].transliteration).toBeTruthy();
    expect(row.sections[0].englishTranslation).toBe("");
    expect(row.meaning).toBe(row.sections[0].localTranslation);
    expect(sectionLanguage(row.sections[0], "en")).toBe("Bahasa Melayu");
  });
});
