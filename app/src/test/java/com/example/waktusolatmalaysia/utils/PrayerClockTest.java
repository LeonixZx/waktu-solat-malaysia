package com.example.waktusolatmalaysia.utils;

import static org.junit.Assert.*;

import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.Test;

public class PrayerClockTest {
  private PrayerDay day(String date) {
    return new PrayerDay(
        LocalDate.parse(date),
        "1448-05-22",
        Arrays.asList(
            LocalTime.of(5, 38),
            LocalTime.of(5, 48),
            LocalTime.of(6, 56),
            LocalTime.of(7, 21),
            LocalTime.of(13, 0),
            LocalTime.of(16, 20),
            LocalTime.of(18, 58),
            LocalTime.of(20, 9)));
  }

  @Test
  public void morningSkipsSyurukAndDhuha() {
    PrayerClock.Next next =
        PrayerClock.next(
            day("2026-11-02"),
            null,
            ZonedDateTime.of(2026, 11, 2, 6, 0, 0, 0, PrayerClock.MALAYSIA));
    assertNotNull(next);
    assertEquals(4, next.index);
    assertEquals(13, next.time.getHour());
  }

  @Test
  public void afterIsyakUsesNextDaysActualSubuhAcrossYearBoundary() {
    ZonedDateTime now = ZonedDateTime.of(2026, 12, 31, 23, 30, 0, 0, PrayerClock.MALAYSIA);
    PrayerClock.Next next = PrayerClock.next(day("2026-12-31"), day("2027-01-01"), now);
    assertNotNull(next);
    assertTrue(next.tomorrow);
    assertEquals(LocalDate.of(2027, 1, 1), next.time.toLocalDate());
    assertEquals(22680, next.secondsFrom(now));
  }

  @Test
  public void usesMalaysiaDateWhenDeviceIsInAnotherTimezone() {
    ZonedDateTime utc = ZonedDateTime.of(2026, 11, 1, 22, 0, 0, 0, ZoneId.of("UTC"));
    PrayerClock.Next next = PrayerClock.next(day("2026-11-02"), null, utc);
    assertNotNull(next);
    assertEquals(4, next.index);
  }

  @Test
  public void missingTomorrowNeverReusesTodaysSubuh() {
    assertNull(
        PrayerClock.next(
            day("2026-11-02"),
            null,
            ZonedDateTime.of(2026, 11, 2, 23, 0, 0, 0, PrayerClock.MALAYSIA)));
  }

  @Test
  public void exactPrayerTimeAdvancesToNextPrayer() {
    PrayerClock.Next next =
        PrayerClock.next(
            day("2026-11-02"),
            null,
            ZonedDateTime.of(2026, 11, 2, 13, 0, 0, 0, PrayerClock.MALAYSIA));
    assertNotNull(next);
    assertEquals(5, next.index);
  }

  @Test
  public void historicalDayDoesNotShowLiveCountdown() {
    assertNull(
        PrayerClock.next(
            day("2026-11-01"),
            day("2026-11-02"),
            ZonedDateTime.of(2026, 11, 2, 12, 0, 0, 0, PrayerClock.MALAYSIA)));
  }
}
