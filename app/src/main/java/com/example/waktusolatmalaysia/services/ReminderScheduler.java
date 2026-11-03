package com.example.waktusolatmalaysia.services;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import com.example.waktusolatmalaysia.model.*;
import com.example.waktusolatmalaysia.preferences.*;
import com.example.waktusolatmalaysia.repo.*;
import com.example.waktusolatmalaysia.utils.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class ReminderScheduler {
  private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

  public static void reschedule(Context c) {
    Context app = c.getApplicationContext();
    WORKER.execute(() -> scheduleNow(app));
  }

  public static synchronized void scheduleNow(Context c) {
    AlarmManager manager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    UserStore store = new UserStore(c);
    String zone = new AppPreferences(c).zone().code;
    ZonedDateTime now = ZonedDateTime.now(PrayerClock.MALAYSIA);
    List<PrayerDay> days =
        new PrayerRepository(c)
            .cachedDays(zone, now.toLocalDate().minusDays(1), now.toLocalDate().plusDays(9));
    String generation = UUID.randomUUID().toString();
    store.set("alarm_generation", generation);
    for (int i : PrayerDay.OBLIGATORY) {
      PendingIntent legacy =
          PendingIntent.getBroadcast(
              c,
              i,
              new Intent(c, PrayerAlarmReceiver.class),
              PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
      if (legacy != null) {
        manager.cancel(legacy);
        legacy.cancel();
      }
      PendingIntent pi = pending(c, "prayer/" + i, new Intent(c, PrayerAlarmReceiver.class), i);
      manager.cancel(pi);
      store.set("scheduled_" + i, "");
      if (PrayerMode.get(c, i) == PrayerMode.OFF) continue;
      ZonedDateTime time = ReminderPlanner.next(days, i, now);
      if (time == null) continue;
      Intent alarm =
          new Intent(c, PrayerAlarmReceiver.class)
              .putExtra("index", i)
              .putExtra("zone", zone)
              .putExtra("generation", generation)
              .putExtra("when", time.toInstant().toEpochMilli());
      schedule(c, pending(c, "prayer/" + i, alarm, i), time);
      store.set("scheduled_" + i, time.toString());
    }
    Set<String> old = new HashSet<>(Arrays.asList(store.text("scheduled_task_ids", "").split(",")));
    List<PrayerRelativeReminder> tasks = new TaskStore(c).safeLoad();
    Set<String> ids = new HashSet<>();
    for (PrayerRelativeReminder task : tasks) {
      ids.add(task.id);
      old.remove(task.id);
      manager.cancel(pending(c, "task/" + task.id, new Intent(c, TaskAlarmReceiver.class), 0));
      TaskPlanner.Occurrence next = TaskPlanner.next(task, days, now);
      store.set("scheduled_task_" + task.id, "");
      if (next == null) continue;
      Intent alarm =
          new Intent(c, TaskAlarmReceiver.class)
              .putExtra("id", task.id)
              .putExtra("generation", generation)
              .putExtra("zone", zone)
              .putExtra("anchor_date", next.anchorDate.toString())
              .putExtra("when", next.time.toInstant().toEpochMilli());
      schedule(c, pending(c, "task/" + task.id, alarm, 0), next.time);
      store.set("scheduled_task_" + task.id, next.time.toString());
    }
    for (String id : old)
      if (!id.isEmpty())
        manager.cancel(pending(c, "task/" + id, new Intent(c, TaskAlarmReceiver.class), 0));
    store.set("scheduled_task_ids", String.join(",", ids));
  }

  private static PendingIntent pending(Context c, String key, Intent i, int request) {
    return PendingIntent.getBroadcast(
        c,
        request,
        i.setData(Uri.parse("wsm://alarm/" + key)),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  public static void schedule(Context c, PendingIntent pending, ZonedDateTime time) {
    AlarmManager a = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    try {
      if (exact(c))
        a.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, time.toInstant().toEpochMilli(), pending);
      else
        a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time.toInstant().toEpochMilli(), pending);
    } catch (SecurityException ignored) {
    }
  }

  public static boolean exact(Context c) {
    return Build.VERSION.SDK_INT < 31
        || ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
  }

  public static boolean current(Context c, Intent i) {
    return new UserStore(c).text("alarm_generation", "").equals(i.getStringExtra("generation"))
        && new AppPreferences(c).zone().code.equals(i.getStringExtra("zone"))
        && i.getLongExtra("when", Long.MAX_VALUE) <= System.currentTimeMillis() + 1000;
  }
}
