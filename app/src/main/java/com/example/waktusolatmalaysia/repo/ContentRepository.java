package com.example.waktusolatmalaysia.repo;

import android.content.Context;
import android.util.AtomicFile;
import com.example.waktusolatmalaysia.model.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

public final class ContentRepository {
  private static final Map<Integer, Integer> generations = new HashMap<>();

  public static synchronized void deleteSurah(Context c, int number) {
    generations.put(number, generations.getOrDefault(number, 0) + 1);
    new AtomicFile(new File(c.getFilesDir(), "quran/surah_" + number + ".json")).delete();
  }

  private final Context context;

  public ContentRepository(Context context) {
    this.context = context.getApplicationContext();
  }

  public String asset(String name) throws IOException {
    try (InputStream input = context.getAssets().open(name)) {
      return read(input);
    }
  }

  public List<Surah> surahs() throws Exception {
    return ContentParser.surahs(asset("quran_surahs.json"));
  }

  public List<QuranVerse> verses(int number) throws Exception {
    return verses(number, com.example.waktusolatmalaysia.ui.I18n.english(context) ? "en" : "ms");
  }

  public List<QuranVerse> verses(int number, String language) throws Exception {
    if (number < 1 || number > 114) throw new IOException("Invalid surah number");
    final int generation;
    synchronized (ContentRepository.class) {
      generation = generations.getOrDefault(number, 0);
    }
    File directory = new File(context.getFilesDir(), "quran");
    AtomicFile cache = new AtomicFile(new File(directory, "surah_" + number + ".json"));
    if (cache.getBaseFile().exists()) {
      try {
        return ContentParser.verses(
            new String(cache.readFully(), StandardCharsets.UTF_8), number, language);
      } catch (Exception ignored) {
      }
    }
    String json =
        fetch(
            "https://api.alquran.cloud/v1/surah/"
                + number
                + "/editions/quran-uthmani,ms.basmeih,ar.alafasy,en.sahih");
    List<QuranVerse> result = ContentParser.verses(json, number, language);
    ContentParser.verses(json, number, "ms");
    ContentParser.verses(json, number, "en");
    synchronized (ContentRepository.class) {
      if (generations.getOrDefault(number, 0) != generation)
        throw new InterruptedIOException("Text deleted during download");
      if (directory.isDirectory() || directory.mkdirs()) {
        FileOutputStream output = null;
        try {
          output = cache.startWrite();
          output.write(json.getBytes(StandardCharsets.UTF_8));
          cache.finishWrite(output);
        } catch (IOException failure) {
          if (output != null) cache.failWrite(output);
        }
      }
    }
    return result;
  }

  public List<Reading> readings(String kind) throws Exception {
    boolean english = com.example.waktusolatmalaysia.ui.I18n.english(context);
    String filename =
        kind.equals("hadith")
            ? "hadiths.json"
            : kind.equals("wirid")
                ? "wirid.json"
                : kind.equals("niat") ? "prayer_intentions.json" : "prayer_guides.json";
    return ReadingParser.parse(kind, asset(filename), english);
  }

  private static String clean(String value) {
    return value.replace("\\n", "\n").replace("\\\"", "\"").trim();
  }

  private static String fetch(String endpoint) throws IOException {
    HttpsURLConnection connection = (HttpsURLConnection) new URL(endpoint).openConnection();
    connection.setConnectTimeout(15000);
    connection.setReadTimeout(20000);
    connection.setRequestProperty("Accept", "application/json");
    try {
      if (connection.getResponseCode() != 200) throw new IOException("Content service unavailable");
      try (InputStream input = connection.getInputStream()) {
        return read(input);
      }
    } finally {
      connection.disconnect();
    }
  }

  private static String read(InputStream input) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int count;
    while ((count = input.read(buffer)) != -1) {
      bytes.write(buffer, 0, count);
      if (bytes.size() > 6000000) throw new IOException("Response too large");
    }
    return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
  }
}
