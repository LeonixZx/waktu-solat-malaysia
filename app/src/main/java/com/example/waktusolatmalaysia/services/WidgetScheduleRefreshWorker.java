package com.example.waktusolatmalaysia.services;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.example.waktusolatmalaysia.repo.PrayerRepository;
import com.example.waktusolatmalaysia.utils.PrayerClock;
import com.example.waktusolatmalaysia.widgets.PrayerWidgets;
import java.time.*;
import java.util.*;

/** Unique work per zone: a newly chosen zone cannot be hidden behind another zone's job. */
public final class WidgetScheduleRefreshWorker extends Worker {
  public WidgetScheduleRefreshWorker(@NonNull Context c, @NonNull WorkerParameters p) {
    super(c, p);
  }

  private static String name(String z) {
    return "widget_schedule_" + z;
  }

  public static void enqueue(Context c, String z) {
    if (z == null || !z.matches("[A-Z]{3}[0-9]{2}")) return;
    WorkManager.getInstance(c)
        .enqueueUniqueWork(
            name(z),
            ExistingWorkPolicy.KEEP,
            new OneTimeWorkRequest.Builder(WidgetScheduleRefreshWorker.class)
                .setInputData(new Data.Builder().putString("zone", z).build())
                .setConstraints(
                    new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build());
  }

  public static void enqueueAll(Context c) {
    for (String z : PrayerWidgets.zones(c)) enqueue(c, z);
  }

  public static void cleanup(Context c) {
    Set<String> current = PrayerWidgets.zones(c);
    android.content.SharedPreferences p = c.getSharedPreferences("widget_work_zones", 0);
    Set<String> previous = p.getStringSet("zones", Collections.emptySet());
    for (String z : previous)
      if (!current.contains(z)) WorkManager.getInstance(c).cancelUniqueWork(name(z));
    p.edit().putStringSet("zones", current).apply();
  }

  @NonNull
  @Override
  public Result doWork() {
    String zone = getInputData().getString("zone");
    if (zone == null || !PrayerWidgets.zones(getApplicationContext()).contains(zone))
      return Result.success();
    try {
      LocalDate today = LocalDate.now(PrayerClock.MALAYSIA);
      Set<YearMonth> months = new HashSet<>();
      months.add(YearMonth.from(today));
      months.add(YearMonth.from(today.plusDays(1)));
      boolean failed = false;
      PrayerRepository repo = new PrayerRepository(getApplicationContext());
      for (YearMonth month : months) failed |= repo.month(zone, month, true).offline;
      PrayerWidgets.refresh(getApplicationContext());
      ReminderScheduler.reschedule(getApplicationContext());
      return failed && getRunAttemptCount() < 3 ? Result.retry() : Result.success();
    } catch (Exception e) {
      return getRunAttemptCount() < 3 ? Result.retry() : Result.failure();
    }
  }
}
