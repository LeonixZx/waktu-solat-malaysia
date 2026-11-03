package com.example.waktusolatmalaysia.services;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.utils.*;
import com.example.waktusolatmalaysia.widgets.PrayerWidgets;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ScheduleRefreshWorker extends Worker {
  public ScheduleRefreshWorker(@NonNull Context c, @NonNull WorkerParameters p) {
    super(c, p);
  }

  public static void enqueue(Context c, boolean force) {
    Constraints constraints =
        new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    WorkManager m = WorkManager.getInstance(c);
    m.enqueueUniquePeriodicWork(
        "prayer_prefetch_periodic",
        ExistingPeriodicWorkPolicy.KEEP,
        new PeriodicWorkRequest.Builder(ScheduleRefreshWorker.class, 12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build());
    if (force
        || new UserStore(c).number("prefetch_day", 0)
            != (int) LocalDate.now(PrayerClock.MALAYSIA).toEpochDay())
      m.enqueueUniqueWork(
          "prayer_prefetch_now",
          ExistingWorkPolicy.KEEP,
          new OneTimeWorkRequest.Builder(ScheduleRefreshWorker.class)
              .setConstraints(constraints)
              .build());
  }

  @NonNull
  @Override
  public Result doWork() {
    Context c = getApplicationContext();
    try {
      Set<String> zones = PrayerWidgets.zones(c);
      zones.add(new AppPreferences(c).zone().code);
      LocalDate now = LocalDate.now(PrayerClock.MALAYSIA);
      PrayerRepository repo = new PrayerRepository(c);
      Set<YearMonth> months = new HashSet<>();
      months.add(YearMonth.from(now.minusDays(1)));
      months.add(YearMonth.from(now));
      months.add(YearMonth.from(now.plusDays(9)));
      boolean failed = false;
      for (String z : zones)
        for (YearMonth month : months) {
          if (isStopped()) return Result.retry();
          failed |= repo.month(z, month, true).offline;
        }
      if (!failed) new UserStore(c).set("prefetch_day", (int) now.toEpochDay());
      ReminderScheduler.scheduleNow(c);
      PrayerWidgets.refresh(c);
      return failed ? Result.retry() : Result.success();
    } catch (Exception e) {
      android.util.Log.w("PrayerPrefetch", "Schedule refresh unavailable");
      return Result.retry();
    }
  }
}
