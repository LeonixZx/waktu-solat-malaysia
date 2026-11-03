package com.example.waktusolatmalaysia.repo;

import com.example.waktusolatmalaysia.model.Reading;
import java.util.*;
import org.json.*;

/** Parses supplied text only. IDs remain compatible with the original local bookmarks. */
public final class ReadingParser {
  private ReadingParser() {}
  public static List<Reading> parse(String kind, String json, boolean english) throws JSONException {
    if (!Set.of("doa", "wirid", "niat", "hadith").contains(kind)) throw new JSONException("Unknown collection");
    JSONArray rows = new JSONObject(json).getJSONArray("data");
    List<Reading> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); i++) {
      JSONObject row = rows.getJSONObject(i);
      List<Reading.Section> sections = new ArrayList<>();
      String title, reference;
      if (kind.equals("hadith")) {
        title = "Sahih al-Bukhari · " + row.getInt("id");
        reference = clean(row.optString("source"));
        if (reference.isEmpty()) reference = "Sahih al-Bukhari " + row.getInt("id");
        sections.add(new Reading.Section("1", "", clean(row.optString("arabic")), "",
            clean(row.optString("translation")), clean(row.optString("english_translation")), "id"));
      } else if (kind.equals("wirid")) {
        title = row.getString("title").replaceFirst("^\\d+\\)\\s*", "");
        reference = english ? "Dhikr after prayer · supplied collection" : "Wirid selepas solat · koleksi sumber";
        sections.add(new Reading.Section("1", "", clean(row.optString("arabic_text")),
            clean(row.optString("arabic_translation")), clean(row.optString("malay_translation")),
            clean(row.optString("english_translation")), "ms"));
      } else {
        title = clean(row.optString(english ? "title_english" : "title_malay"));
        if (title.isEmpty()) title = clean(row.optString("title_malay"));
        reference = kind.equals("niat") ? (english ? "Prayer intentions" : "Niat solat fardu")
            : (english ? "Prayer guides" : "Doa dan bacaan solat");
        for (int n = 1; n <= 5; n++) {
          String arabic = clean(row.optString("arabic_text_" + n));
          if (arabic.isEmpty()) continue;
          String heading = clean(row.optString("head_text_" + n + (english ? "_english" : "_malay")));
          if (heading.isEmpty()) heading = clean(row.optString("head_text_" + n + "_malay"));
          sections.add(new Reading.Section(String.valueOf(n), heading, arabic,
              clean(row.optString("transliteration_" + n)), clean(row.optString("malay_translation_" + n)),
              clean(row.optString("english_translation_" + n)), "ms"));
        }
      }
      result.add(new Reading(kind + ":" + i, title, reference, kind, sections, english));
    }
    return result;
  }
  private static String clean(String s) { return s.replace("\\n", "\n").replace("\\\"", "\"").trim(); }
}
