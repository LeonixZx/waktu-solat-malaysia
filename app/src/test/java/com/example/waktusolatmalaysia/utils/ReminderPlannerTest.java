package com.example.waktusolatmalaysia.utils;

import static org.junit.Assert.*;

import com.example.waktusolatmalaysia.model.PrayerDay;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class ReminderPlannerTest {
  private PrayerDay day(LocalDate d) {
    return new PrayerDay(
        d,
        "1448-05-22",
        Arrays.asList(
            LocalTime.of(5, 40),
            LocalTime.of(5, 50),
            LocalTime.of(7, 0),
            LocalTime.of(7, 30),
            LocalTime.of(13, 0),
            LocalTime.of(16, 20),
            LocalTime.of(19, 0),
            LocalTime.of(20, 10)));
  }

  @Test
  public void elapsedPrayerRollsToNextKnownDay() {
    LocalDate d = LocalDate.of(2026, 11, 2);
    var result =
        ReminderPlanner.next(
            Arrays.asList(day(d.plusDays(1)), day(d)),
            1,
            d.atTime(6, 0).atZone(PrayerClock.MALAYSIA));
    assertEquals(d.plusDays(1).atTime(5, 50).atZone(PrayerClock.MALAYSIA), result);
  }

  @Test
  public void currentPrayerIsExcludedAndMissingTomorrowNotInvented() {
    LocalDate d = LocalDate.of(2026, 11, 2);
    assertNull(
        ReminderPlanner.next(List.of(day(d)), 4, d.atTime(13, 0).atZone(PrayerClock.MALAYSIA)));
  }

  @Test
  public void comparesInstantsAcrossTimeZones() {
    LocalDate d = LocalDate.of(2026, 11, 2);
    var result = ReminderPlanner.next(List.of(day(d)), 4, d.atTime(4, 0).atZone(ZoneOffset.UTC));
    assertEquals(d.atTime(13, 0).atZone(PrayerClock.MALAYSIA), result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void sunriseIsNotAnObligatoryPrayer() {
    ReminderPlanner.next(Collections.emptyList(), 2, ZonedDateTime.now());
  }
}
