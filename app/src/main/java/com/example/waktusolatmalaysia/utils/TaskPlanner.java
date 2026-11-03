package com.example.waktusolatmalaysia.utils;

import com.example.waktusolatmalaysia.model.*;
import java.time.*;
import java.util.*;

public final class TaskPlanner {
  public static final class Occurrence {
    public final LocalDate anchorDate;
    public final ZonedDateTime time;

    Occurrence(LocalDate d, ZonedDateTime t) {
      anchorDate = d;
      time = t;
    }
  }

  public static Occurrence next(
      PrayerRelativeReminder task, Collection<PrayerDay> days, ZonedDateTime now) {
    if (!task.enabled) return null;
    Occurrence result = null;
    for (PrayerDay d : days) {
      if (!task.weekdays.contains(d.date.getDayOfWeek().getValue())) continue;
      ZonedDateTime t =
          d.date
              .atTime(d.times.get(task.anchor))
              .atZone(PrayerClock.MALAYSIA)
              .plusMinutes(task.offsetMinutes);
      if (t.isAfter(now) && (result == null || t.isBefore(result.time)))
        result = new Occurrence(d.date, t);
    }
    return result;
  }
}
