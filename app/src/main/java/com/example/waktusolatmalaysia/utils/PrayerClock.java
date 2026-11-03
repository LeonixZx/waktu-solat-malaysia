package com.example.waktusolatmalaysia.utils;

import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class PrayerClock {
  public static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");

  private PrayerClock() {}

  public static Next next(PrayerDay today, PrayerDay tomorrow, ZonedDateTime instant) {
    ZonedDateTime now = instant.withZoneSameInstant(MALAYSIA);
    if (today == null || !today.date.equals(now.toLocalDate())) return null;
    for (int index : PrayerDay.OBLIGATORY) {
      ZonedDateTime time = today.date.atTime(today.times.get(index)).atZone(MALAYSIA);
      if (time.isAfter(now)) return new Next(index, time, false);
    }
    if (tomorrow != null && tomorrow.date.equals(today.date.plusDays(1))) {
      return new Next(1, tomorrow.date.atTime(tomorrow.times.get(1)).atZone(MALAYSIA), true);
    }
    return null;
  }

  public static final class Next {
    public final int index;
    public final ZonedDateTime time;
    public final boolean tomorrow;

    Next(int index, ZonedDateTime time, boolean tomorrow) {
      this.index = index;
      this.time = time;
      this.tomorrow = tomorrow;
    }

    public long secondsFrom(ZonedDateTime now) {
      return Math.max(0, Duration.between(now, time).getSeconds());
    }
  }
}
