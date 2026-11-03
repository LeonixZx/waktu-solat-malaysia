package com.example.waktusolatmalaysia.utils;

import static org.junit.Assert.*;

import com.example.waktusolatmalaysia.model.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class TaskPlannerTest {
  private PrayerDay day(LocalDate d) {
    return new PrayerDay(
        d,
        "1448-05-22",
        Arrays.asList(
            LocalTime.of(5, 30),
            LocalTime.of(5, 40),
            LocalTime.of(7, 0),
            LocalTime.of(7, 30),
            LocalTime.of(13, 0),
            LocalTime.of(16, 20),
            LocalTime.of(19, 0),
            LocalTime.of(20, 10)));
  }

  @Test
  public void mondayIshaAfterSixHoursFallsOnTuesday() {
    LocalDate monday = LocalDate.of(2026, 11, 2);
    var task = new PrayerRelativeReminder("test", "Read", 7, 360, Set.of(1), true);
    var next =
        TaskPlanner.next(
            task, List.of(day(monday)), monday.atTime(21, 0).atZone(PrayerClock.MALAYSIA));
    assertEquals(monday, next.anchorDate);
    assertEquals(monday.plusDays(1).atTime(2, 10).atZone(PrayerClock.MALAYSIA), next.time);
  }

  @Test
  public void negativeOffsetCrossesToPreviousDay() {
    LocalDate tuesday = LocalDate.of(2026, 11, 3);
    var task = new PrayerRelativeReminder("test", "Prepare", 1, -360, Set.of(2), true);
    var next =
        TaskPlanner.next(
            task,
            List.of(day(tuesday)),
            tuesday.minusDays(1).atTime(20, 0).atZone(PrayerClock.MALAYSIA));
    assertEquals(tuesday, next.anchorDate);
    assertEquals(tuesday.minusDays(1).atTime(23, 40).atZone(PrayerClock.MALAYSIA), next.time);
  }

  @Test
  public void noMissingOrDisabledOccurrenceIsInvented() {
    LocalDate d = LocalDate.of(2026, 11, 2);
    var disabled = new PrayerRelativeReminder("test", "Read", 7, 15, Set.of(1), false);
    assertNull(TaskPlanner.next(disabled, List.of(day(d)), ZonedDateTime.now()));
    assertNull(
        TaskPlanner.next(
            new PrayerRelativeReminder("test", "Read", 7, 15, Set.of(2), true),
            List.of(day(d)),
            d.atStartOfDay(PrayerClock.MALAYSIA)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsOffsetsBeyondSixHours() {
    new PrayerRelativeReminder("test", "Read", 1, 361, Set.of(1), true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsIntegerOverflowOffsets() {
    new PrayerRelativeReminder("test", "Read", 1, Integer.MIN_VALUE, Set.of(1), true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsEmptyWeekdays() {
    new PrayerRelativeReminder("test", "Read", 1, 0, Set.of(), true);
  }

  @Test
  public void calendarHasFiveActualUtcTimes() {
    String text = CalendarFile.prayers(List.of(day(LocalDate.of(2026, 11, 2))), "WLY01", true);
    assertEquals(5, text.split("BEGIN:VEVENT").length - 1);
    assertTrue(text.contains("DTSTART:20261101T214000Z"));
    assertTrue(text.endsWith("END:VCALENDAR\r\n"));
  }
}
