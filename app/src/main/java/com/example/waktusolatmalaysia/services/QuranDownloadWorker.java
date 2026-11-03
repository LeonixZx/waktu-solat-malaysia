package com.example.waktusolatmalaysia.services;

import android.content.*;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.example.waktusolatmalaysia.repo.ContentRepository;
import java.util.*;
import org.json.*;

public final class QuranDownloadWorker extends Worker {
  public QuranDownloadWorker(@NonNull Context c, @NonNull WorkerParameters p) {
    super(c, p);
  }

  public static List<Integer> pending(Context c) {
    List<Integer> list = new ArrayList<>();
    try {
      JSONArray a =
          new JSONArray(c.getSharedPreferences("quran_downloads", 0).getString("pending", "[]"));
      for (int i = 0; i < a.length(); i++) {
        int n = a.getInt(i);
        if (n >= 1 && n <= 114 && !list.contains(n)) list.add(n);
      }
    } catch (Exception ignored) {
    }
    return list;
  }

  private static void queue(Context c, List<Integer> list) {
    c.getSharedPreferences("quran_downloads", 0)
        .edit()
        .putString("pending", new JSONArray(list).toString())
        .commit();
  }

  public static void start(Context c, List<Integer> values, boolean cellular) {
    synchronized (QuranDownloadWorker.class) {
      List<Integer> all = pending(c);
      for (int i : values) if (i >= 1 && i <= 114 && !all.contains(i)) all.add(i);
      queue(c, all);
    }
    c.getSharedPreferences("quran_downloads", 0).edit().putString("error", "").apply();
    Constraints constraint =
        new Constraints.Builder()
            .setRequiredNetworkType(cellular ? NetworkType.CONNECTED : NetworkType.UNMETERED)
            .build();
    WorkManager.getInstance(c)
        .enqueueUniqueWork(
            "quran_text_download",
            ExistingWorkPolicy.KEEP,
            new OneTimeWorkRequest.Builder(QuranDownloadWorker.class)
                .setConstraints(constraint)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
                .build());
  }

  public static void cancel(Context c) {
    WorkManager.getInstance(c).cancelUniqueWork("quran_text_download");
  }

  public static synchronized void delete(Context c, int number) {
    cancel(c);
    List<Integer> remaining = pending(c);
    remaining.remove(Integer.valueOf(number));
    queue(c, remaining);
    ContentRepository.deleteSurah(c, number);
  }

  @NonNull
  @Override
  public Result doWork() {
    long started = System.currentTimeMillis();
    Context c = getApplicationContext();
    ContentRepository repository = new ContentRepository(c);
    try {
      while (!pending(c).isEmpty()) {
        if (isStopped() || System.currentTimeMillis() - started > 420000) return Result.retry();
        int n = pending(c).get(0);
        repository.verses(n, "en");
        synchronized (QuranDownloadWorker.class) {
          List<Integer> remaining = pending(c);
          remaining.remove(Integer.valueOf(n));
          queue(c, remaining);
        }
        setProgressAsync(
            new Data.Builder().putInt("surah", n).putInt("remaining", pending(c).size()).build());
      }
      return Result.success();
    } catch (Exception e) {
      c.getSharedPreferences("quran_downloads", 0).edit().putString("error", "network").apply();
      return Result.retry();
    }
  }
}
