package com.example.waktusolatmalaysia.repo;

import com.example.waktusolatmalaysia.model.*;
import java.util.*;
import org.json.*;

public final class ContentParser {
  private ContentParser() {}

  public static List<Surah> surahs(String json) throws JSONException {
    JSONArray data = new JSONObject(json).getJSONArray("data");
    List<Surah> result = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (int i = 0; i < data.length(); i++) {
      JSONObject row = data.getJSONObject(i);
      int number = row.getInt("number"), count = row.getInt("numberOfAyahs");
      if (number < 1 || number > 114 || count < 1 || !seen.add(number))
        throw new JSONException("Invalid surah metadata");
      result.add(
          new Surah(
              number,
              count,
              row.getString("englishName"),
              row.getString("name"),
              row.getString("englishNameTranslation"),
              row.getString("revelationType")));
    }
    return result;
  }

  public static List<QuranVerse> verses(String json, int surah) throws JSONException {
    return verses(json, surah, "ms");
  }

  public static List<QuranVerse> verses(String json, int surah, String language)
      throws JSONException {
    JSONArray editions = new JSONObject(json).getJSONArray("data");
    JSONObject arabic = null, meaning = null, audio = null;
    for (int i = 0; i < editions.length(); i++) {
      JSONObject row = editions.getJSONObject(i);
      if (row.getInt("number") != surah) throw new JSONException("Wrong surah response");
      String edition = row.getJSONObject("edition").getString("identifier");
      if (edition.equals("quran-uthmani")) arabic = row;
      else if (edition.equals(language.equals("en") ? "en.sahih" : "ms.basmeih")) meaning = row;
      else if (edition.equals("ar.alafasy")) audio = row;
    }
    if (arabic == null || meaning == null)
      throw new JSONException("Arabic and Malay editions are required");
    JSONArray a = arabic.getJSONArray("ayahs"), m = meaning.getJSONArray("ayahs");
    JSONArray sounds = audio == null ? null : audio.getJSONArray("ayahs");
    if (a.length() == 0
        || a.length() != m.length()
        || (sounds != null && sounds.length() != a.length()))
      throw new JSONException("Mismatched Quran editions");
    List<QuranVerse> result = new ArrayList<>();
    for (int i = 0; i < a.length(); i++) {
      JSONObject verse = a.getJSONObject(i), translated = m.getJSONObject(i);
      if (verse.getInt("number") != translated.getInt("number")
          || verse.getInt("numberInSurah") != i + 1)
        throw new JSONException("Misaligned Quran verse");
      if (translated.getInt("numberInSurah") != i + 1
          || (sounds != null
              && (sounds.getJSONObject(i).getInt("number") != verse.getInt("number")
                  || sounds.getJSONObject(i).getInt("numberInSurah") != i + 1)))
        throw new JSONException("Misaligned translation or audio");
      result.add(
          new QuranVerse(
              i + 1,
              verse.getInt("number"),
              verse.getString("text").replace("\uFEFF", ""),
              translated.getString("text"),
              sounds == null ? "" : sounds.getJSONObject(i).optString("audio")));
    }
    return result;
  }
}
