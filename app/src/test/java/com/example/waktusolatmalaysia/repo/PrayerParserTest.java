package com.example.waktusolatmalaysia.repo;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import org.json.JSONException;
import org.junit.Test;

public class PrayerParserTest {
  private static final String RESPONSE =
      "{\"zone\":\"WLY01\",\"prayerTime\":[{\"date\":\"02-Nov-2026\",\"hijri\":\"1448-05-22\","
          + "\"imsak\":\"05:38:00\",\"fajr\":\"05:48:00\",\"syuruk\":\"06:56:00\",\"dhuha\":\"07:21:00\","
          + "\"dhuhr\":\"13:00:00\",\"asr\":\"16:20:00\",\"maghrib\":\"18:58:00\",\"isha\":\"20:09:00\"}]}";

  @Test
  public void readsAllEightTimesAndHijriDate() throws Exception {
    var day =
        PrayerParser.parse(RESPONSE, YearMonth.of(2026, 11), "WLY01")
            .get(LocalDate.of(2026, 11, 2));
    assertNotNull(day);
    assertEquals("1448-05-22", day.hijri);
    assertEquals(8, day.times.size());
    assertEquals(LocalTime.of(20, 9), day.times.get(7));
  }

  @Test(expected = JSONException.class)
  public void rejectsWrongZone() throws Exception {
    PrayerParser.parse(RESPONSE, YearMonth.of(2026, 11), "SGR01");
  }

  @Test(expected = JSONException.class)
  public void rejectsWrongMonth() throws Exception {
    PrayerParser.parse(RESPONSE, YearMonth.of(2026, 12), "WLY01");
  }

  @Test(expected = JSONException.class)
  public void rejectsEmptyTimetable() throws Exception {
    PrayerParser.parse("{\"prayerTime\":[]}", YearMonth.of(2026, 11), "WLY01");
  }

  @Test(expected = JSONException.class)
  public void rejectsMissingTimes() throws Exception {
    PrayerParser.parse(
        RESPONSE.replace("\"fajr\":\"05:48:00\",", ""), YearMonth.of(2026, 11), "WLY01");
  }
}
