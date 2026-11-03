package com.example.waktusolatmalaysia.repo;

import static org.junit.Assert.*;

import java.nio.file.*;
import org.json.*;
import org.junit.Test;

public class ContentParserTest {
  private JSONObject fixture() throws Exception {
    JSONArray data = new JSONArray();
    for (String edition : new String[] {"quran-uthmani", "ms.basmeih", "ar.alafasy"}) {
      JSONObject row =
          new JSONObject()
              .put("number", 1)
              .put("edition", new JSONObject().put("identifier", edition));
      JSONArray ayahs = new JSONArray();
      for (int i = 1; i <= 2; i++)
        ayahs.put(
            new JSONObject()
                .put("number", i)
                .put("numberInSurah", i)
                .put("text", edition.equals("quran-uthmani") ? "بِسْمِ اللَّهِ" : "Makna " + i)
                .put("audio", "https://example.org/" + i + ".mp3"));
      row.put("ayahs", ayahs);
      data.put(row);
    }
    return new JSONObject().put("data", data);
  }

  @Test
  public void shippedCatalogHasAll114Surahs() throws Exception {
    var surahs =
        ContentParser.surahs(
            new String(
                Files.readAllBytes(Paths.get("src/main/assets/quran_surahs.json")),
                java.nio.charset.StandardCharsets.UTF_8));
    assertEquals(114, surahs.size());
    assertEquals(7, surahs.get(0).verses);
    assertEquals(286, surahs.get(1).verses);
    assertEquals(114, surahs.get(113).number);
  }

  @Test
  public void arabicMalayAndAudioStayAligned() throws Exception {
    var verses = ContentParser.verses(fixture().toString(), 1);
    assertEquals(2, verses.size());
    assertEquals("بِسْمِ اللَّهِ", verses.get(0).arabic);
    assertEquals("Makna 2", verses.get(1).meaning);
    assertEquals("https://example.org/2.mp3", verses.get(1).audio);
  }

  @Test(expected = JSONException.class)
  public void wrongSurahIsRejected() throws Exception {
    ContentParser.verses(fixture().toString(), 2);
  }

  @Test(expected = JSONException.class)
  public void shiftedMalayIsRejected() throws Exception {
    JSONObject f = fixture();
    f.getJSONArray("data")
        .getJSONObject(1)
        .getJSONArray("ayahs")
        .getJSONObject(0)
        .put("numberInSurah", 2);
    ContentParser.verses(f.toString(), 1);
  }

  @Test(expected = JSONException.class)
  public void shiftedAudioIsRejected() throws Exception {
    JSONObject f = fixture();
    f.getJSONArray("data").getJSONObject(2).getJSONArray("ayahs").getJSONObject(0).put("number", 2);
    ContentParser.verses(f.toString(), 1);
  }

  @Test(expected = JSONException.class)
  public void missingTranslationIsRejected() throws Exception {
    JSONObject f = fixture();
    f.getJSONArray("data").remove(1);
    ContentParser.verses(f.toString(), 1);
  }

  @Test(expected = JSONException.class)
  public void duplicateSurahMetadataIsRejected() throws Exception {
    JSONObject f =
        new JSONObject(
            new String(
                Files.readAllBytes(Paths.get("src/main/assets/quran_surahs.json")),
                java.nio.charset.StandardCharsets.UTF_8));
    f.getJSONArray("data").getJSONObject(1).put("number", 1);
    ContentParser.surahs(f.toString());
  }
}
