package com.example.waktusolatmalaysia.utils;

import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.*;
import java.util.*;

public final class ReminderPlanner {
  private ReminderPlanner() {}

  public static ZonedDateTime next(Collection<PrayerDay> days, int index, ZonedDateTime now) {
    if (Arrays.stream(PrayerDay.OBLIGATORY).noneMatch(i -> i == index))
      throw new IllegalArgumentException("Obligatory prayer required");
    ZonedDateTime best = null;
    for (PrayerDay day : days) {
      ZonedDateTime t = day.date.atTime(day.times.get(index)).atZone(PrayerClock.MALAYSIA);
      if (t.isAfter(now) && (best == null || t.isBefore(best))) best = t;
    }
    return best;
  }
}
