package com.example.waktusolatmalaysia.repo;

import android.content.Context;
import android.util.AtomicFile;
import com.example.waktusolatmalaysia.model.PrayerDay;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/** Called exclusively from the ViewModel's worker thread. Cache is keyed by zone AND month. */
public final class PrayerRepository {
  private static final Object CACHE_LOCK = new Object();
  private static final java.util.concurrent.ConcurrentHashMap<
          String, java.util.concurrent.FutureTask<Month>>
      IN_FLIGHT = new java.util.concurrent.ConcurrentHashMap<>();

  private static byte[] readCache(AtomicFile file) throws IOException {
    synchronized (CACHE_LOCK) {
      return file.readFully();
    }
  }

  private final File directory;
  private final Context context;
  private final Map<String, Month> memory = new java.util.concurrent.ConcurrentHashMap<>();

  public PrayerRepository(Context context) {
    this.context = context.getApplicationContext();
    directory = new File(context.getFilesDir(), "prayer_times");
  }

  public Month month(String zone, YearMonth month, boolean refresh) throws IOException {
    if (!zone.matches("[A-Z]{3}[0-9]{2}")) throw new IOException("Invalid zone");
    String key = zone + "_" + month;
    AtomicFile cache = new AtomicFile(new File(directory, key + ".json"));
    Month saved = memory.get(key);
    if (saved == null && cache.getBaseFile().exists()) {
      try {
        saved =
            new Month(
                PrayerParser.parse(
                    new String(readCache(cache), StandardCharsets.UTF_8), month, zone),
                true,
                false);
      } catch (Exception ignored) {
        /* A corrupt cache must never block a fresh download. */
      }
    }
    if (saved != null && !refresh) return saved;
    java.util.concurrent.FutureTask<Month> task =
        new java.util.concurrent.FutureTask<>(() -> download(zone, month, cache));
    java.util.concurrent.FutureTask<Month> existing = IN_FLIGHT.putIfAbsent(key, task);
    boolean owner = existing == null;
    if (!owner) task = existing;
    try {
      if (owner) task.run();
      Month result = task.get();
      memory.put(key, new Month(result.days, true, false));
      return result;
    } catch (Exception failure) {
      if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
      if (saved != null) return new Month(saved.days, true, true);
      throw new IOException("Cannot load prayer timetable", failure);
    } finally {
      if (owner) IN_FLIGHT.remove(key, task);
    }
  }

  private Month download(String zone, YearMonth month, AtomicFile cache) throws IOException {
    HttpsURLConnection connection = null;
    long started = android.os.SystemClock.elapsedRealtime();
    boolean success = false;
    try {
      String endpoint =
          "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone="
              + zone
              + "&year="
              + month.getYear()
              + "&month="
              + month.getMonthValue();
      connection = (HttpsURLConnection) new URL(endpoint).openConnection();
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(15000);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("User-Agent", "WaktuSolatMalaysia-Android/1.0");
      if (connection.getResponseCode() != 200)
        throw new IOException("Prayer service HTTP " + connection.getResponseCode());
      byte[] bytes;
      try (InputStream input = connection.getInputStream();
          ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = input.read(buffer)) != -1) {
          output.write(buffer, 0, length);
          if (output.size() > 512000) throw new IOException("Prayer response too large");
        }
        bytes = output.toByteArray();
      }
      Month result =
          new Month(
              PrayerParser.parse(new String(bytes, StandardCharsets.UTF_8), month, zone),
              false,
              false);
      writeCache(cache, bytes);
      com.example.waktusolatmalaysia.widgets.PrayerWidgets.refreshAsync(context);
      success = true;
      return result;
    } catch (Exception failure) {
      throw new IOException("Cannot load prayer timetable", failure);
    } finally {
      android.util.Log.d(
          "PrayerProvider",
          "JAKIM "
              + (success ? "ok" : "unavailable")
              + " ms="
              + (android.os.SystemClock.elapsedRealtime() - started));
      if (connection != null) connection.disconnect();
    }
  }

  public java.util.List<PrayerDay> cachedDays(String zone, LocalDate from, LocalDate to) {
    java.util.List<PrayerDay> result = new java.util.ArrayList<>();
    if (!zone.matches("[A-Z]{3}[0-9]{2}")) return result;
    for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
      try {
        AtomicFile file = new AtomicFile(new File(directory, zone + "_" + m + ".json"));
        for (PrayerDay day :
            PrayerParser.parse(new String(readCache(file), StandardCharsets.UTF_8), m, zone)
                .values()) if (!day.date.isBefore(from) && !day.date.isAfter(to)) result.add(day);
      } catch (Exception ignored) {
      }
    }
    return result;
  }

  public long cachedAt(String zone, YearMonth month) {
    return new File(directory, zone + "_" + month + ".json").lastModified();
  }

  private void writeCache(AtomicFile file, byte[] bytes) {
    synchronized (CACHE_LOCK) {
      if (!directory.isDirectory() && !directory.mkdirs()) return;
      FileOutputStream output = null;
      try {
        output = file.startWrite();
        output.write(bytes);
        file.finishWrite(output);
      } catch (IOException failure) {
        if (output != null) file.failWrite(output);
      }
    }
  }

  public static final class Month {
    public final Map<LocalDate, PrayerDay> days;
    public final boolean cached;
    public final boolean offline;

    Month(Map<LocalDate, PrayerDay> days, boolean cached, boolean offline) {
      this.days = days;
      this.cached = cached;
      this.offline = offline;
    }
  }
}
